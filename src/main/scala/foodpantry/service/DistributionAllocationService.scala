package foodpantry.service

import foodpantry.model.DistributionPlan

import java.time.LocalDate
import scala.util.Try

// ai-assisted: #44
// why: AI helped define a small abstraction for atomic distribution allocation.
trait DistributionAllocationService:

  def allocate(
      householdRequestId: String,
      foodItemId: String,
      allocatedQuantity: Int,
      distributionDate: LocalDate
  ): Try[DistributionPlan]

  // ai-assisted: #46
  // why: AI helped expose controlled workflow actions instead of arbitrary status edits.
  def complete(distributionPlanId: String): Try[DistributionPlan]

  def cancel(distributionPlanId: String): Try[DistributionPlan]
