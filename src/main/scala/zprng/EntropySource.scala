package zprng

import zio.*

/** Encapsulates entropy acquisition.
  */
trait EntropySource:
  def nextBytes(n: Int): Task[Chunk[Byte]]

object EntropySource:
  val live: ZLayer[Any, Nothing, EntropySource] = ZLayer.succeed(LiveEntropySource())

final private class LiveEntropySource extends EntropySource:
  private val secureRandom = new java.security.SecureRandom()

  override def nextBytes(n: Int): Task[Chunk[Byte]] = ZIO.attempt {
    val bytes = new Array[Byte](n)
    secureRandom.nextBytes(bytes)
    Chunk.fromArray(bytes)
  }
