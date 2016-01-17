package com.jejking.rprng.api

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpEntity.{Chunk, ChunkStreamPart, Chunked}
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.stage._
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
    Source.fromPublisher(publisher)
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
      Source.fromPublisher(publisher)
    }

    def listsFromStream(): Future[RandomIntegerCollectionResponse] = {
      createIntSource()
        .take(req.count * req.size)
        .grouped(req.size)
        .grouped(req.count)
        .map(lli => RandomIntegerCollectionResponse(lli))
        .toMat(Sink.head)(Keep.right)
        .run()
    }

    def setsFromStream(): Future[RandomIntegerCollectionResponse] = {
      createIntSource()
        .transform(() => ToSizedSet(req.size))
        .take(req.count)
        .grouped(req.count)
        .map(seqOfSets => RandomIntegerCollectionResponse(seqOfSets))
        .toMat(Sink.head)(Keep.right)
        .run()
    }

    /*
    Think about creating some form of custom PushPullStage that allows us
    to map the source to a traversable to a set of size N *using WithToSizedSet.
    Input would be ints, an the appropriately sized set.
     */

    req.collectionType match {
      case RandomList => listsFromStream
      case RandomSet  => setsFromStream
    }

  }
}

class ToSizedSet(requestedSetSize: Int) extends PushPullStage[Int, Set[Int]] {

  private var setBeingBuilt = Set.empty[Int]

  override def onPush(elem: Int, ctx: Context[Set[Int]]): SyncDirective = {

    this.setBeingBuilt = this.setBeingBuilt + elem

    // we're done and can push downstream
    if (this.setBeingBuilt.size == requestedSetSize) {
      val copy = this.setBeingBuilt
      // reset
      this.setBeingBuilt = Set.empty[Int]
      ctx.push(copy)
    } else {
      ctx.pull() // get some more as we're not finished yet
    }

  }

  override def onPull(ctx: Context[Set[Int]]): SyncDirective = {
    ctx.pull() // start the process of collecting ints needed for our set
  }

  override def onUpstreamFinish(ctx: Context[Set[Int]]): TerminationDirective = {
    ctx.absorbTermination()
  }

}

object ToSizedSet {

  def apply(requestedSetSize: Int) = new ToSizedSet(requestedSetSize)
}

object AkkaStreamsHelper {



}
