package com.powerspace.test

import com.powerspace.test.bidder.bidder._
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}

/**
 * Service publicitaire optimisé avec cache, patterns asynchrones et gestion d'erreurs
 */
class AdvertisementService(
  bidderClient: BidderClient,
  cacheService: CacheService
)(implicit ec: ExecutionContext) {

  /**
   * Version optimisée avec cache et traitement parallèle
   */
  def getAdvertisementByPositionCode(positionCode: String): Future[Option[Advertisement]] = {
    // Récupération en parallèle avec cache
    val positionFuture = cacheService.getPositions
    
    positionFuture.flatMap { positions =>
      positions.find(_.code == positionCode) match {
        case Some(position) =>
          // Utilisation du cache filtré par pays
          cacheService.getEligibleAdGroups(position.countryCode).flatMap { eligibleAdGroups =>
            if (eligibleAdGroups.isEmpty) {
              Future.successful(None)
            } else {
              // Conversion optimisée en AdGroupData
              val adGroupData = eligibleAdGroups.map(convertToAdGroupData)
              
              // Appel gRPC avec gestion d'erreurs
              bidderClient.getBid(positionCode, position.countryCode, adGroupData)
                .map(convertBidResponseToAdvertisement(positionCode))
                .recover {
                  case ex =>
                    println(s"Erreur lors du bidding pour $positionCode: ${ex.getMessage}")
                    None
                }
            }
          }
        case None =>
          Future.successful(None)
      }
    }.recover {
      case ex =>
        println(s"Erreur lors de la récupération des données pour $positionCode: ${ex.getMessage}")
        None
    }
  }

  /**
   * Facturation optimisée avec invalidation de cache
   */
  def recordImpression(advertisement: Advertisement): Future[Unit] = {
    // Facturation asynchrone avec gestion d'erreurs
    val billingFuture = AdGroupRepository.getAdGroups.flatMap { adGroups =>
      adGroups.find(_.id == advertisement.id) match {
        case Some(adGroup) =>
          // Facturation via le repository directement (plus rapide)
          AdGroupRepository.billAdGroup(adGroup).map { _ =>
            // Invalidation du cache après facturation
            cacheService.invalidateAdGroupsCache()
          }
        case None =>
          Future.successful(())
      }
    }

    billingFuture.recover {
      case ex =>
        println(s"Erreur lors de la facturation de l'annonce ${advertisement.id}: ${ex.getMessage}")
        () // Continue même en cas d'erreur de facturation
    }
  }

  /**
   * Conversion optimisée AdGroup -> AdGroupData
   */
  private def convertToAdGroupData(adGroup: AdGroup): AdGroupData = {
    AdGroupData(
      id = adGroup.id,
      name = adGroup.name,
      description = adGroup.description,
      image = adGroup.image,
      countryCode = adGroup.countryCode,
      cost = adGroup.cost,
      costType = adGroup.costType,
      budget = adGroup.budget,
      isActive = true // Déjà filtré par le cache
    )
  }

  /**
   * Conversion BidResponse -> Advertisement avec gestion d'erreurs
   */
  private def convertBidResponseToAdvertisement(positionCode: String)(bidResponse: BidResponse): Option[Advertisement] = {
    if (bidResponse.success && bidResponse.winningBid.isDefined) {
      val winningBid = bidResponse.winningBid.get
      Some(Advertisement(
        id = winningBid.id,
        name = winningBid.name,
        description = winningBid.description,
        image = winningBid.image,
        positionCode = positionCode,
        countryCode = winningBid.countryCode
      ))
    } else {
      println(s"Aucune publicité sélectionnée pour $positionCode: ${bidResponse.errorMessage}")
      None
    }
  }

  /**
   * Méthode de pré-chauffage (un peu "manuelle" mais effective)
   */
  def warmupCache(): Future[Unit] = {
    println("Préchauffage du cache...")
    val countries = Seq("fr", "be", "it")
    
    val warmupFutures = countries.map { country =>
      cacheService.getEligibleAdGroups(country).map { adGroups =>
        println(s"Cache préchauffé pour $country: ${adGroups.size} AdGroups")
      }
    }
    
    Future.sequence(warmupFutures).map(_ => ())
  }

  /**
   * Méthode de nettoyage périodique du cache
   */
  def cleanupCache(): Unit = {
    cacheService.cleanupExpiredEntries()
  }
}
