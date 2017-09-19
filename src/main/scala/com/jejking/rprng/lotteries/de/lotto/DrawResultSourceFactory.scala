package com.jejking.rprng.lotteries.de.lotto

import java.time.Clock
import java.time.temporal.{ChronoField, ChronoUnit, Temporal, TemporalUnit}
import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.jejking.rprng.api.ToSizedSet
import com.jejking.rprng.rng.{EightByteString, EightByteStringOps}

import scala.concurrent.duration.FiniteDuration

class DrawResultSourceFactory(eightByteStringSource: Source[EightByteString, NotUsed]) {

  private val numbersSource = createNumbersSource()
  private val superNumberSource = createSuperNumberSource()

  def drawResultSource(): Source[DrawResult, NotUsed] = {
    numbersSource.zipWith(superNumberSource)(DrawResult(_, _))
  }



  private def createNumbersSource(): Source[Set[Int], NotUsed] = {
    eightByteStringSource
      .map(ebs => EightByteStringOps.toInt(ebs, 1, 49))
      .via(new ToSizedSet(6))
  }

  private def createSuperNumberSource(): Source[Int, NotUsed] = {
    eightByteStringSource
      .map(ebs => EightByteStringOps.toInt(ebs, 0, 9))
  }

}

object DrawResultSourceFactory {


  def computeInitialDelay(clock: Clock): FiniteDuration = {
    val now = clock.instant()
    val nextWholeMinute = now.plus(1, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES)
    val millis = now.until(nextWholeMinute, ChronoUnit.MILLIS)
    FiniteDuration(millis, TimeUnit.MILLISECONDS)
  }
}
