package com.powerspace.test

import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger
import scala.concurrent.ExecutionContext
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

/**
 * Serveur AdServer optimisé avec cache, connection pooling et nettoyage automatique
 */
object AdServer extends IOApp {

  implicit val ec: ExecutionContext = ExecutionContext.global
  
  // Pool de threads dédié pour les tâches de maintenance
  private val scheduledExecutor: ScheduledExecutorService = 
    Executors.newScheduledThreadPool(2)

  def run(args: List[String]): IO[ExitCode] = {
    // Initialisation des services optimisés
    val bidderClient = BidderClient.create()
    val cacheService = CacheService()
    val advertisementService = new AdvertisementService(bidderClient, cacheService)
    val advertisementRoutes = new AdvertisementRoutes(advertisementService)

    val httpApp = advertisementRoutes.routes.orNotFound

    // Logger avec configuration optimisée
    val finalHttpApp = Logger.httpApp(
      logHeaders = false, // Réduire le logging pour les performances
      logBody = false
    )(httpApp)

    // Préchauffage du cache au démarrage
    val warmupIO = IO.fromFuture(IO(advertisementService.warmupCache()))

    // Configuration du nettoyage automatique du cache
    val cleanupTask = new Runnable {
      def run(): Unit = {
        try {
          advertisementService.cleanupCache()
          println("Cache nettoyé automatiquement")
        } catch {
          case ex: Exception =>
            println(s"Erreur lors du nettoyage du cache: ${ex.getMessage}")
        }
      }
    }
    
    // Nettoyage du cache toutes les 5 minutes
    scheduledExecutor.scheduleAtFixedRate(cleanupTask, 5, 5, TimeUnit.MINUTES)

    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(finalHttpApp)
      // Optimisations du serveur HTTP
      .withMaxConnections(1000) // Augmenter le nombre de connexions simultanées
      .withReceiveBufferSize(64 * 1024) // Buffer
      .build
      .use { server =>
        warmupIO >> // Préchauffage avant de démarrer
        IO.println(s" Serveur AdServer optimisé démarré sur ${server.address}") >>
        IO.println(" Le serveur bidder doit être démarré sur le port 9090") >>
        IO.never
      }
      .guarantee(IO.delay(scheduledExecutor.shutdown())) // Nettoyage à l'arrêt
      .as(ExitCode.Success)
  }
}
