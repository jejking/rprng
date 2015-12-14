package com.jejking.rprng.api

import akka.actor.ActorSystem.Settings
import akka.actor._
import akka.dispatch.{Dispatchers, Mailboxes}
import akka.event.{LoggingAdapter, EventStream}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.testkit.scaladsl.TestSink
import com.jejking.rprng.rng.RandomGeneratorFactory
import org.apache.commons.math3.random.MersenneTwister
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Inspectors, BeforeAndAfterAll, Matchers, FlatSpec}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * Tests [[ToSizedSet]] custom streams processing stage.
 */
class ToSizedSetSpec extends FlatSpec with Matchers with Inspectors with ScalaFutures with BeforeAndAfterAll {

  implicit val system = ActorSystem("test")
  implicit val materializer = ActorMaterializer()


  "the stage" should "produce a single set of right size from a sequence of integers when requested" in {
    val toSizedSet = ToSizedSet(5)
    val set: Future[Set[Int]] = Source(1 to 10)
                                .transform(() => toSizedSet)
                                .take(1)
                                .toMat(Sink.head)(Keep.right)
                                .run
    whenReady(set) {
      s => s shouldBe Set(1, 2, 3, 4, 5)
    }
  }

  it should "produce multiple sets of right size from a sequence of integers when requested" in {
    val toSizedSet = ToSizedSet(5)
      Source(1 to 20)
              .transform(() => toSizedSet)
              .runWith(TestSink.probe[Set[Int]])
              .request(2)
              .expectNext(Set(1,2,3,4,5), Set(6,7,8,9,10))
  }

  it should "produce multiple sets of right size from a random source of integers" in {

    val randomIterator = new Iterator[Int] {

      val rng = RandomGeneratorFactory.createNewGeneratorInstance[MersenneTwister]

      override def hasNext: Boolean = true

      override def next(): Int = rng.nextInt(100)
    }

    val toSizedSet = ToSizedSet(5)

    val futureSeq: Future[Seq[Set[Int]]] = Source(() => randomIterator)
                                    .transform(() => toSizedSet)
                                    .take(100)
                                    .grouped(100)
                                    .toMat(Sink.head)(Keep.right)
                                    .run
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
