package com.powerspace.test

import scala.concurrent.Future
import scala.util.Random

object PositionRepository {
  import scala.concurrent.ExecutionContext.Implicits.global

  private val positions = Seq(
    Position(id = 1, name = "First Position", code = "FRA", countryCode = "fr"),
    Position(id = 11, name = "French First Media", code = "FRB", countryCode = "fr"),
    Position(id = 111, name = "Auto Media", code = "FRC", countryCode = "fr"),
    Position(id = 2, name = "Second Position", code = "BEA", countryCode = "be"),
    Position(id = 22, name = "RTBB", code = "BEB", countryCode = "be"),
    Position(id = 3, name = "RTBB", code = "ITA", countryCode = "it")
  )

  def getPositions: Future[Seq[Position]] = this.networkCall(this.positions)

  private def networkCall[T](maybeResult: T): Future[T] =
    Future {
      Thread.sleep(Random.nextInt(1000))
      if (Random.nextInt(10) > 8)
        throw new Exception("Network error")

      maybeResult
    }

}
