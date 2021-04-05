package com.jejking.rprng.png

import akka.http.scaladsl.coding.DeflateCompressor
import akka.util.ByteString
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.zip.CRC32

/**
  * Created by jking on 09/07/2017.
  */
class PngSpec extends AnyFlatSpec with Matchers {

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

    val expectedBytes = Png.toUnsignedFourByteInt(13) ++ Png.IHDR_CHUNK_TYPE ++ width ++ height ++ ByteString(8, 6, 0, 0, 0) ++ crc

    Png.ihdr(256, 512) shouldBe expectedBytes

  }

  "scanline" should "create prepend a filter type 0 to a byte string assumed to represent a scanline" in {
    val width = 3
    val bytesPerPixel = 2
    val inputBytes = ByteString(1, 2, 3, 4, 5, 6)
    val expected = ByteString(0) ++ inputBytes

    val scanline: ByteString => ByteString = Png.scanline(bytesPerPixel, width)
    scanline(inputBytes) shouldBe expected
  }

  it should "fail if the scanline is not the same size as the value given in the width parameter" in {
    assertThrows[IllegalArgumentException] {
      val width = 5
      val bytesPerPixel = 1
      val inputBytes = ByteString(1, 2, 3)
      Png.scanline(bytesPerPixel, width)(inputBytes)
    }
  }

  "idat" should "create an IDAT chunk given a byte string assumed to represent scanlines and finish deflate" in {
    val bytes = ByteString("this is a very nice picture", Charset.forName("UTF-8"))
    val compressedBytes = javaDeflateFinish(bytes)
    val toChecksum = Png.IDAT_CHUNK_TYPE ++ compressedBytes
    val checkSum = javaCrc(toChecksum)
    val expected = Png.toUnsignedFourByteInt(compressedBytes.length) ++ toChecksum ++ checkSum

    val deflateCompressor = new DeflateCompressor()

    val idat = Png.idat(deflateCompressor) _
    idat(bytes, true) shouldBe expected
  }

  it should "create an IDAT chunk given a byte string assumed to represent scanlines and flush deflate" in {
    val bytes = ByteString("this is a very nice picture", Charset.forName("UTF-8"))
    val compressedBytes = javaDeflateFlush(bytes)
    val toChecksum = Png.IDAT_CHUNK_TYPE ++ compressedBytes
    val checkSum = javaCrc(toChecksum)
    val expected = Png.toUnsignedFourByteInt(compressedBytes.length) ++ toChecksum ++ checkSum

    val deflateCompressor = new DeflateCompressor()

    val idat = Png.idat(deflateCompressor) _
    idat(bytes, false) shouldBe expected
  }

  "iend" should "create an IEND chunk" in {
    val zeroLength = Png.toUnsignedFourByteInt(0)
    val crc = javaCrc(Png.IEND_CHUNK_TYPE)
    val expectedByteString = zeroLength ++ Png.IEND_CHUNK_TYPE ++ crc

    Png.iend shouldBe expectedByteString
  }

  private def javaDeflateFinish(bytes: ByteString): ByteString = {
    import java.util.zip.Deflater
    val deflater = new Deflater(6, false)
    deflater.setInput(bytes.toArray)
    deflater.finish()
    val buffer = new Array[Byte](1024)
    val writtenBytes = deflater.deflate(buffer)
    deflater.end()
    ByteString.fromArray(buffer, 0, writtenBytes)
  }


  private def javaDeflateFlush(bytes: ByteString): ByteString = {
    import java.util.zip.Deflater
    val deflater = new Deflater(6, false)
    deflater.setInput(bytes.toArray)
    val buffer = new Array[Byte](1024)
    val writtenBytes = deflater.deflate(buffer, 0, buffer.length, Deflater.SYNC_FLUSH)
    ByteString.fromArray(buffer, 0, writtenBytes)
  }

  private def javaCrc(byteString: ByteString): ByteString = {
    val crc = new CRC32()
    crc.update(byteString.toArray)
    val crcValue = crc.getValue
    ByteString.fromArray(ByteBuffer.allocate(8).putLong(crcValue).array()).drop(4)
  }



}
