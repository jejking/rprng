package com.jejking.rprng.png

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.zip.CRC32

import akka.util.ByteString

/**
  * Functions for doing some basic work encoding random streams
  * into PNGs.
  */
object Png {

  private val US_ASCII = Charset.forName("US-ASCII")

  val PNG_SIGNATURE = ByteString(137, 80, 78, 71, 13, 10, 26, 10)
  val IHDR_CHUNK_TYPE = ByteString("IHDR", US_ASCII)
  val IDAT_CHUNK_TYPE = ByteString("IDAT", US_ASCII)

  def ihdr(width: Int, height:Int): ByteString = {

    require(width > 0)
    require(height > 0)

    val IHDR_LENGTH = 13

    // constant for our case, not in general
    val BIT_DEPTH = 8
    val COLOUR_TYPE = 6 // Truecolour with Alpha
    val COMPRESSION_METHOD = 0 // deflate
    val FILTER_METHOD = 0 // adaptive filtering as per PNG spec
    val INTERLACE_METHOD = 0 // none
    val constantPart = ByteString(BIT_DEPTH, COLOUR_TYPE, COMPRESSION_METHOD, FILTER_METHOD, INTERLACE_METHOD)

    val chunkData = toUnsignedFourByteInt(width) ++ toUnsignedFourByteInt(height) ++ constantPart

    val partToCrc = IHDR_CHUNK_TYPE ++ chunkData
    val crc = crc32(partToCrc)

    ByteString(IHDR_LENGTH) ++ partToCrc ++ crc
  }

  def toUnsignedFourByteInt(value: Int): ByteString = {
    ByteString( (value >>> 24).toByte,
                (value >>> 16).toByte,
                (value >>> 8).toByte,
                (value >>> 0).toByte)
  }

  // adapted from Hacker's Delight http://www.hackersdelight.org/hdcodetxt/crc.c.txt
  def crc32(byteString: ByteString): ByteString = {

    val initialCrc = 0xFFFFFFFF

    val crc = ~byteString.foldLeft(initialCrc)((crcIn: Int, b: Byte) => {
      var crcTmp = crcIn ^ b
      (0 to 7).foreach(i => {
        val mask = -(crcTmp & 1)
        crcTmp = (crcTmp >>> 1) ^ (0xEDB88320 & mask)
      })
      crcTmp
    })

    toUnsignedFourByteInt(crc)
  }

}
