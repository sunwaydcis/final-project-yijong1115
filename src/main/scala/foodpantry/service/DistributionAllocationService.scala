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
