package com.powerspace.test

import com.powerspace.test.bidder.bidder._
import io.grpc.stub.StreamObserver
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class BidderServiceImpl(implicit ec: ExecutionContext) extends BidderServiceGrpc.BidderService {

  override def getBid(request: BidRequest): Future[BidResponse] =
    Future {
      // Vérifier que nous avons des AdGroups disponibles
      if (request.availableAdgroups.isEmpty) {
        BidResponse(
          success = false,
          errorMessage = s"Aucun AdGroup disponible pour la position ${request.positionCode}",
          winningBid = None
        )
      } else {
        // Filtrer les AdGroups actifs avec budget suffisant
        val eligibleAdGroups = request.availableAdgroups.filter {
          adGroup =>
            adGroup.isActive && adGroup.budget > 0
        }

        // Sélectionner le meilleur AdGroup (coût le plus élevé)
        eligibleAdGroups.sortBy(-_.cost).headOption match {
          case Some(winningAdGroup) =>
            BidResponse(
              success = true,
              errorMessage = "",
              winningBid = Some(
                AdGroupBid(
                  id = winningAdGroup.id,
                  name = winningAdGroup.name,
                  description = winningAdGroup.description,
                  image = winningAdGroup.image,
                  countryCode = winningAdGroup.countryCode,
                  cost = winningAdGroup.cost,
                  costType = winningAdGroup.costType,
                  remainingBudget = winningAdGroup.budget
                )
              )
            )
          case None =>
            BidResponse(
              success = false,
              errorMessage = s"Aucun AdGroup éligible trouvé pour la position ${request.positionCode}",
              winningBid = None
            )
        }
      }
    }.recover {
      case error: Throwable =>
        BidResponse(
          success = false,
          errorMessage = s"Erreur lors du bidding: ${error.getMessage}",
          winningBid = None
        )
    }

  override def billAdGroup(request: BillRequest): Future[BillResponse] =
    // Le bidder ne gère plus directement la facturation
    // Il confirme simplement que la facturation peut avoir lieu
    Future.successful(
      BillResponse(
        success = true,
        errorMessage = "",
        remainingBudget = 0.0 // L'AdServer gère le budget réel
      )
    )

}
