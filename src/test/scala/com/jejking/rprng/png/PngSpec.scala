package com.jejking.rprng.png

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.zip.CRC32

import org.scalatest.{FlatSpec, Matchers}
import akka.util.ByteString

/**
  * Created by jking on 09/07/2017.
  */
class PngSpec extends FlatSpec with Matchers {

  "Png" should "define the PNG signature" in {
    val expectedBytes = ByteString(0xFFFFFF89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    Png.PNG_SIGNATURE shouldBe expectedBytes
  }

  it should "define correct bytes for IHDR_CHUNK_TYPE critical chunk type" in {
    val expectedBytes = ByteString(73, 72, 68, 82)
    Png.IHDR_CHUNK_TYPE shouldBe expectedBytes
  }

  it should "define correct bytes for IDAT_CHUNK_TYPE critical chunk type" in {
    val expectedBytes = ByteString(73, 68, 65, 84)
    Png.IDAT_CHUNK_TYPE shouldBe expectedBytes
  }

  it should "define correct bytes for IEND_CHUNK_TYPE critical chunk type" in {
    val expectedBytes = ByteString(73, 69, 78, 68)
    Png.IEND_CHUNK_TYPE shouldBe expectedBytes
  }


  "crc32" should "behave like the Java one" in {
    val bytes = ByteString(1, 2, 3, 4, 5, 6, 7, 8)
    Png.crc32(bytes) shouldBe javaCrc(bytes)
  }

  it should "also do so with negative bytes" in {
    val bytes = ByteString(-1, -2, -3, -4, -5, -6, -7, -8)
    Png.crc32(bytes) shouldBe javaCrc(bytes)
  }

  "ihdr" should "convert a signed 32 bit integer to unsigned four byte array" in {
    val expectedLong = java.lang.Integer.toUnsignedLong(256)
    val expectedByteString = ByteString.fromArray(ByteBuffer.allocate(8).putLong(expectedLong).array()).drop(4)
    Png.toUnsignedFourByteInt(256) shouldBe expectedByteString
  }

  it should "reject a negative width parameter" in {
    a [IllegalArgumentException] should be thrownBy {
      Png.ihdr(-10, 512)
    }
  }

  it should "reject a zero width parameter" in {
    a [IllegalArgumentException] should be thrownBy {
      Png.ihdr(0, 512)
    }
  }

  it should "reject a negative height parameter" in {
    a [IllegalArgumentException] should be thrownBy {
      Png.ihdr(256, -512)
    }
  }

  it should "reject a zero height parameter" in {
    a [IllegalArgumentException] should be thrownBy {
      Png.ihdr(256, 0)
    }
  }

  it should "define the correct IHDR chunk given a positive width and a positive header" in {
    val width = ByteString.fromArray(ByteBuffer.allocate(4).putInt(256).array())
    val height = ByteString.fromArray(ByteBuffer.allocate(4).putInt(512).array())

    val crc = javaCrc(Png.IHDR_CHUNK_TYPE ++ width ++ height ++ ByteString(8, 6, 0, 0, 0))

    val expectedBytes = ByteString(13) ++ Png.IHDR_CHUNK_TYPE ++ width ++ height ++ ByteString(8, 6, 0, 0, 0) ++ crc

    Png.ihdr(256, 512) shouldBe expectedBytes

  }


  "idat" should "create an IDAT chunk given a byte string assumed to represent pixels" in {
    val bytes = ByteString("this is a very nice picture", Charset.forName("UTF-8"))
    val compressedBytes = javaDeflate(bytes)
    val toChecksum = Png.IDAT_CHUNK_TYPE ++ compressedBytes
    val checkSum = javaCrc(toChecksum)
    val expected = Png.toUnsignedFourByteInt(toChecksum.length) ++ toChecksum ++ checkSum

    Png.idat(bytes) shouldBe expected
  }

  "iend" should "create an IEND chunk" in {
    val zeroLength = Png.toUnsignedFourByteInt(0)
    val crc = javaCrc(Png.IEND_CHUNK_TYPE)
    val expectedByteString = zeroLength ++ Png.IEND_CHUNK_TYPE ++ crc

    Png.iend shouldBe expectedByteString
  }

  private def javaDeflate(bytes: ByteString): ByteString = {
    import java.util.zip.Deflater
    val deflater = new Deflater(Deflater.BEST_COMPRESSION, false)
    deflater.setInput(bytes.toArray)
    deflater.finish()
    val buffer = new Array[Byte](1024)
    val writtenBytes = deflater.deflate(buffer)
    deflater.end()
    ByteString.fromArray(buffer, 0, writtenBytes)
  }


  private def javaCrc(byteString: ByteString): ByteString = {
    val crc = new CRC32()
    crc.update(byteString.toArray)
    val crcValue = crc.getValue
    ByteString.fromArray(ByteBuffer.allocate(8).putLong(crcValue).array()).drop(4)
  }



}
