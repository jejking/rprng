package com.jejking.rprng.png

import java.io.{BufferedOutputStream, File, FileOutputStream, OutputStream}
import java.util.Random
import javax.imageio.ImageIO

import akka.util.{ByteString, ByteStringBuilder}

import scala.language.postfixOps

/**
  * Created by jking on 13/07/2017.
  */
object PngMain {

  def main(args: Array[String]): Unit = {

    val bufferedOutputStream = new BufferedOutputStream(new FileOutputStream("/tmp/test1.png"))


    writeByteStringToStream(Png.PNG_SIGNATURE, bufferedOutputStream)
    writeByteStringToStream(Png.ihdr(250, 500), bufferedOutputStream)

    val rng = new Random()

//    for (i <- 1 to 50) {
//      val scanLinesByteStringBuilder = new ByteStringBuilder()
//      for (j <- 1 to 10) {
//        val picData = new Array[Byte](1 * 250 * 4)
//        rng.nextBytes(picData)
//
//        val rawScanLineBytes = ByteString(picData)
//        val scanLine = Png.scanline(4, 250)(rawScanLineBytes)
//        scanLinesByteStringBuilder ++= scanLine
//      }
//      writeByteStringToStream(Png.idat(scanLinesByteStringBuilder.result()), bufferedOutputStream)
//    }

    writeByteStringToStream(Png.iend(), bufferedOutputStream)

    bufferedOutputStream.close()

    println("done writing")

    val bufferedImage = ImageIO.read(new File("/tmp/test1.png"))

    println(bufferedImage.getWidth + " x " + bufferedImage.getHeight())
  }

  def writeByteStringToStream(byteString: ByteString, outputStream: OutputStream): Unit = {
    outputStream.write(byteString.toArray)
    outputStream.flush()
  }

}
