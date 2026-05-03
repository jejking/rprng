package zprng

import zio.*

/** Service for generating random numbers.
  */
trait RandomService:
  def nextBytes(n: Int): UIO[Chunk[Byte]]
  def nextInt: UIO[Int]
  def nextInt(bound: Int): UIO[Int]
  def nextDouble: UIO[Double] // [-1.0, 1.0)
  def reseed: UIO[Unit]
  def split: UIO[RandomService]

/** Configuration for RandomService.
  *
  * @param autoReseedThreshold
  *   Optional threshold in bytes. If set, the service will automatically reseed after this many
  *   bytes have been generated.
  */
final case class RandomConfig(
  autoReseedThreshold: Option[Long] = None
)

object RandomService:
  val live: ZLayer[EntropySource & RandomConfig, Throwable, RandomService] =
    ZLayer.fromZIO {
      for {
        entropySource <- ZIO.service[EntropySource]
        config        <- ZIO.service[RandomConfig]
        seed          <- entropySource.nextBytes(RNGState.KeySize + RNGState.NonceSize)
        stateRef <- Ref.make(
          RNGState(
            seed.take(RNGState.KeySize),
            seed.drop(RNGState.KeySize).take(RNGState.NonceSize),
            0,
            0,
            0
          )
        )
      } yield LiveRandomService(stateRef, entropySource, config)
    }

  def fromSeed(
    key: Chunk[Byte],
    nonce: Chunk[Byte],
    config: RandomConfig = RandomConfig()
  ): ZLayer[EntropySource, Nothing, RandomService] =
    ZLayer.fromZIO {
      for {
        entropySource <- ZIO.service[EntropySource]
        stateRef      <- Ref.make(RNGState(key, nonce, 0, 0, 0))
      } yield LiveRandomService(stateRef, entropySource, config)
    }

final private class LiveRandomService(
  stateRef: Ref[RNGState],
  entropySource: EntropySource,
  config: RandomConfig
) extends RandomService:

  override def nextBytes(n: Int): UIO[Chunk[Byte]] =
    for {
      _ <- maybeAutoReseed(n)
      bytes <- stateRef.modify { state =>
        val (chunk, nextState) = ChaChaCore.generateBytes(state, n)
        (
          chunk,
          nextState.copy(bytesGeneratedSinceReseed = state.bytesGeneratedSinceReseed + n)
        )
      }
    } yield bytes

  private def maybeAutoReseed(n: Int): UIO[Unit] =
    stateRef.get.flatMap { state =>
      // The ChaCha20 counter is 32-bit. Each increment represents one 64-byte block.
      // We must reseed before the counter hits Int.MaxValue to avoid overflow and crashes.
      val maxSafeBytes = Int.MaxValue.toLong * 64
      val threshold    = config.autoReseedThreshold.getOrElse(maxSafeBytes)
      val limit        = Math.min(threshold, maxSafeBytes)

      if (state.bytesGeneratedSinceReseed + n > limit) reseed
      else ZIO.unit
    }

  override def nextInt: UIO[Int] =
    nextBytes(4).map(RandomMapping.bytesToInt)

  override def nextInt(bound: Int): UIO[Int] =
    RandomMapping.nextIntBoundedZIO(bound)(nextBytes)

  override def nextDouble: UIO[Double] =
    nextBytes(8).map(RandomMapping.bytesToDouble)

  override def reseed: UIO[Unit] =
    for {
      entropy <- entropySource.nextBytes(RNGState.KeySize + RNGState.NonceSize).orDie
      _ <- stateRef.update { state =>
        val newKey = ChaChaCore.mixEntropy(state.key, entropy.take(RNGState.KeySize))
        val newNonce =
          ChaChaCore.mixEntropyNonce(
            state.nonce,
            entropy.drop(RNGState.KeySize).take(RNGState.NonceSize)
          )
        state.copy(key = newKey, nonce = newNonce, counter = 0, bytesGeneratedSinceReseed = 0)
      }
    } yield ()

  override def split: UIO[RandomService] =
    stateRef.modify { state =>
      val childIndex = state.splitCounter
      val streamId = Chunk.fromArray(
        java.nio.ByteBuffer.allocate(8).putLong(childIndex).array()
      )
      val newKey        = ChaChaCore.deriveKey(state.key, streamId)
      val newNonce      = ChaChaCore.deriveNonce(state.nonce, streamId)
      val newState      = RNGState(newKey, newNonce, 0, 0, 0)
      val updatedParent = state.copy(splitCounter = childIndex + 1)

      val childRef = Unsafe.unsafe { implicit u =>
        Ref.unsafe.make(newState)
      }

      (new LiveRandomService(childRef, entropySource, config), updatedParent)
    }
