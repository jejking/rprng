package com.jejking.rprng.rng

import org.apache.commons.math3.random.AbstractRandomGenerator

/**
 * Created by jking on 23/08/15.
 */
class FixedApacheRandomGenerator(retValue: Double = 0d) extends AbstractRandomGenerator {

  def this() {
    this(0d)
  }

  override def nextBytes(array: Array[Byte]) = {
    for (i <- 0 to array.length - 1) {
      array(i) = 0
    }
  }

  override def setSeed(seed: Long): Unit = {}

  override def nextDouble(): Double = retValue
}

object FixedApacheRandomGenerator {

  def apply(): FixedApacheRandomGenerator = new FixedApacheRandomGenerator()

  def apply(retValue: Double): FixedApacheRandomGenerator = new FixedApacheRandomGenerator(retValue)
}

class RandomGeneratorWithObservableSeed extends AbstractRandomGenerator {



  var seed: Long = -1

  override def setSeed(seed: Long): Unit = {
    this.seed = seed
  }

  override def nextDouble(): Double = ???
}