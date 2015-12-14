package com.jejking.rprng.gatling

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Load and stress test for a locally running RPRNG web service. The intention
 * is to hammer it for a bit to see that it holds up and doesn't leak resources horribly
 * and behaves quite consistently.
 */
class RprngLoadSimulation extends Simulation {

  // most basic scenario, just grab 1kb of random bytes repeatedly, in parallel
  val httpConf = http.baseURL("http://localhost:8080")

  val scn = scenario("bytes")
    .exec(http("bytes").get("/byte/block").check(status.is(200)))

  setUp(scn.inject(rampUsersPerSec(10) to 250 during(10 minutes) randomized)
    .protocols(httpConf)).maxDuration(15 minutes)


  // lets go for:
  // - some randomly sized lists
  // - some randomly sized sets
  // - some blocks of bytes

  // do this over 30 minutes


}
