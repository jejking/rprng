package com.jejking.rprng.rng

import org.apache.commons.math3.random.RandomGenerator

import scala.reflect.ClassTag

/**
 * Provides functionality to instantiate various Commons Math [[org.apache.commons.math3.random.RandomGenerator]] types.
 */
object CommonsMathRandomGeneratorFactory {

  /**
   * Creates a new [[org.apache.commons.math3.random.RandomGenerator]] instance of the specified concrete type.
   * @param tag implicit class tag, supplied by compiler
   * @tparam G the type. Note that this *must* have a zero arg constructor.
   * @return new instance, as yet unseeded.
   */
  def createNewGeneratorInstance[G <: RandomGenerator]()(implicit tag: ClassTag[G]): G = {
    tag.runtimeClass.newInstance().asInstanceOf[G]
  }

}
