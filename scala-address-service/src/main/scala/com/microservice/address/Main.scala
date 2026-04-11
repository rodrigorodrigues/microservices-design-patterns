package com.microservice.address

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.http.scaladsl.Http
import com.microservice.address.routes.AddressRoutes
import com.microservice.address.repository.AddressRepository
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
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

    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "scala-address-service")
    implicit val executionContext: ExecutionContextExecutor = system.executionContext

    val addressRoutes = new AddressRoutes(repository)

    val bindingFuture = Http().newServerAt("0.0.0.0", 8085).bind(addressRoutes.routes)

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
