package com.jejking.rprng.api

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpEntity.{Chunk, ChunkStreamPart, Chunked}
import akka.http.scaladsl.model._
import akka.stream._
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
    *
    * @param blockSize number of bytes to obtain, must be strictly positive
   * @return graph to run
   */
  def responseForByteBlock(blockSize: Int): Future[HttpResponse]

  def responseForByteStream(chunkSize: Int): HttpResponse

  def responseForIntegerCollection(req: RandomIntegerCollectionRequest): Future[RandomIntegerCollectionResponse]

}

class AkkaStreamsHelper(path: String = "/user/randomRouter")(implicit actorSystem: ActorSystem, actorMaterializer: ActorMaterializer) extends StreamsHelper {

  private def createByteStringSource(blockSize: Int): Source[ByteString, NotUsed] = {
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
    val chunkSource: Source[ChunkStreamPart, NotUsed] = createByteStringSource(chunkSize).map(bs => Chunk(bs))
    val entity: ResponseEntity = Chunked(ContentTypes.`application/octet-stream`, chunkSource)
    HttpResponse(StatusCodes.OK).withEntity(entity)
  }

  override def responseForIntegerCollection(req: RandomIntegerCollectionRequest): Future[RandomIntegerCollectionResponse] = {
    val builder = List.newBuilder[Iterable[Int]]
    builder.sizeHint(req.count)

    def createIntSource(): Source[Int, NotUsed] = {
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
        .via(new ToSizedSet(req.size))
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

class ToSizedSet(requestedSize: Int) extends GraphStage[FlowShape[Int, Set[Int]]] {

  val in = Inlet[Int]("ints.in")
  val out = Outlet[Set[Int]]("int sets.out")

  val shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {

      private var setBeingBuilt = Set.empty[Int]

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val elem = grab(in)

          setBeingBuilt = setBeingBuilt + elem

          // we're done and can push downstream
          if (setBeingBuilt.size == requestedSize) {
            val copy = setBeingBuilt
            // reset
            setBeingBuilt = Set.empty[Int]
            push(out, copy)
          } else {
            pull(in)
          }
        }
      })
      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          pull(in)
        }
      })
    }
}

object ToSizedSet {

  def apply(requestedSetSize: Int) = new ToSizedSet(requestedSetSize)
}

object AkkaStreamsHelper {



}
