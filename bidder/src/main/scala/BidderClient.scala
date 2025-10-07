package com.powerspace.test

import com.powerspace.test.bidder.bidder._
import io.grpc.netty.NettyChannelBuilder
import io.grpc.{ManagedChannel, CallOptions}
import java.util.concurrent.TimeUnit
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._

class BidderClient(channel: ManagedChannel)(implicit ec: ExecutionContext) {

  private val stub = BidderServiceGrpc.stub(channel)

  def getBid(
    positionCode: String,
    countryCode: String,
    availableAdGroups: Seq[AdGroupData] = Seq.empty
  ): Future[BidResponse] = {
    val request = BidRequest(
      positionCode = positionCode,
      countryCode = countryCode,
      availableAdgroups = availableAdGroups
    )

    // Appel gRPC avec gestion d'erreurs robuste
    stub.getBid(request).recover {
      case _: java.util.concurrent.TimeoutException =>
        BidResponse(
          success = false,
          errorMessage = "Timeout lors de l'appel au bidder",
          winningBid = None
        )
      case ex: Exception =>
        BidResponse(
          success = false,
          errorMessage = s"Erreur gRPC: ${ex.getMessage}",
          winningBid = None
        )
    }
  }

  def billAdGroup(adGroupId: Int, amount: Double): Future[BillResponse] = {
    val request = BillRequest(
      adgroupId = adGroupId,
      amount = amount
    )

    stub.billAdGroup(request).recover {
      case _: java.util.concurrent.TimeoutException =>
        BillResponse(
          success = false,
          errorMessage = "Timeout lors de la facturation",
          remainingBudget = 0.0
        )
      case ex: Exception =>
        BillResponse(
          success = false,
          errorMessage = s"Erreur gRPC: ${ex.getMessage}",
          remainingBudget = 0.0
        )
    }
  }

  def close(): Unit =
    channel.shutdown()

}

object BidderClient {

  def create(host: String = "localhost", port: Int = 9090)(implicit ec: ExecutionContext): BidderClient = {
    val channel = NettyChannelBuilder
      .forAddress(host, port)
      .usePlaintext()
      // Optimisations de performance
      .keepAliveTime(30, TimeUnit.SECONDS)
      .keepAliveTimeout(5, TimeUnit.SECONDS)
      .keepAliveWithoutCalls(true)
      .maxInboundMessageSize(4 * 1024 * 1024) // 4MB max
      .maxInboundMetadataSize(8 * 1024) // 8KB max pour les métadonnées
      // Connection pooling implicite avec Netty
      .build()

    new BidderClient(channel)
  }

}
