package com.jejking.rprng.rng

import org.apache.commons.math3.random.AbstractRandomGenerator

/**
 * Created by jking on 23/08/15.
 */
class FixedApacheRandomGenerator(retValue: Double = 0d) extends AbstractRandomGenerator {

  override def setSeed(seed: Long): Unit = {}

  override def nextDouble(): Double = retValue
}

object FixedApacheRandomGenerator {

  def apply(): FixedApacheRandomGenerator = new FixedApacheRandomGenerator()

  def apply(retValue: Double): FixedApacheRandomGenerator = new FixedApacheRandomGenerator(retValue)
}