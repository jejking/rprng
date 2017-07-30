package com.jejking.rprng.png

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FileIO
import com.jejking.rprng.main.{RprngConfig, createRandomSourceActors}
import com.jejking.rprng.rng.actors.TimeRangeToReseed

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by jking on 13/07/2017.
  */
object PngMain {

  def main(args: Array[String]): Unit = {

    import scala.concurrent.ExecutionContext.Implicits.global
    implicit val actorSystem = ActorSystem("RandomByteToStandardOut")
    implicit val materializer = ActorMaterializer()

    val routerActorRef = createRandomSourceActors(actorSystem, RprngConfig(0, TimeRangeToReseed(1 hour, 8 hours), 8))

    val pngSourceFactory = PngSourceFactory.pngSource(actorSystem.actorSelection(routerActorRef.path)) _

    val width = 100
    val height = 400

    val future = pngSourceFactory(width, height)
        .runWith(FileIO.toPath(Paths.get("/tmp/streamingPng.png")))

    future.foreach(_ => {
      println("done")
      actorSystem.terminate()
    })

  }

}
