package esmonad

import cats.data.{EitherT, OptionT}
import cats.instances.future._
import org.scalatest.{AsyncFlatSpec, Matchers}

import scala.concurrent.Future

class V7Spec extends AsyncFlatSpec with Matchers {

  import esmonad.V7App._
  import Sourced._

  "The V7 object" should "be valid" in {
    val id = "123"
    def walkRight = {
      source(Turtle.walk(1)) and
      source(Turtle.turn(ToRight))
    }

    (for {
      _ <- EitherT.fromEither[Future] {
        (
          sourceNew(Turtle.create(id, Position.zero, North)) and
          walkRight and
          walkRight and
          source(Turtle.walk(2))
        ).events(Turtle.handler)
      }.semiflatMap(persist(_))

      state1 <- OptionT(hydrate[Turtle](id)).toRight("not found")
      _ <- EitherT.fromEither[Future] {
        Sourced.events(state1, Turtle.handler) { // alternative syntax
          source(Turtle.turn(ToRight)) and
          source(Turtle.walk(2))
        }
      }.semiflatMap(persist(_))

      state2 <- OptionT(hydrate[Turtle](id)).toRight("not found")
    } yield state2
      ).value.map {
      _ shouldBe Right(Turtle("123", Position(-1, -1), West))
    }
  }

}