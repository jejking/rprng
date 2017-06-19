package com.jejking.rprng.main

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.{ByteArrayOutputStream, File}
import java.nio.file.Paths
import javax.imageio.ImageIO

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Graph, IOResult, SourceShape}
import akka.stream.scaladsl.{FileIO, Keep, Sink, Source, StreamConverters}
import akka.util.ByteString
import com.jejking.rprng.rng.ByteStringSource
import com.jejking.rprng.rng.actors.TimeRangeToReseed

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by jking on 14/06/2017.
  */
object ImageSpike {

    def main(args: Array[String]): Unit = {

      import RgbPixel._
      import scala.concurrent.ExecutionContext.Implicits.global
      val width = 500;
      val height = 200;

      val img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

      implicit val actorSystem = ActorSystem("RandomByteToStandardOut")
      implicit val materializer = ActorMaterializer()

      val routerActorRef = createRandomSourceActors(actorSystem, RprngConfig(0, TimeRangeToReseed(1 hour, 8 hours), 8))
      val byteSourceGraph: Graph[SourceShape[ByteString], NotUsed] = new ByteStringSource(actorSystem.actorSelection(routerActorRef.path), 4)

      val colorSource: Source[Color, NotUsed] = Source
        .fromGraph(byteSourceGraph)
        .map(toRgbInt(_))
        .map(i => new Color(i))


      val xSource = Source(0 to width - 1)

      val fileSink: Sink[ByteString, Future[IOResult]] = FileIO.toPath(Paths.get("/tmp/random.png"))

      xSource.map(num => ByteString(s"$num\n"))
        .runWith(fileSink)

      val graph = xSource
                  .flatMapConcat(x => ySource(x, height))
                  .zip(colorSource)
                  .map(xyRgb => PixelData(xyRgb._1._1, xyRgb._1._2, xyRgb._2))
                  .fold(img)((img, pd) => {img.setRGB(pd.x, pd.y, pd.color.getRGB); img})
                  .map(bi => {
                    val os = new ByteArrayOutputStream()
                    ImageIO.write(bi, "png", os)
                    ByteString(os.toByteArray)
                  })
                  .runWith(fileSink)
                  .foreach(ior => {
                    println("got an IOResult")
                    println(ior.getCount)
                    materializer.shutdown()
                    actorSystem.terminate()
                  })
                  /*.flatMapConcat(bi => StreamConverters
                                        .asOutputStream()
                                        .mapMaterializedValue(os => {
                                          println("writing image")
                                          ImageIO.write(bi, "png", os)
                                          println("written image")
                                        }))
                  .runWith(fileSink)
                  .foreach((ior => {
                    println("got an IOResult")
                    println(ior.getError)
                    materializer.shutdown()
                    actorSystem.terminate()
                  }))*/

    }




    def ySource(x: Int, height: Int): Source[(Int, Int), NotUsed] = {
      Source(0 to height - 1).map(y => (x, y))
    }

    case class PixelData(x: Int, y: Int, color: Color)

    object RgbPixel {

      def toRgbInt(fourByteString: ByteString): Int = {
        val bytes = fourByteString.toArray
        ((bytes(0) & 0xFF) << 24) | ((bytes(1) & 0xFF) << 16) | ((bytes(2) & 0xFF) << 8) | (bytes(3) & 0xFF)
      }

    }
}
