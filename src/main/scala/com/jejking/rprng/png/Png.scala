package com.jejking.rprng.png

import akka.http.scaladsl.coding.DeflateCompressor
import akka.util.ByteString

import java.nio.charset.Charset

/**
  * Functions for doing some basic work encoding random streams
  * into PNGs.
  */
object Png {

  private val TARGET_IDAT_CHUNK_SIZE = 1024 * 32
  private val US_ASCII = Charset.forName("US-ASCII")

  val PNG_SIGNATURE = ByteString(137, 80, 78, 71, 13, 10, 26, 10)
  val IHDR_CHUNK_TYPE = ByteString("IHDR", US_ASCII)
  val IDAT_CHUNK_TYPE = ByteString("IDAT", US_ASCII)
  val IEND_CHUNK_TYPE = ByteString("IEND", US_ASCII)

  def ihdr(width: Int, height:Int): ByteString = {

    require(width > 0)
    require(height > 0)

    val IHDR_LENGTH = toUnsignedFourByteInt(13)

    // constant for our case, not in general
    val BIT_DEPTH = 8
    val COLOUR_TYPE = 6 // Truecolour with Alpha
    val COMPRESSION_METHOD = 0 // deflate
    val FILTER_METHOD = 0 // adaptive filtering as per PNG spec
    val INTERLACE_METHOD = 0 // none
    val constantPart = ByteString(BIT_DEPTH.toByte,
                                  COLOUR_TYPE.toByte,
                                  COMPRESSION_METHOD.toByte,
                                  FILTER_METHOD.toByte,
                                  INTERLACE_METHOD.toByte)

    val chunkData = toUnsignedFourByteInt(width) ++ toUnsignedFourByteInt(height) ++ constantPart

    val partToCrc = IHDR_CHUNK_TYPE ++ chunkData
    val crc = crc32(partToCrc)

    IHDR_LENGTH ++ partToCrc ++ crc
  }

  def scanline(bytesPerPixel: Int, expectedWidth: Int): ByteString => ByteString = bs => {
    require(bs.length == expectedWidth * bytesPerPixel)
    ByteString(0.toByte) ++ bs
  }

  def idat(deflateCompressor: DeflateCompressor = new DeflateCompressor())(bytes: ByteString, shouldFinish: Boolean) = {

    def doCompression(): ByteString = {
      if (shouldFinish) {
        deflateCompressor.compressAndFinish(bytes)
      } else {
        deflateCompressor.compressAndFlush(bytes)
      }
    }

    val compressedBytes = doCompression()
    val toCheckSum = IDAT_CHUNK_TYPE ++ compressedBytes
    val crc = crc32(toCheckSum)
    val length = toUnsignedFourByteInt(compressedBytes.length)
    length ++ toCheckSum ++ crc

  }

  def iend() = {
    toUnsignedFourByteInt(0) ++ IEND_CHUNK_TYPE ++ crc32(IEND_CHUNK_TYPE)
  }



  def toUnsignedFourByteInt(value: Int): ByteString = {
    ByteString( (value >>> 24).toByte,
                (value >>> 16).toByte,
                (value >>> 8).toByte,
                (value >>> 0).toByte)
  }

  def crc32(byteString: ByteString): ByteString = {

    val initialCrc: Int = 0xFFFFFFFF
    val reversePolynomial = 0xEDB88320

    val crc = ~byteString.foldLeft(initialCrc)((crcIn: Int, b: Byte) => {
      var crcTmp = (crcIn ^ b) & 0xff

      (0 to 7).foreach(_ => {
        if ((crcTmp & 1) == 1) {
          crcTmp = (crcTmp >>> 1) ^ reversePolynomial
        } else {
          crcTmp = crcTmp >>> 1
        }
      })
      (crcIn >>> 8) ^ crcTmp
    })

    toUnsignedFourByteInt(crc)
  }


}
