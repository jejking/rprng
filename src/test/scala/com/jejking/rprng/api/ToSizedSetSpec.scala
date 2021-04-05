package com.jejking.rprng.api

import akka.actor._
import akka.stream.{ActorMaterializer, SystemMaterializer}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.testkit.scaladsl.TestSink
import com.jejking.rprng.rng.CommonsMathRandomGeneratorFactory
import org.apache.commons.math3.random.MersenneTwister
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.SpanSugar._
import org.scalatest.{BeforeAndAfterAll, Inspectors}

import scala.concurrent.Future

/**
 * Tests [[ToSizedSet]] custom streams processing stage.
 */
class ToSizedSetSpec extends AnyFlatSpec with Matchers with Inspectors with ScalaFutures with BeforeAndAfterAll {

  implicit override val patienceConfig = PatienceConfig(timeout = 1 second, interval = 100 milliseconds)
  implicit val system = ActorSystem("test")
  implicit val materializer = SystemMaterializer.get(system)


  "the stage" should "produce a single set of right size from a sequence of integers when requested" in {
    val toSizedSet = ToSizedSet(5)
    val set: Future[Set[Int]] = Source(1 to 10)
                                .via(toSizedSet)
                                .take(1)
                                .toMat(Sink.head)(Keep.right)
                                .run()
    whenReady(set) {
      s => s shouldBe Set(1, 2, 3, 4, 5)
    }
  }

  it should "produce multiple sets of right size from a sequence of integers when requested" in {
    val toSizedSet = ToSizedSet(5)
      Source(1 to 20)
              .via(toSizedSet)
              .runWith(TestSink.probe[Set[Int]])
              .request(2)
              .expectNext(Set(1,2,3,4,5), Set(6,7,8,9,10))
  }

  it should "produce multiple sets of right size from a random source of integers" in {

    val randomIterator = new Iterator[Int] {

      val rng = CommonsMathRandomGeneratorFactory.createNewGeneratorInstance[MersenneTwister]

      override def hasNext: Boolean = true

      override def next(): Int = rng.nextInt(100)
    }

    val toSizedSet = ToSizedSet(5)

    val futureSeq: Future[Seq[Set[Int]]] = Source.fromIterator(() => randomIterator)
                                    .via(toSizedSet)
                                    .take(100)
                                    .grouped(100)
                                    .toMat(Sink.head)(Keep.right)
                                    .run()
    whenReady(futureSeq) {
      seq => {
        seq should have size 100
        forAll(seq) {
          set => set should have size 5
          forAll(set) {
            i => i should (be >= 0 and be < 100)
          }
        }

      }

    }
  }

  override def afterAll(): Unit = {
    this.system.terminate()
  }

}
