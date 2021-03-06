package esmonad

import cats.data.{EitherT, OptionT}
import cats.instances.future._
import org.scalatest.{AsyncFlatSpec, Matchers}

class V6Spec extends AsyncFlatSpec with Matchers {
  import esmonad.V6._
  import Sourced._

  "The V6 object" should "be valid" in {

    val err = "INVALID"

    def walkRight(dist: Int): Sourced[Turtle, TurtleEvent, Unit] =
      for {
        _ <- source(Turtle.walk(dist), err)
        _ <- source(Turtle.turn(ToRight), err)
      } yield ()

    {
      for {
        events <- EitherT.fromEither(
          (for {
            _ <- sourceNew[Turtle](Turtle.create("123", Position.zero, North))
            _ <- walkRight(1)
            _ <- walkRight(1)
            _ <- source(Turtle.walk(2), err)
          } yield ()).events(None)
        )
        _ <- EitherT.right(persist(events))

        state1 <- OptionT(hydrate[Turtle]("123")).toRight("not found")
        moreEvents <- EitherT.fromEither(
          (for {
            _ <- source(state1, Turtle.turn(ToRight), err)
            _ <- source(Turtle.walk(2), err)
          } yield ()).events(Some(state1))
        )
        _ <- EitherT.right(persist(moreEvents))

        state3 <- OptionT(hydrate[Turtle]("123")).toRight("not found")
      } yield state3
    }.value.map {
      _ shouldBe Right(Turtle("123", Position(-1, -1), West))
    }

  }

}
