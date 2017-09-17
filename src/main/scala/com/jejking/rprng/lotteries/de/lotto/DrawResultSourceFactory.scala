package com.jejking.rprng.lotteries.de.lotto

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.jejking.rprng.api.ToSizedSet
import com.jejking.rprng.rng.{EightByteString, EightByteStringOps}

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
