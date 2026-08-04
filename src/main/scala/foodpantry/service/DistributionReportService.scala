package foodpantry.service

import foodpantry.model.{
  DistributionPlan,
  DistributionStatus
}

case class DistributionReport(
    totalPlans: Int,
    plannedPlans: Int,
    completedPlans: Int,
    cancelledPlans: Int,
    requestsServed: Int
)

// ai-assisted: #41
// why: AI helped create immutable distribution-summary calculations for reporting.
object DistributionReportService:

  def generate(
      distributionPlans: List[DistributionPlan]
  ): DistributionReport =
    val statusCounts =
      distributionPlans.groupMapReduce(
        distributionPlan =>
          distributionPlan.status
      )(
        _ => 1
      )(
        (firstCount, secondCount) =>
          firstCount + secondCount
      )

    // ai-assisted: #53
    // why: AI helped rename this request-based count so the report does not overstate households served.
    val requestsServed =
      distributionPlans
        .filter(
          distributionPlan =>
            distributionPlan.status ==
              DistributionStatus.Completed
        )
        .map(
          distributionPlan =>
            distributionPlan.householdRequestId
        )
        .distinct
        .size

    DistributionReport(
      totalPlans = distributionPlans.size,
      plannedPlans =
        statusCounts.getOrElse(
          DistributionStatus.Planned,
          0
        ),
      completedPlans =
        statusCounts.getOrElse(
          DistributionStatus.Completed,
          0
        ),
      cancelledPlans =
        statusCounts.getOrElse(
          DistributionStatus.Cancelled,
          0
        ),
      requestsServed = requestsServed
    )
