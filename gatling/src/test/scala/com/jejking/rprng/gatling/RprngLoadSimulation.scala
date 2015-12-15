package com.jejking.rprng.gatling

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.util.Random

/**
 * Load and stress test for a locally running RPRNG web service. The intention
 * is to hammer it for a bit to see that it holds up and doesn't leak resources horribly
 * and behaves quite consistently.
 */
class RprngLoadSimulation extends Simulation {

  val httpConf = http.baseURL("http://localhost:8080")

  val byteBlockSizeFeeder = Iterator.continually(Map("blockSize" -> (Random.nextInt(768) + 256)))

  val bytesScn = scenario("bytes")
                  .feed(byteBlockSizeFeeder)
                  .exec(http("byte block")
                    .get("/byte/block/${blockSize}")
                    .check(status.is(200)))



  // size, count, min, max
  val collectionFeeder = Iterator.continually(Map("val" -> Map("size" -> (Random.nextInt(10) + 1),
                                                              "count" -> (Random.nextInt(10) + 1),
                                                              "min" -> Random.nextInt(10),
                                                              "max" -> (Random.nextInt(100) + 100))))


  val intListScn = scenario("int list")
                    .feed(collectionFeeder)
                    .exec(http("int list")
                      .get("/int/list")
                      .queryParamMap("${val}")
                      .check(status.is(200)))

  val intSetScn = scenario("int set")
                  .feed(collectionFeeder)
                  .exec(http("int set")
                    .get("/int/set")
                    .queryParamMap("${val}")
                    .check(status.is(200)))

  val scnList = List(bytesScn, intListScn, intSetScn)


  setUp(scnList.map(scn =>
                    scn.inject(constantUsersPerSec(100) during (30 minutes))
                    .protocols(httpConf)))
    .throttle(reachRps(300) in (10 minutes), holdFor(20 minutes))
    .maxDuration(30 minutes)


}
