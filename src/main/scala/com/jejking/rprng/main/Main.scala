package com.jejking.rprng.main

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.SystemMaterializer
import com.jejking.rprng.api.{AkkaRngStreaming, Routes}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future
import scala.language.postfixOps

/**
  * Reads config, initialises actor system and starts web service.
  */
object Main {

  val randomRouterPath = "/user/randomRouter"

  private implicit val actorSystem = ActorSystem("rprng")
  private implicit val materializer = SystemMaterializer.get(actorSystem)

  def main(args: Array[String]): Unit = {
    val myConfig = processConfig(ConfigFactory.load())
    createAndStartServer(myConfig)
  }

  def createAndStartServer(myConfig: RprngConfig): Future[Http.ServerBinding] = {

    actorSystem.registerOnTermination(() -> println("shutting down actor system"))
    createRandomSourceActors(actorSystem, myConfig)

    val streamsHelper = new AkkaRngStreaming(actorSystem.actorSelection(randomRouterPath))
    val route = new Routes(streamsHelper).route

    Http().newServerAt("0.0.0.0", myConfig.port).bindFlow(route)
  }

  def shutdown(): Unit = {
    actorSystem.terminate()
  }

}
