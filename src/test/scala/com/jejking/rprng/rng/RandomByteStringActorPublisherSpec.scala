package com.jejking.rprng.rng

import org.scalatest.{Matchers, FlatSpec}

/**
 * Tests for [[RandomByteStringActorPublisher]].
 */
class RandomByteStringActorPublisherSpec extends FlatSpec with Matchers {

  "the random byte string actor publisher" should "provide a stream of byte strings from wrapped actor path" in {

    // configure something to pass out arrays of 0 behind actor path
    fail("not done")
  }

  it should "notify the subscriber if attempt to get a byte string from the wrapped actor path fails" in {
    fail("not done")
  }

  it should "stop itself when it receives a cancel message from the subscriber" in {
    fail("not done")
  }

}
