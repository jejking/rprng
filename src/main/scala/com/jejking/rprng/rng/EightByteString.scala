package com.jejking.rprng.rng

import akka.util.ByteString

/**
  * Wraps a byte string that is ensured to be of length 8.
  */
case class EightByteString(byteString: ByteString) {
  require(byteString.length == 8, s"ByteString is not of length 8. It is of length ${byteString.length}")
}
