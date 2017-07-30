package com.jejking.rprng.png

import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.util.ByteString

/**
  * Graph stage to emit PNG signature, IHDR, IDAT and IEND chunks.
  *
  * The stage starts by emitting the signature and IHDR chunks. Next, all the IDAT chunks that
  * represent the input to the stage are consumed and passed downstream. When the input source
  * is exhausted, the IEND chunk is emitted and the stage completes.
  *
  * @param width of the PNG to be generated
  * @param height of the PNG to be generated
  */
private[png] class PngStage(width: Int, height: Int) extends GraphStage[FlowShape[ByteString, ByteString]] {

  val in = Inlet[ByteString]("Idat.in")
  val out = Outlet[ByteString]("Png.out")

  override def shape: FlowShape[ByteString, ByteString] = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    var sentPngSignature = false
    var sentIhdr = false

    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        val incoming = grab(in)
        push(out, incoming)
      }

      override def onUpstreamFinish(): Unit = {
        emit(out, Png.iend())
        completeStage()
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        if (!sentPngSignature) {
          push(out, Png.PNG_SIGNATURE)
          sentPngSignature = true
        } else if (!sentIhdr) {
          push(out, Png.ihdr(width, height))
          sentIhdr = true
        } else {
          pull(in)
        }

      }
    })

  }
}

