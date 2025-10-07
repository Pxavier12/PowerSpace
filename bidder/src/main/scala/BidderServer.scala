package com.powerspace.test

import com.powerspace.test.bidder.bidder._
import io.grpc.netty.NettyServerBuilder
import io.grpc.{Server}
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object BidderServer extends App {

  //Gestionnaire par defaut des pools et threads
  implicit val ec: ExecutionContext = ExecutionContext.global

  def createServer(): Server = {
    val bidderService = new BidderServiceImpl()

    NettyServerBuilder
      .forPort(9090)
      .addService(BidderServiceGrpc.bindService(bidderService, ec))
      .build()
      .start()
  }

  val server = createServer()
  println(s"Serveur Bidder démarré sur le port ${server.getPort}")

  sys.addShutdownHook {
    println("Arrêt du serveur Bidder...")
    server.shutdown()
  }

  server.awaitTermination()
}
