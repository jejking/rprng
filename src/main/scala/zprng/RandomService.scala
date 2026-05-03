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
        stateRef      <- Ref.make(RNGState(seed.take(32), seed.drop(32).take(12), 0, 0))
      } yield LiveRandomService(stateRef, entropySource)
    }

  def fromSeed(
    key: Chunk[Byte],
    nonce: Chunk[Byte]
  ): ZLayer[EntropySource, Nothing, RandomService] =
    ZLayer.fromZIO {
      for {
        entropySource <- ZIO.service[EntropySource]
        stateRef      <- Ref.make(RNGState(key, nonce, 0, 0))
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
    stateRef.modify { state =>
      val childIndex    = state.splitCounter
      val newKey        = ChaChaCore.deriveKey(state.key, childIndex.toString)
      val newState      = RNGState(newKey, state.nonce, 0, 0)
      val updatedParent = state.copy(splitCounter = childIndex + 1)

      val childRef = Unsafe.unsafe { implicit u =>
        Ref.unsafe.make(newState)
      }

      (new LiveRandomService(childRef, entropySource), updatedParent)
    }
