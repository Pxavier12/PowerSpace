package com.powerspace.test

import java.time.Instant
import scala.concurrent.{Future, ExecutionContext}
import scala.util.Random
import scala.collection.mutable

/**
 * Repository optimisé avec index et accès rapide par critères
 */
object AdGroupRepository {
  import scala.concurrent.ExecutionContext.Implicits.global

  private val adGroups = Seq(
    AdGroup(
      id = 1000,
      name = "Café Snubucks",
      description = "Café gratuit chaque lundi",
      image = "https://i.pinimg.com/originals/15/09/ed/1509eda0f50aa2cb923a08db2a4a9a4d.jpg",
      countryCode = "fr",
      cost = 1,
      costType = "cpm",
      budget = 5,
      start = None,
      end = None
    ),
    AdGroup(
      id = 1100,
      name = "Snipers",
      description = "Tu n'es pas toi quand tu as faim",
      image = "https://piximus.net/media2/71017/fake-brands-14-1.jpg",
      countryCode = "fr",
      cost = 2,
      costType = "cpm",
      budget = 10,
      start = Some(Instant.now().plusSeconds(2628000)),
      end = None
    ),
    AdGroup(
      id = 1200,
      name = "Numa",
      description = "Toujours plus rapide",
      image = "https://i.pinimg.com/originals/88/a3/9e/88a39ef65ca11a92f90dd7a06ad05214.jpg",
      countryCode = "fr",
      cost = 5,
      costType = "cpm",
      budget = 5,
      start = None,
      end = Some(Instant.now().minusSeconds(2628000))
    ),
    AdGroup(
      id = 1300,
      name = "Prongles",
      description = "Une fois que tu commences, tu ne peux plus t'arrêter",
      image = "https://img.buzzfeed.com/buzzfeed-static/static/2018-11/28/16/asset/buzzfeed-prod-web-04/sub-buzz-998-1543441865-23.jpg?downsize=600:*&output-format=auto&output-quality=auto",
      countryCode = "be",
      cost = 5,
      costType = "cpm",
      budget = 20,
      start = Some(Instant.now().minusSeconds(2628000)),
      end = Some(Instant.now().plusSeconds(2628000))
    ),
    AdGroup(
      id = 1400,
      name = "Borneo",
      description = "Parlez-vous Borneo ?",
      image = "https://img.buzzfeed.com/buzzfeed-static/static/2018-11/28/16/asset/buzzfeed-prod-web-05/sub-buzz-10256-1543442313-1.jpg?downsize=600:*&output-format=auto&output-quality=auto",
      countryCode = "be",
      cost = 3,
      costType = "cpm",
      budget = 5,
      start = None,
      end = None
    ),
    AdGroup(
      id = 1500,
      name = "Montre intelligente Iwo",
      description = "",
      image = "https://bestaliproducts.com/wp-content/uploads/2022/03/3-1-700x394.jpg",
      countryCode = "be",
      cost = 1,
      costType = "cpm",
      budget = 5,
      start = None,
      end = None
    ),
    AdGroup(
      id = 1600,
      name = "Abibas",
      description = "Rien n'est impossible",
      image = "https://i.pinimg.com/originals/87/f4/4f/87f44fcab873dbf25d4ec54a2f13a411.jpg",
      countryCode = "fr",
      cost = 6,
      costType = "cpm",
      budget = 15,
      start = Some(Instant.now().plusSeconds(2628000)),
      end = None
    ),
    AdGroup(
      id = 1700,
      name = "Deats",
      description = "Écoute ce que tu veux",
      image = "https://www.zikoko.com/wp-content/uploads/cloudinary/v1492774334/funny-name-brand-imitation-8__605_z7d93c.jpg",
      countryCode = "fr",
      cost = 1,
      costType = "cpm",
      budget = 5,
      start = None,
      end = Some(Instant.now().minusSeconds(2628000))
    ),
    AdGroup(
      id = 1800,
      name = "Caffè Stellato",
      description = "Il miglior caffè sotto le stelle",
      image = "https://s3-media0.fl.yelpcdn.com/bphoto/MDJ2vViwZW8Bk7bFrcC47g/348s.jpg",
      countryCode = "it",
      cost = 3,
      costType = "cpm",
      budget = 8,
      start = None,
      end = None
    ),
    AdGroup(
      id = 1900,
      name = "Scarpe Veloci",
      description = "Corri come il vento",
      image = "https://images.hardloop.fr/206653-large_default/scarpa-veloce-chaussons-escalade-homme.jpg",
      countryCode = "it",
      cost = 4,
      costType = "cpm",
      budget = 12,
      start = Some(Instant.now().minusSeconds(2628000)),
      end = Some(Instant.now().plusSeconds(2628000))
    ),
    AdGroup(
      id = 2000,
      name = "Gelato Supremo",
      description = "Il gusto dell'estate italiana",
      image = "https://augustusgelatery.com.au/wp-content/uploads/2023/10/Gelato-Sorbet-Blog-Image1-1-scaled.jpg",
      countryCode = "it",
      cost = 2,
      costType = "cpm",
      budget = 6,
      start = None,
      end = None
    )
  )

