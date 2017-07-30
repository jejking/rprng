package com.jejking.rprng.png

import akka.NotUsed
import akka.actor.ActorSelection
import akka.stream.{Graph, SourceShape}
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.jejking.rprng.rng.ByteStringSource

object PngSourceFactory {

 val BYTES_PER_PIXEL = 4 // RGBA

  def pngSource(rngActorSelection: ActorSelection)(width: Int, height: Int): Source[ByteString, NotUsed] = {

    val sourceGraph: Graph[SourceShape[ByteString], NotUsed] = new ByteStringSource(rngActorSelection, bytesPerLine(width))
    val toScanline = Png.scanline(BYTES_PER_PIXEL, width)

    Source.fromGraph(sourceGraph)
      .map(toScanline(_))
      .via(new IdatStage(width, height))
      .via(new PngStage(width, height))
  }

  private def bytesPerLine(width: Int) = {
    BYTES_PER_PIXEL * width
  }
}
