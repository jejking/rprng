package com.jejking.rprng.png

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.util.{ByteString, ByteStringBuilder}

/**
  * Graph stage to emit Png IDAT chunks as [[akka.util.ByteString]] instances from a source of byte strings
  * which are assumed to represent Png scanlines of [[width]] pixels.
  *
  * The stage consumes [[height]] number of scanlines and attempts to build IDAT chunks of round about 32kb
  * which are emitted downstream. The stage is completed when [[height]] lines have been consumed.
  *
  * @param width of the PNG to be generated
  * @param height of the PNG to be generated
  */
private[png] class IdatStage(width: Int, height: Int) extends GraphStage[FlowShape[ByteString, ByteString]] {

  import IdatStage._

  val in = Inlet[ByteString]("Scanline.in")
  val out = Outlet[ByteString]("Idat.out")

  override def shape: FlowShape[ByteString, ByteString] = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    val scanlinesPerChunk: Int = numberOfScanlinesPerIdatChunk(width)

    private var byteStringBuilder = new ByteStringBuilder
    private var scanlinesPulled = 0

    private val shouldPush = shouldPushDownstream(scanlinesPerChunk, height)
    private val idat = Png.idat() _

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        pull(in)
      }
    })

    setHandler(in, new InHandler {

      override def onPush(): Unit = {
        updateLocalState()

        if (shouldPush(scanlinesPulled)) {
          if (finished()) {
            sendFinalIdatChunk()
          } else {
            sendNonFinalIdatChunk()
          }
        } else {
          pull(in)
        }
      }

      private def updateLocalState() = {
        scanlinesPulled = scanlinesPulled + 1
        val scanline = grab(in)
        byteStringBuilder ++= scanline
      }

      private def finished(): Boolean = scanlinesPulled == height

      private def sendNonFinalIdatChunk(): Unit = {
        val idatChunk = idat(byteStringBuilder.result(), false)
        byteStringBuilder = new ByteStringBuilder()
        push(out, idatChunk)
      }

      private def sendFinalIdatChunk(): Unit = {
        val idatChunk = idat(byteStringBuilder.result(), true)
        push(out, idatChunk)
        completeStage()
      }
    })

  }


}

object IdatStage {

  private val bytesPerPixel = 4 // RGBA
  private val filterByte = 1
  private val thirtyTwoKilobytes = 32 * 1024

  /**
    * Computes the number of scanlines we should be aiming to
    * put into an IDAT chunk so that we get as close as possible to 32 kB.
    * @param width width of the PNG
    * @return number of scanlines
    */
  def numberOfScanlinesPerIdatChunk(width: Int): Int = {
    val bytesPerScanline = (width * bytesPerPixel) + filterByte
    val initialResult = thirtyTwoKilobytes / bytesPerScanline // deliberate use of int division
    if (initialResult > 0) initialResult else 1
  }

  def shouldPushDownstream(scanlinesPerChunk: Int, height: Int): Int => Boolean = scanlinesPulled => {
    scanlinesPulled == height || scanlinesPulled % scanlinesPerChunk == 0
  }


}
