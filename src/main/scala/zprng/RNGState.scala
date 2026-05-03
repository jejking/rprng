package zprng

import zio.Chunk

/** Represents the state of the ChaCha20-based RNG.
  *
  * @param key
  *   32-byte key
  * @param nonce
  *   12-byte nonce (IETF variant)
  * @param bytesGenerated
  *   Number of bytes generated from the current key/nonce pair.
  * @param splitCounter
  *   Number of splits performed by this RNG instance.
  */
final case class RNGState(
  key: Chunk[Byte],
  nonce: Chunk[Byte],
  bytesGenerated: Long,
  splitCounter: Long
)

object RNGState:
  val KeySize   = 32
  val NonceSize = 12

  /** Maximum number of bytes that can be generated from a single key/nonce pair before the 32-bit
    * ChaCha20 block counter overflows. 2^32 blocks * 64 bytes per block.
    */
  val MaxBytesPerReseed: Long = Int.MaxValue.toLong * 64
