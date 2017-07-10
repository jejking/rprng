package com.jejking.rprng.png

import java.nio.ByteBuffer
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

  "crc32" should "behave like the Java one" in {
    val bytes = ByteString(1, 2, 3, 4, 5, 6, 7, 8)
    Png.crc32(bytes) shouldBe myCrc(bytes)
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

  it should "define the correct IHDR_CHUNK_TYPE chunk given a positive width and a positive header" in {
    val width = ByteString.fromArray(ByteBuffer.allocate(4).putInt(256).array())
    val height = ByteString.fromArray(ByteBuffer.allocate(4).putInt(512).array())

    val crc = myCrc(Png.IHDR_CHUNK_TYPE ++ width ++ height ++ ByteString(8, 6, 0, 0, 0))

    val expectedBytes = ByteString(13) ++ Png.IHDR_CHUNK_TYPE ++ width ++ height ++ ByteString(8, 6, 0, 0, 0) ++ crc

    Png.ihdr(256, 512) shouldBe expectedBytes

  }

  def myCrc(byteString: ByteString): ByteString = {
    val crc = new CRC32()
    crc.update(byteString.toArray)
    val crcValue = crc.getValue
    ByteString.fromArray(ByteBuffer.allocate(8).putLong(crcValue).array()).drop(4)
  }



}
