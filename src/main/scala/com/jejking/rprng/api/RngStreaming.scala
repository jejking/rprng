package com.jejking.rprng.api

import akka.NotUsed
import akka.actor.{ActorSelection, ActorSystem}
import akka.http.scaladsl.model.HttpEntity.{Chunk, ChunkStreamPart, Chunked}
import akka.http.scaladsl.model._
import akka.stream._
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.stage._
import akka.util.ByteString
import com.jejking.rprng.png.PngSourceFactory
import com.jejking.rprng.rng._

import scala.concurrent.Future

/**
 * Defines useful functionality for the Routing part of the API to use in interacting with the
 * underlying streams of pseudo-randomness.
 */
trait RngStreaming {

  /**
    * Generates a byte string of the requested size which is encapsulated
    * in the entity of the HTTP Response.
    *
    * @param blockSize number of bytes to obtain, must be strictly positive
    * @return future of http response
    */
  def responseForByteBlock(blockSize: Int): Future[HttpResponse]

  /**
    * Generates a HTTP Response whose entity is a chunked stream of random bytes
    * - terminated by the client aborting the connnection.
    * @param chunkSize requested chunks of randomness
    * @return http response with entity wired to a stream of randomness
    */
  def responseForByteStream(chunkSize: Int): HttpResponse

  /**
    * Returns a structured response (for conversion to JSON) to the request
    * @param req the request
    * @return future of the requested structure
    */
  def responseForIntegerCollection(req: RandomIntegerCollectionRequest): Future[RandomIntegerCollectionResponse]

  /**
    * Generates a HTTP Response whose entity is a stream of bytes
    * representing a PNG with random RGBA channel values of the
    * requested width and height.
    *
    * The PNG is streamed, being broken down into IDAT chunks
    * of around 32KB.
    *
    * @param width of the PNG requested
    * @param height of the PNG requested
    * @return response with appropriate content-type set and entity being the PNG
    */
  def responseForPng(width: Int, height: Int): HttpResponse
}

class AkkaRngStreaming(path: ActorSelection)(implicit actorSystem: ActorSystem, materializer: Materializer) extends RngStreaming {

  private val pngSourceFactory = PngSourceFactory.pngSource(path) _

  private def createByteStringSource(blockSize: Int): Source[ByteString, NotUsed] = {
    val sourceGraph: Graph[SourceShape[ByteString], NotUsed] = new ByteStringSource(path, blockSize)
    Source.fromGraph(sourceGraph)
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

    import EightByteStringOps.toInt

    def createIntSource(): Source[Int, NotUsed] = {
      createByteStringSource(8)
        .map(bs => EightByteString(bs))
        .map(ebs => toInt(ebs, req.minBound, req.maxBound))
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

    req.collectionType match {
      case RandomList => listsFromStream()
      case RandomSet  => setsFromStream()
    }

  }

  override def responseForPng(width: Int, height: Int): HttpResponse = {
    val pngSource = pngSourceFactory(width, height)
    val entity: ResponseEntity = HttpEntity(ContentType(MediaTypes.`image/png`), pngSource)
    HttpResponse(StatusCodes.OK).withEntity(entity)
  }

}

/**
  * Akka streams graph stage that takes a stream of random ints in - and emits
  * a set of the requested size as soon as one has become available. Duplicate
  * inputs during set construction are - obviously - discarded.
  *
  * @param requestedSize
  */
class ToSizedSet(requestedSize: Int) extends GraphStage[FlowShape[Int, Set[Int]]] {

  val in: Inlet[Int] = Inlet[Int]("ints.in")
  val out: Outlet[Set[Int]] = Outlet[Set[Int]]("int sets.out")

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

