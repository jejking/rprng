package zprng

import zio.*
import zio.stream.*

/** ZStream adapters for RandomService.
  */
object RandomStreams:

  def randomBytesStream(chunkSize: Int): ZStream[RandomService, Nothing, Chunk[Byte]] =
    ZStream.repeatZIO(ZIO.serviceWithZIO[RandomService](_.nextBytes(chunkSize)))

  def randomIntStream: ZStream[RandomService, Nothing, Int] =
    ZStream.repeatZIO(ZIO.serviceWithZIO[RandomService](_.nextInt))

  def randomIntBoundedStream(bound: Int): ZStream[RandomService, Nothing, Int] =
    ZStream.repeatZIO(ZIO.serviceWithZIO[RandomService](_.nextInt(bound)))
