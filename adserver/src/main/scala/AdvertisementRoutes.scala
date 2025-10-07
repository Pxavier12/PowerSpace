package com.powerspace.test

import cats.effect.IO
import io.circe.generic.auto._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.{HttpRoutes}
import scala.concurrent.ExecutionContext


class AdvertisementRoutes(advertisementService: AdvertisementService)(implicit ec: ExecutionContext) {

  implicit val advertisementEncoder = jsonEncoderOf[IO, Advertisement]

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "api" / "health" =>
      Ok("Service de publicité optimisé opérationnel")

    case GET -> Root / positionCode =>
      IO.fromFuture(IO(advertisementService.getAdvertisementByPositionCode(positionCode)))
        .flatMap {
          case Some(advertisement) =>
            // Enregistrer l'impression et facturer de manière asynchrone
            IO.fromFuture(IO(advertisementService.recordImpression(advertisement)))
              .as(Ok(advertisement))
              .flatten
          case None =>
            NotFound(s"Aucune publicité trouvée pour la position: $positionCode")
        }
        .handleErrorWith {
          error =>
            // Gestion d'erreurs robuste
            println(s"Erreur lors du traitement de la requête $positionCode: ${error.getMessage}")
            InternalServerError(s"Erreur interne du serveur: ${error.getMessage}")
        }
  }

}
