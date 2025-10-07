package com.powerspace.test

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

/**
 * Service de cache avec TTL et invalidation automatique
 */
class CacheService(implicit ec: ExecutionContext) {
  
  private case class CacheEntry[T](value: T, expiry: Instant)
  
  private val positionsCache = new ConcurrentHashMap[String, CacheEntry[Seq[Position]]]()
  private val adGroupsCache = new ConcurrentHashMap[String, CacheEntry[Seq[AdGroup]]]()
  private val filteredAdGroupsCache = new ConcurrentHashMap[String, CacheEntry[Seq[AdGroup]]]()
  
  private val CACHE_TTL = 5.minutes
  private val FILTERED_CACHE_TTL = 1.minute
  
  /**
   * Cache des positions avec TTL
   */
  def getPositions: Future[Seq[Position]] = {
    val key = "all_positions"
    Option(positionsCache.get(key)) match {
      case Some(entry) if entry.expiry.isAfter(Instant.now()) =>
        Future.successful(entry.value)
      case _ =>
        PositionRepository.getPositions.map { positions =>
          val entry = CacheEntry(positions, Instant.now().plusSeconds(CACHE_TTL.toSeconds))
          positionsCache.put(key, entry)
          positions
        }
    }
  }
  
  /**
   * Cache des AdGroups avec TTL
   */
  def getAdGroups: Future[Seq[AdGroup]] = {
    val key = "all_adgroups"
    Option(adGroupsCache.get(key)) match {
      case Some(entry) if entry.expiry.isAfter(Instant.now()) =>
        Future.successful(entry.value)
      case _ =>
        AdGroupRepository.getAdGroups.map { adGroups =>
          val entry = CacheEntry(adGroups, Instant.now().plusSeconds(CACHE_TTL.toSeconds))
          adGroupsCache.put(key, entry)
          adGroups
        }
    }
  }
  
  /**
   * Cache des AdGroups filtrés par pays avec TTL plus court
   */
  def getEligibleAdGroups(countryCode: String): Future[Seq[AdGroup]] = {
    val key = s"eligible_$countryCode"
    Option(filteredAdGroupsCache.get(key)) match {
      case Some(entry) if entry.expiry.isAfter(Instant.now()) =>
        Future.successful(entry.value)
      case _ =>
        getAdGroups.map { adGroups =>
          val now = Instant.now()
          val eligible = adGroups.filter { adGroup =>
            adGroup.countryCode == countryCode &&
            adGroup.budget > 0 &&
            adGroup.start.forall(_.isBefore(now)) &&
            adGroup.end.forall(_.isAfter(now))
          }
          val entry = CacheEntry(eligible, Instant.now().plusSeconds(FILTERED_CACHE_TTL.toSeconds))
          filteredAdGroupsCache.put(key, entry)
          eligible
        }
    }
  }
  
  /**
   * Invalider le cache des AdGroups (utile après facturation)
   */
  def invalidateAdGroupsCache(): Unit = {
    adGroupsCache.clear()
    filteredAdGroupsCache.clear()
  }
  
  /**
   * Nettoyage automatique des entrées expirées
   */
  def cleanupExpiredEntries(): Unit = {
    val now = Instant.now()
    
    positionsCache.entrySet().asScala.foreach { entry =>
      if (entry.getValue.expiry.isBefore(now)) {
        positionsCache.remove(entry.getKey)
      }
    }
    
    adGroupsCache.entrySet().asScala.foreach { entry =>
      if (entry.getValue.expiry.isBefore(now)) {
        adGroupsCache.remove(entry.getKey)
      }
    }
    
    filteredAdGroupsCache.entrySet().asScala.foreach { entry =>
      if (entry.getValue.expiry.isBefore(now)) {
        filteredAdGroupsCache.remove(entry.getKey)
      }
    }
  }
}

object CacheService {
  def apply()(implicit ec: ExecutionContext): CacheService = new CacheService()
}
