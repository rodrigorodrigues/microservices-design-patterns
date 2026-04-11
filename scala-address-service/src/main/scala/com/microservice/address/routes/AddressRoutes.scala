package com.microservice.address.routes

import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.marshalling.ToResponseMarshaller
import org.apache.pekko.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import com.microservice.address.repository.AddressRepository
import com.microservice.address.models.Address
import com.microservice.address.models.AddressJsonProtocol._
import com.microservice.address.auth.JwtDirectives
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._
import scala.concurrent.ExecutionContext
import reactor.core.publisher.Mono
import reactor.core.publisher.Flux
import scala.jdk.FutureConverters._
import scala.concurrent.Future

import scala.jdk.CollectionConverters._
import org.apache.pekko.http.scaladsl.unmarshalling.Unmarshaller

trait JsonSupport {
  import org.apache.pekko.http.scaladsl.marshalling.Marshaller
  import org.apache.pekko.http.scaladsl.model.ContentTypes
  import org.apache.pekko.http.scaladsl.model.HttpEntity
  import org.apache.pekko.http.scaladsl.model.HttpResponse

  implicit def circeMarshaller[A : io.circe.Encoder]: ToResponseMarshaller[A] =
    Marshaller.opaque { a =>
      HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, a.asJson.noSpaces))
    }

  implicit def circeUnmarshaller[A : io.circe.Decoder]: FromRequestUnmarshaller[A] =
    Unmarshaller.messageUnmarshallerFromEntityUnmarshaller(
      Unmarshaller.stringUnmarshaller.forContentTypes(ContentTypes.`application/json`).map { str =>
        decode[A](str).getOrElse(throw new Exception("Invalid JSON"))
      }
    )
}

class AddressRoutes(repository: AddressRepository)(implicit ec: ExecutionContext) extends JwtDirectives with JsonSupport {

  private def toFuture[T](mono: Mono[T]): Future[Option[T]] = 
    mono.toFuture().asScala.map(Option.apply)

  private def fluxToFuture[T](flux: Flux[T]): Future[Seq[T]] =
    flux.collectList().toFuture().asScala.map(_.asScala.toSeq)

  val routes = pathPrefix("api" / "addresses") {
    authenticate { claims =>
      concat(
        get {
          concat(
            pathEndOrSingleSlash {
              authorizeRoles("ROLE_ADMIN")(claims) { _ =>
                complete(fluxToFuture(repository.findAll()))
              }
            },
            path(Segment) { id =>
              authorizeRoles("ROLE_PERSON_READ", "ROLE_ADMIN")(claims) { _ =>
                onSuccess(toFuture(repository.findById(id))) {
                  case Some(addr) => complete(addr)
                  case None       => complete(StatusCodes.NotFound)
                }
              }
            }
          )
        },
        post {
          pathEndOrSingleSlash {
            authorizeRoles("ROLE_PERSON_SAVE", "ROLE_ADMIN")(claims) { _ =>
              entity(as[Address]) { addr =>
                complete(repository.save(addr).toFuture().asScala.map(_.id.getOrElse("")))
              }
            }
          }
        },
        delete {
          path(Segment) { id =>
            authorizeRoles("ROLE_ADMIN")(claims) { _ =>
              complete(repository.deleteById(id).toFuture().asScala.map(_ => StatusCodes.NoContent))
            }
          }
        }
      )
    }
  }
}
