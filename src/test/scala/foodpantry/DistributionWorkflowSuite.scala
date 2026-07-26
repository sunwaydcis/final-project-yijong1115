package foodpantry

import foodpantry.database.{
  DatabaseInitializer,
  DatabaseManager
}
import foodpantry.model.{
  DistributionStatus,
  HouseholdRequest,
  PerishableFood,
  RequestStatus
}
import foodpantry.repository.{
  DerbyDistributionAllocationService,
  DerbyFoodItemRepository,
  DerbyHouseholdRequestRepository
}
import foodpantry.service.DistributionPlanningService

import munit.FunSuite

import java.nio.file.Paths
import java.time.LocalDate
import java.util.UUID

// ai-assisted: #46
// why: AI helped verify stock and status changes against an isolated Derby database.
class DistributionWorkflowSuite extends FunSuite:

  private def withWorkflow(
      testCode: (
          DerbyFoodItemRepository,
          DerbyHouseholdRequestRepository,
          DerbyDistributionAllocationService,
          String,
          String
      ) => Unit
  ): Unit =
    val databaseDirectory =
      Paths
        .get(
          "target",
          "test-databases",
          UUID.randomUUID().toString
        )
        .toAbsolutePath

    System.setProperty(
      "foodpantry.database.url",
      s"jdbc:derby:$databaseDirectory;create=true"
    )

    try
      DatabaseInitializer.initialize().get

      val foodItemRepository =
        new DerbyFoodItemRepository

      val requestRepository =
        new DerbyHouseholdRequestRepository

      val allocationService =
        new DerbyDistributionAllocationService(
          new DistributionPlanningService
        )

      val foodItemId =
        UUID.randomUUID().toString

      val requestId =
        UUID.randomUUID().toString

      foodItemRepository
        .add(
          PerishableFood(
            foodItemId,
            "Rice packs",
            "Grains",
            10,
            "packs",
            LocalDate.now.plusMonths(3)
          )
        )
        .get

      requestRepository
        .add(
          HouseholdRequest(
            requestId,
            "Test household",
            4,
            "Grains",
            4,
            LocalDate.now,
            RequestStatus.Approved
          )
        )
        .get

      testCode(
        foodItemRepository,
        requestRepository,
        allocationService,
        foodItemId,
        requestId
      )
    finally
      System.clearProperty("foodpantry.database.url")

  test("completing a plan reduces stock and fulfils the request"):
    withWorkflow:
      (
          foodItemRepository,
          requestRepository,
          allocationService,
          foodItemId,
          requestId
      ) =>
        val planned =
          allocationService
            .allocate(
              requestId,
              foodItemId,
              4,
              LocalDate.now.plusDays(1)
            )
            .get

        assertEquals(
          requestRepository.findById(requestId).get.get.status,
          RequestStatus.Approved
        )

        val completed =
          allocationService.complete(planned.id).get

        assertEquals(
          completed.status,
          DistributionStatus.Completed
        )

        assertEquals(
          foodItemRepository
            .findById(foodItemId)
            .get
            .get
            .quantity,
          6
        )

        assertEquals(
          requestRepository.findById(requestId).get.get.status,
          RequestStatus.Fulfilled
        )

        assert(allocationService.complete(planned.id).isFailure)

  test("cancelling a plan releases it without reducing stock"):
    withWorkflow:
      (
          foodItemRepository,
          requestRepository,
          allocationService,
          foodItemId,
          requestId
      ) =>
        val planned =
          allocationService
            .allocate(
              requestId,
              foodItemId,
              4,
              LocalDate.now.plusDays(1)
            )
            .get

        val cancelled =
          allocationService.cancel(planned.id).get

        assertEquals(
          cancelled.status,
          DistributionStatus.Cancelled
        )

        assertEquals(
          foodItemRepository
            .findById(foodItemId)
            .get
            .get
            .quantity,
          10
        )

        assertEquals(
          requestRepository.findById(requestId).get.get.status,
          RequestStatus.Approved
        )

        assert(
          allocationService
            .allocate(
              requestId,
              foodItemId,
              4,
              LocalDate.now.plusDays(1)
            )
            .isSuccess
        )

  test("legacy completed plans deduct stock exactly once"):
    val databaseDirectory =
      Paths
        .get(
          "target",
          "test-databases",
          UUID.randomUUID().toString
        )
        .toAbsolutePath

    System.setProperty(
      "foodpantry.database.url",
      s"jdbc:derby:$databaseDirectory;create=true"
    )

    try
      DatabaseManager.withConnection: connection =>
        val statement =
          connection.createStatement()

        try
          statement.executeUpdate(
            """
              |CREATE TABLE FOOD_ITEMS (
              |  ID VARCHAR(36) PRIMARY KEY,
              |  ITEM_TYPE VARCHAR(30) NOT NULL,
              |  NAME VARCHAR(100) NOT NULL,
              |  CATEGORY VARCHAR(50) NOT NULL,
              |  QUANTITY INT NOT NULL,
              |  UNIT VARCHAR(30) NOT NULL,
              |  EXPIRY_DATE DATE
              |)
              |""".stripMargin
          )

          statement.executeUpdate(
            """
              |CREATE TABLE DISTRIBUTION_PLANS (
              |  ID VARCHAR(36) PRIMARY KEY,
              |  HOUSEHOLD_REQUEST_ID VARCHAR(36) NOT NULL,
              |  HOUSEHOLD_NAME VARCHAR(100) NOT NULL,
              |  FOOD_ITEM_ID VARCHAR(36) NOT NULL,
              |  FOOD_ITEM_NAME VARCHAR(100) NOT NULL,
              |  ALLOCATED_QUANTITY INT NOT NULL,
              |  ALLOCATED_UNIT VARCHAR(30) NOT NULL,
              |  DISTRIBUTION_DATE DATE NOT NULL,
              |  STATUS VARCHAR(20) NOT NULL
              |)
              |""".stripMargin
          )

          statement.executeUpdate(
            """
              |INSERT INTO FOOD_ITEMS
              |  (
              |    ID,
              |    ITEM_TYPE,
              |    NAME,
              |    CATEGORY,
              |    QUANTITY,
              |    UNIT,
              |    EXPIRY_DATE
              |  )
              |VALUES
              |  (
              |    'food-1',
              |    'PERISHABLE',
              |    'Rice packs',
              |    'Grains',
              |    10,
              |    'packs',
              |    DATE('2030-01-01')
              |  )
              |""".stripMargin
          )

          statement.executeUpdate(
            """
              |INSERT INTO DISTRIBUTION_PLANS
              |  (
              |    ID,
              |    HOUSEHOLD_REQUEST_ID,
              |    HOUSEHOLD_NAME,
              |    FOOD_ITEM_ID,
              |    FOOD_ITEM_NAME,
              |    ALLOCATED_QUANTITY,
              |    ALLOCATED_UNIT,
              |    DISTRIBUTION_DATE,
              |    STATUS
              |  )
              |VALUES
              |  (
              |    'plan-1',
              |    'request-1',
              |    'Legacy household',
              |    'food-1',
              |    'Rice packs',
              |    4,
              |    'packs',
              |    DATE('2026-07-27'),
              |    'Completed'
              |  )
              |""".stripMargin
          )
        finally
          statement.close()
      .get

      DatabaseInitializer.initialize().get

      val foodItemRepository =
        new DerbyFoodItemRepository

      assertEquals(
        foodItemRepository
          .findById("food-1")
          .get
          .get
          .quantity,
        6
      )

      DatabaseInitializer.initialize().get

      assertEquals(
        foodItemRepository
          .findById("food-1")
          .get
          .get
          .quantity,
        6
      )
    finally
      System.clearProperty("foodpantry.database.url")
