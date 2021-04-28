package com.jejking.rprng.png

import akka.util.{ByteString, ByteStringBuilder}

import java.util.zip.Deflater
import scala.annotation.tailrec

/**
  * Essentially copied from the deprecated Akka Http Deflate Compressor.
  * @param compressionLevel
  */
class DeflateHelper (compressionLevel: Int) {
  require(compressionLevel >= 0 && compressionLevel <= 9, "Compression level needs to be between 0 and 9")
  import DeflateHelper._

  def this() = this(DeflateHelper.DefaultCompressionLevel)

  protected lazy val deflater = new Deflater(compressionLevel, false)

  final def compressAndFlush(input: ByteString): ByteString = {
    val buffer = newTempBuffer(input.size)

    compressWithBuffer(input, buffer) ++ flushWithBuffer(buffer)
  }
  final def compressAndFinish(input: ByteString): ByteString = {
    val buffer = newTempBuffer(input.size)

    compressWithBuffer(input, buffer) ++ finishWithBuffer(buffer)
  }

  protected def compressWithBuffer(input: ByteString, buffer: Array[Byte]): ByteString = {
    require(deflater.needsInput())
    deflater.setInput(input.toArray)
    drainDeflater(deflater, buffer)
  }
  protected def flushWithBuffer(buffer: Array[Byte]): ByteString = {
    val written = deflater.deflate(buffer, 0, buffer.length, Deflater.SYNC_FLUSH)
    ByteString.fromArray(buffer, 0, written)
  }
  protected def finishWithBuffer(buffer: Array[Byte]): ByteString = {
    deflater.finish()
    val res = drainDeflater(deflater, buffer)
    deflater.end()
    res
  }

  private def newTempBuffer(size: Int = 65536): Array[Byte] = {
    new Array[Byte](math.max(size, MinBufferSize))
  }
}

object DeflateHelper {
  val MinBufferSize = 1024
  val DefaultCompressionLevel = 6

  @tailrec
  def drainDeflater(deflater: Deflater, buffer: Array[Byte], result: ByteStringBuilder = new ByteStringBuilder()): ByteString = {
    val len = deflater.deflate(buffer)
    if (len > 0) {
      result ++= ByteString.fromArray(buffer, 0, len)
      drainDeflater(deflater, buffer, result)
    } else {
      require(deflater.needsInput())
      result.result()
    }
  }
}
