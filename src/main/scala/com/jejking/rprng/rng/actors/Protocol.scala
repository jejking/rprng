package com.jejking.rprng.rng.actors

import com.jejking.rprng.rng.Seed

/**
  * Defines additional types that can be sent to and from an [[RngActor]] in addition
  * to [[Seed]] and [[com.jejking.rprng.rng.RandomByteRequest]].
  */
object Protocol {

  /**
    * Instruction to the actor to ask for more seed to apply to the wrapped PRNG.
    */
  case object Reseed

  // errors
  sealed trait Error
  case object UnknownInputType extends Error
}
