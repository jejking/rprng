package com.jejking.rprng.rng

import akka.util.ByteString
import org.scalatest.{Matchers, FlatSpec}

/**
 * Created by jking on 23/08/15.
 */
class ByteStringEqualitySpec extends FlatSpec with Matchers with ByteStringEquality {

  "the implicit byte string equality" should "find two equal byte strings equal" in {
    val a = ByteString(1, 2, 3, 4)
    val b = ByteString(1, 2, 3, 4)

    a shouldBe b
  }

  it should "find them different byte strings not equal" in {
    val a = ByteString(1, 2, 3)
    val b = ByteString(6, 6, 6)

    a shouldNot be (b)
  }

  it should "find a string and a byte string not equal" in {
    val a = ByteString(1, 2, 3)
    val b = "1, 2, 3    "

    a shouldNot be (b)
  }
}