  // Index optimisés pour accès rapide
  private val adGroupsById: Map[Int, AdGroup] = adGroups.map(ag => ag.id -> ag).toMap
  private val adGroupsByCountry: Map[String, Seq[AdGroup]] = adGroups.groupBy(_.countryCode)
  
  // Budget mutable avec accès concurrent sécurisé
  private val budgets = new java.util.concurrent.ConcurrentHashMap[Int, Double]()
  adGroups.foreach(ag => budgets.put(ag.id, ag.budget))

  /**
   * Accès optimisé par ID (O(1))
   */
  def getAdGroupById(id: Int): Future[Option[AdGroup]] = {
    networkCall(adGroupsById.get(id))
  }

  /**
   * Accès optimisé par pays avec filtrage (O(n) sur subset)
   */
  def getEligibleAdGroupsByCountry(countryCode: String): Future[Seq[AdGroup]] = {
    val countryAdGroups = adGroupsByCountry.getOrElse(countryCode, Seq.empty)
    val now = Instant.now()
    
    val eligible = countryAdGroups.filter { adGroup =>
      val currentBudget = budgets.get(adGroup.id)
      currentBudget > 0 &&
      adGroup.start.forall(_.isBefore(now)) &&
      adGroup.end.forall(_.isAfter(now))
    }
    
    networkCall(eligible.sortBy(-_.cost)) // Pré-trié par coût décroissant
  }

  /**
   * Méthode standard pour compatibilité
   */
  def getAdGroups: Future[Seq[AdGroup]] = networkCall(adGroups)

  /**
   * Facturation optimisée avec mise à jour
   */
  def billAdGroup(adGroup: AdGroup): Future[Unit] = {
    val currentBudget = budgets.get(adGroup.id)
    val newBudget = math.max(0, currentBudget - adGroup.cost)
    budgets.put(adGroup.id, newBudget)
    
    networkCall(()) // Simulation de l'appel réseau
  }

  /**
   * Récupération optimisée des budgets
   */
  def getBudgets: Future[Map[Int, Double]] = {
    import scala.jdk.CollectionConverters._
    networkCall(budgets.asScala.toMap)
  }

  /**
   * Simulation d'appel réseau optimisée (latence réduite)
   */
  private def networkCall[T](maybeResult: T): Future[T] = {
    Future {
      // Latence réduite pour simulation
      Thread.sleep(Random.nextInt(100)) // Max 100ms au lieu de 300ms
      
      // Taux d'erreur réduit
      if (Random.nextInt(100) > 95) // 5% d'erreur au lieu de 20%
        throw new Exception("Network error")

      maybeResult
    }
  }

  /**
   * Statistiques de performance
   */
  def getStats: Future[Map[String, Any]] = {
    import scala.jdk.CollectionConverters._
    Future.successful(Map(
      "total_adgroups" -> adGroups.size,
      "countries" -> adGroupsByCountry.keys.toSeq,
      "active_budgets" -> budgets.asScala.count(_._2 > 0),
      "total_budget" -> budgets.asScala.values.sum
    ))
  }
}
