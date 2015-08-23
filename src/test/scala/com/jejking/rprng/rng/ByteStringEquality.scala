package com.jejking.rprng.rng

import akka.util.ByteString
import org.scalactic.Equality
import org.scalactic.TripleEquals._

/**
 * Created by jking on 23/08/15.
 */
trait ByteStringEquality {

  implicit val byteStringEquality = new Equality[ByteString] {
    override def areEqual(a: ByteString, b: Any): Boolean = {
      System.err.println(a, b)
      b match {
        case bs: ByteString => a.toIterable === bs.toIterable
        case _ => false
      }
    }
  }

}
