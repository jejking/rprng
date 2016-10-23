package com.jejking.rprng.main

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.jejking.rprng.api.{AkkaRoutingHelper, Routes}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future
import scala.language.postfixOps

/**
  * Reads config, initialises actor system and starts web service.
  */
object Main {

  val randomRouterPath = "/user/randomRouter"

  private implicit val actorSystem = ActorSystem("rprng")
  private implicit val materializer = ActorMaterializer()

  def main(args: Array[String]): Unit = {
    val myConfig = processConfig(ConfigFactory.load())
    createAndStartServer(myConfig)
  }

  def createAndStartServer(myConfig: RprngConfig): Future[Http.ServerBinding] = {

    actorSystem.registerOnTermination(() -> println("shutting down actor system"))
    createRandomSourceActors(actorSystem, myConfig)

    val streamsHelper = new AkkaRoutingHelper()
    val route = new Routes(streamsHelper).route

    Http().bindAndHandle(route, "0.0.0.0", myConfig.port)
  }

  def shutdown(): Unit = {
    actorSystem.terminate()
  }

}
