package zprng

import zio.Chunk

/** Represents the state of the ChaCha20-based RNG.
  *
  * @param key
  *   32-byte key
  * @param nonce
  *   12-byte nonce (IETF variant)
  * @param counter
  *   32-bit counter
  */
final case class RNGState(
  key: Chunk[Byte],
  nonce: Chunk[Byte],
  counter: Int
)

object RNGState:
  val KeySize   = 32
  val NonceSize = 12
