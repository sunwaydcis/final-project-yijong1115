package foodpantry

import foodpantry.model.{
  DistributionPlan,
  DistributionStatus
}
import foodpantry.service.DistributionReportService

import munit.FunSuite

import java.time.LocalDate

// ai-assisted: #53
// why: AI helped verify the corrected request-based reporting meaning.
class DistributionReportServiceSuite extends FunSuite:

  test("report counts distinct requests receiving completed distributions"):
    val plans =
      List(
        plan("plan-1", "request-1", "Shared Name", DistributionStatus.Completed),
        plan("plan-2", "request-1", "Shared Name", DistributionStatus.Completed),
        plan("plan-3", "request-2", "Shared Name", DistributionStatus.Completed),
        plan("plan-4", "request-3", "Other Name", DistributionStatus.Planned),
        plan("plan-5", "request-4", "Other Name", DistributionStatus.Cancelled)
      )

    val report =
      DistributionReportService.generate(plans)

    assertEquals(report.totalPlans, 5)
    assertEquals(report.completedPlans, 3)
    assertEquals(report.plannedPlans, 1)
    assertEquals(report.cancelledPlans, 1)
    assertEquals(report.requestsServed, 2)

  private def plan(
      id: String,
      requestId: String,
      householdName: String,
      status: DistributionStatus
  ): DistributionPlan =
    DistributionPlan(
      id = id,
      householdRequestId = requestId,
      householdName = householdName,
      foodItemId = "food-1",
      foodItemName = "Rice",
      allocatedQuantity = 1,
      allocatedUnit = "packs",
      distributionDate = LocalDate.of(2026, 8, 4),
      status = status
    )
