package com.jejking.rprng.api

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpEntity.{Chunk, ChunkStreamPart, Chunked}
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import com.jejking.rprng.rng._

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

  def responseForIntegerCollection(req: RandomIntegerCollectionRequest): Future[RandomIntegerCollectionResponse]

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

  override def responseForIntegerCollection(req: RandomIntegerCollectionRequest): Future[RandomIntegerCollectionResponse] = {
    val builder = List.newBuilder[Iterable[Int]]
    builder.sizeHint(req.count)

    def createIntSource(): Source[Int, Unit] = {
      val publisherActor = actorSystem.actorOf(RandomIntActorPublisher.props(req.randomIntRequest(), path))
      val publisher = ActorPublisher[Int](publisherActor)
      Source(publisher)
    }

    def listsFromStream(): Future[RandomIntegerCollectionResponse] = {
      createIntSource()
        .take(req.count * req.size)
        .grouped(req.size)
        .map(s => s.toList)
        .grouped(req.count)
        .map(s => s.toList)
        .map(lli => RandomIntegerCollectionResponse(lli))
        .toMat(Sink.head)(Keep.right)
        .run()
    }

    req.collectionType match {
      case RandomList => listsFromStream
      case RandomSet  => throw new UnsupportedOperationException("not done yet")
    }

  }
}

object AkkaStreamsHelper {



}
