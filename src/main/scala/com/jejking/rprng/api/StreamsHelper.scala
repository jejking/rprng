package com.jejking.rprng.api

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpEntity.{Chunk, ChunkStreamPart, Chunked}
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import com.jejking.rprng.rng.{RandomIntegerCollectionResponse, RandomIntegerCollectionRequest, RandomByteStringActorPublisher}

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

  override def responseForIntegerCollection(req: RandomIntegerCollectionRequest) = ???
}

object AkkaStreamsHelper {

  // from java.util.Random

  /**
   * protected  Int next(bits : Int) {
  var oldseed : Long = 0L
  var nextseed : Long = 0L
  val seed : AtomicLong = this.seed
do {
oldseed = seed.get
nextseed = (oldseed * multiplier + addend) & mask
}while (!seed.compareAndSet(oldseed, nextseed))
return (nextseed >>> (48 - bits)).toInt
}
   *
   * /**
   * The form of nextInt used by IntStream Spliterators.
   * For the unbounded case: uses nextInt().
   * For the bounded case with representable range: uses nextInt(int bound)
   * For the bounded case with unrepresentable range: uses nextInt()
   *
   * @param origin the least value, unless greater than bound
   * @param bound the upper bound (exclusive), must not equal origin
   * @return a pseudorandom value
   */
private[util]   def internalNextInt(origin : Int, bound : Int) : Int = {
if (origin < bound) {
  val n : Int = bound - origin
if (n > 0) {
return nextInt(n) + origin
}
else {
  var r : Int = 0
do {
r = nextInt
}while (r < origin || r >= bound)
return r
}
}
else {
return nextInt
}
}
   *
   *
   * Int nextInt(bound : Int) {
if (bound <= 0) throw new IllegalArgumentException(BadBound)
  var r : Int = next(31)
  val m : Int = bound - 1
if ((bound & m) == 0) r = ((bound * r.toLong) >> 31).toInt
else {

{
  var u : Int = r
while (u - (({r = u % bound; r})) + m < 0) {

u = next(31)
}
}
}
return r
}
   *
   *
   */


  // some method to map from four bytes to an integer

  // some method to constrain the range of an integer

  // some method to construct a set of a given size from a stream of integers

  // some method to convert a stream of byte strings of (known) size 4 to a stream of integers
}
