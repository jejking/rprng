package com.jejking.rprng.api

import akka.actor.{ActorPath, ActorRef, ActorSystem}
import akka.http.scaladsl.model.HttpEntity.{Chunked, ChunkStreamPart, Chunk}
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{Keep, Sink, Source, RunnableGraph}
import akka.util.ByteString
import com.jejking.rprng.rng.RandomByteStringActorPublisher

import scala.concurrent.Future

/**
 * Defines useful functionality for the Routing part of the API to use in interacting with the
 * underlying RPRNG infrastructure.
 */
trait StreamsHelper {

  /**
   * Creates a runnable graph that generates a single block of random bytes which is
   * converted to a future http response.
   * @param blockSize number of bytes to obtain, must be strictly positive
   * @return graph to run
   */
  def responseForByteBlock(blockSize: Int): Future[HttpResponse]

  def responseForByteStream(chunkSize: Int): HttpResponse

}

class AkkaStreamsHelper(path: String = "/user/randomRouter")(implicit actorSystem: ActorSystem, actorMaterializer: ActorMaterializer) extends StreamsHelper {

  private def createByteStringSource(blockSize: Int): Source[ByteString, Unit] = {
    val publisherActor = actorSystem.actorOf(RandomByteStringActorPublisher.props(blockSize, path))
    val publisher = ActorPublisher[ByteString](publisherActor)
    Source(publisher)
  }

  override def responseForByteBlock(blockSize: Int): Future[HttpResponse] = {
    createByteStringSource(blockSize)
      .take(1)
      .map(bs => HttpResponse(entity = bs))
      .toMat(Sink.head)(Keep.right)
      .run()
  }

  override def responseForByteStream(chunkSize: Int): HttpResponse = {
    val chunkSource: Source[ChunkStreamPart, Unit] = createByteStringSource(chunkSize).map(bs => Chunk(bs))
    val entity: ResponseEntity = Chunked(ContentTypes.`application/octet-stream`, chunkSource)
    HttpResponse(StatusCodes.OK).withEntity(entity)
  }
}
