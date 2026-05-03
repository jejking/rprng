package zprng

import zio.*

/** Pure functions for mapping random bytes to other types.
  */
object RandomMapping:

  /** Converts 4 bytes to a signed 32-bit integer.
    */
  def bytesToInt(bytes: Chunk[Byte]): Int =
    require(bytes.size >= 4, "Need at least 4 bytes for an Int")
    val b = bytes.toArray
    ((b(0) & 0xff) << 24) |
      ((b(1) & 0xff) << 16) |
      ((b(2) & 0xff) << 8) |
      (b(3) & 0xff)

  /** Generates a random integer in the range [0, bound) using rejection sampling.
    *
    * @param bound
    *   The upper bound (exclusive). Must be positive.
    * @param nextBytes
    *   A function that provides a chunk of random bytes.
    * @return
    *   The bounded integer.
    */
  def nextIntBounded(bound: Int)(nextBytes: Int => Chunk[Byte]): Int =
    require(bound > 0, "Bound must be positive")

    val threshold = (Int.MaxValue.toLong * 2 + 2) % bound
    val limit     = (Int.MaxValue.toLong * 2 + 2) - threshold

    def loop(): Int =
      val bytes         = nextBytes(4)
      val unsignedValue = bytesToInt(bytes).toLong & 0xffffffffL
      if (unsignedValue < limit) (unsignedValue % bound).toInt
      else loop()

    loop()

  /** Effectful version of nextIntBounded.
    */
  def nextIntBoundedZIO(bound: Int)(nextBytes: Int => UIO[Chunk[Byte]]): UIO[Int] =
    require(bound > 0, "Bound must be positive")

    val threshold = (Int.MaxValue.toLong * 2 + 2) % bound
    val limit     = (Int.MaxValue.toLong * 2 + 2) - threshold

    def loop(): UIO[Int] =
      nextBytes(4).flatMap { bytes =>
        val unsignedValue = bytesToInt(bytes).toLong & 0xffffffffL
        if (unsignedValue < limit) ZIO.succeed((unsignedValue % bound).toInt)
        else loop()
      }

    loop()

  /** Maps a 64-bit value (from 8 bytes) to a Double in the range [-1.0, 1.0].
    */
  def bytesToDouble(bytes: Chunk[Byte]): Double =
    require(bytes.size >= 8, "Need at least 8 bytes for a Double")
    val b = bytes.toArray
    val longValue =
      ((b(0).toLong & 0xff) << 56) |
        ((b(1).toLong & 0xff) << 48) |
        ((b(2).toLong & 0xff) << 40) |
        ((b(3).toLong & 0xff) << 32) |
        ((b(4).toLong & 0xff) << 24) |
        ((b(5).toLong & 0xff) << 16) |
        ((b(6).toLong & 0xff) << 8) |
        (b(7).toLong & 0xff)

    // Map Long.MinValue to Long.MaxValue to [-1.0, 1.0]
    // Double precision is 53 bits.
    val mask     = (1L << 53) - 1
    val mantissa = longValue & mask
    val exponent = 1.0 / (1L << 53)
    (mantissa * exponent * 2.0) - 1.0
