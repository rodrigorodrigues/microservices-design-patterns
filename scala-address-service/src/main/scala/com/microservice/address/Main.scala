package com.microservice.address

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.http.scaladsl.Http
import com.microservice.address.routes.AddressRoutes
import com.microservice.address.repository.AddressRepository
import com.microservice.address.auth.JwtDirectives
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.{Bean, ComponentScan}
import org.springframework.data.mongodb.core.mapping.MongoMappingContext
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

@SpringBootApplication
@ComponentScan(basePackages = Array("com.microservice.address"))
@EnableReactiveMongoRepositories(basePackages = Array("com.microservice.address.repository"))
class ScalaAddressServiceConfig

object Main {
  def main(args: Array[String]): Unit = {
    val springContext = SpringApplication.run(classOf[ScalaAddressServiceConfig], args*)
    val repository = springContext.getBean(classOf[AddressRepository])
    val env = springContext.getEnvironment

    val jwksUrl = Option(env.getProperty("com.microservice.authentication.jwk.key-set-uri"))
    val jwtKeyValue = Option(env.getProperty("com.microservice.authentication.jwt.key-value"))
    val allowUnverified = Option(env.getProperty("com.microservice.authentication.jwt.allow-unverified"))
      .exists(_.equalsIgnoreCase("true"))
    val port = Option(env.getProperty("SERVER_PORT", "8085"))

    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "scala-address-service")
    implicit val executionContext: ExecutionContextExecutor = system.executionContext

    val jwtDirectives =
      try {
        new JwtDirectives(jwksUrl, jwtKeyValue, allowUnverified)
      } catch {
        case e: IllegalStateException =>
          system.log.error("Refusing to start: JWT verification is not configured", e)
          system.terminate()
          springContext.close()
          throw e
      }

    val addressRoutes = new AddressRoutes(repository, jwtDirectives)

    val bindingFuture = Http().newServerAt("0.0.0.0", Integer.valueOf(port.get)).bind(addressRoutes.routes)

    bindingFuture.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info(s"Server online at http://${address.getHostString}:${address.getPort}/")
      case Failure(ex) =>
        system.log.error(s"Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
        springContext.close()
    }
  }
}
