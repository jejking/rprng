package com.jejking.rprng.png

import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.util.{ByteString, ByteStringBuilder}

/**
  * Created by jking on 23/07/2017.
  */
class IdatStage(width: Int, height: Int) extends GraphStage[FlowShape[ByteString, ByteString]] {

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

  def numberOfScanlinesPerIdatChunk(width: Int): Int = {
    val bytesPerScanline = (width * bytesPerPixel) + filterByte
    val initialResult = thirtyTwoKilobytes / bytesPerScanline // deliberate use of int division
    if (initialResult > 0) initialResult else 1
  }

  def shouldPushDownstream(scanlinesPerChunk: Int, height: Int): Int => Boolean = scanlinesPulled => {
    scanlinesPulled == height || scanlinesPulled % scanlinesPerChunk == 0
  }


}
