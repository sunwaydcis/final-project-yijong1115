package foodpantry

import foodpantry.model.{
  HouseholdRequest,
  RequestStatus
}
import foodpantry.service.HouseholdRequestWorkflow

import munit.FunSuite

import java.time.LocalDate

// ai-assisted: #52
// why: AI helped verify valid and invalid household-request decisions.
class HouseholdRequestWorkflowSuite extends FunSuite:

  private val pendingRequest =
    HouseholdRequest(
      "request-1",
      "Test household",
      4,
      "Grains",
      5,
      LocalDate.now,
      RequestStatus.Pending
    )

  test("approves a pending request"):
    assertEquals(
      HouseholdRequestWorkflow
        .approve(pendingRequest)
        .map(_.status),
      Right(RequestStatus.Approved)
    )

  test("rejects a pending request"):
    assertEquals(
      HouseholdRequestWorkflow
        .reject(pendingRequest)
        .map(_.status),
      Right(RequestStatus.Rejected)
    )

  test("does not reject an approved request"):
    val approvedRequest =
      pendingRequest.copy(
        status = RequestStatus.Approved
      )

    assert(
      HouseholdRequestWorkflow
        .reject(approvedRequest)
        .isLeft
    )

  test("does not approve a rejected request"):
    val rejectedRequest =
      pendingRequest.copy(
        status = RequestStatus.Rejected
      )

    assert(
      HouseholdRequestWorkflow
        .approve(rejectedRequest)
        .isLeft
    )
