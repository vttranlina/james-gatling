import io.gatling.app.Gatling
import io.gatling.core.config.GatlingPropertiesBuilder

object GatlingRunner extends App {

  val props: GatlingPropertiesBuilder = new GatlingPropertiesBuilder
  props.resourcesDirectory("/home/hp/workplace/james-gatling/src/main/scala-2.12")
  props.binariesDirectory("/home/hp/workplace/james-gatling/target/scala-2.12/classes")
  props.simulationClass("org.apache.james.gatling.simulation.jmap.rfc8621.PushPlatformValidationSimulation")

  Gatling.fromMap(props.build)
}