package zprng

import zio.*

/** Service for generating random numbers.
  */
trait RandomService:
  def nextBytes(n: Int): UIO[Chunk[Byte]]
  def nextInt: UIO[Int]
  def nextInt(bound: Int): UIO[Int]
  def nextDouble: UIO[Double] // [-1.0, 1.0]
  def reseed: UIO[Unit]
  def split: UIO[RandomService]

object RandomService:
  val live: ZLayer[EntropySource, Throwable, RandomService] =
    ZLayer.fromZIO {
      for {
        entropySource <- ZIO.service[EntropySource]
        seed          <- entropySource.nextBytes(32 + 12)
        stateRef      <- Ref.make(RNGState(seed.take(32), seed.drop(32).take(12), 0))
      } yield LiveRandomService(stateRef, entropySource)
    }

  def fromSeed(
    key: Chunk[Byte],
    nonce: Chunk[Byte]
  ): ZLayer[EntropySource, Nothing, RandomService] =
    ZLayer.fromZIO {
      for {
        entropySource <- ZIO.service[EntropySource]
        stateRef      <- Ref.make(RNGState(key, nonce, 0))
      } yield LiveRandomService(stateRef, entropySource)
    }

final private class LiveRandomService(
  stateRef: Ref[RNGState],
  entropySource: EntropySource
) extends RandomService:

  override def nextBytes(n: Int): UIO[Chunk[Byte]] =
    stateRef.modify { state =>
      ChaChaCore.generateBytes(state, n)
    }

  override def nextInt: UIO[Int] =
    nextBytes(4).map(RandomMapping.bytesToInt)

  override def nextInt(bound: Int): UIO[Int] =
    RandomMapping.nextIntBoundedZIO(bound)(nextBytes)

  override def nextDouble: UIO[Double] =
    nextBytes(8).map(RandomMapping.bytesToDouble)

  override def reseed: UIO[Unit] =
    for {
      entropy <- entropySource.nextBytes(32).orDie
      _ <- stateRef.update { state =>
        val newKey = ChaChaCore.mixEntropy(state.key, entropy)
        state.copy(key = newKey, counter = 0)
      }
    } yield ()

  override def split: UIO[RandomService] =
    for {
      // Derive a new key using some of the current RNG's output as a stream ID
      bytesForId <- nextBytes(16)
      streamId = bytesForId.map("%02x".format(_)).mkString
      currentState <- stateRef.get
      newKey = ChaChaCore.deriveKey(currentState.key, streamId)
      // New RNG starts with same nonce but counter 0 (or we could vary nonce)
      newStateRef <- Ref.make(RNGState(newKey, currentState.nonce, 0))
    } yield LiveRandomService(newStateRef, entropySource)
