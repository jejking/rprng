package zprng.demos

import zio.*
import zprng.*

import scala.collection.immutable.SortedSet

/** Base for Lotto demos.
  */
trait LottoDemo extends ZIOAppDefault:
  def draw(
    name: String,
    count: Int,
    max: Int,
    extraName: Option[String] = None,
    extraMax: Option[Int] = None
  ): ZIO[RandomService, Nothing, Unit] =
    for {
      service <- ZIO.service[RandomService]
      numbers <- pickUnique(count, max, service)
      extra <- extraName match
        case Some(_) => service.nextInt(extraMax.get).map(v => Some(v + 1))
        case None    => ZIO.none
      _ <- Console.printLine(s"--- $name ---").orDie
      _ <- Console.printLine(s"Main numbers: ${numbers.mkString(", ")}").orDie
      _ <- extra match
        case Some(v) => Console.printLine(s"${extraName.get}: $v").orDie
        case None    => ZIO.unit
    } yield ()

  protected def pickUnique(
    count: Int,
    max: Int,
    service: RandomService
  ): ZIO[Any, Nothing, SortedSet[Int]] =
    def loop(acc: SortedSet[Int]): ZIO[Any, Nothing, SortedSet[Int]] =
      if (acc.size == count) ZIO.succeed(acc)
      else service.nextInt(max).flatMap(v => loop(acc + (v + 1)))
    loop(SortedSet.empty)

object GermanLotto extends LottoDemo:
  def run = draw("German Lotto (6aus49)", 6, 49, Some("Superzahl"), Some(10))
    .provide(RandomService.live, EntropySource.live)

object EuroMillions extends LottoDemo:
  def run = draw("EuroMillions", 5, 50, Some("Stars (2 pick)"), Some(12))
    .provide(RandomService.live, EntropySource.live)

  override def draw(
    name: String,
    count: Int,
    max: Int,
    extraName: Option[String] = None,
    extraMax: Option[Int] = None
  ): ZIO[RandomService, Nothing, Unit] =
    if (name == "EuroMillions" || name == "Eurojackpot") {
      for {
        service <- ZIO.service[RandomService]
        numbers <- pickUnique(count, max, service)
        stars   <- pickUnique(2, extraMax.get, service)
        _       <- Console.printLine(s"--- $name ---").orDie
        _       <- Console.printLine(s"Main numbers: ${numbers.mkString(", ")}").orDie
        _       <- Console.printLine(s"Stars/Euronumbers: ${stars.mkString(", ")}").orDie
      } yield ()
    } else super.draw(name, count, max, extraName, extraMax)

object Eurojackpot extends LottoDemo:
  def run = draw("Eurojackpot", 5, 50, Some("Euronumbers (2 pick)"), Some(12))
    .provide(RandomService.live, EntropySource.live)

  override def draw(
    name: String,
    count: Int,
    max: Int,
    extraName: Option[String] = None,
    extraMax: Option[Int] = None
  ): ZIO[RandomService, Nothing, Unit] =
    EuroMillions.draw(name, count, max, extraName, extraMax)
