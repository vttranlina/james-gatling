package org.apache.james.gatling.simulation.jmap.rfc8621

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import org.apache.james.gatling.jmap.rfc8621.scenari.WebsocketEchoScenario
import org.apache.james.gatling.simulation.Configuration.{InjectionDuration, MaxDuration, ScenarioDuration, UserCount}
import org.apache.james.gatling.simulation.{HttpSettings, UsersFeederCSVFactory}

class WebsocketEchoSimulation extends Simulation {
  private val scenario: WebsocketEchoScenario = new WebsocketEchoScenario()
  private val feederFactory: UsersFeederCSVFactory = new UsersFeederCSVFactory().loadUsers

  setUp(scenario.generateEcho(
      duration = ScenarioDuration,
      userFeeder = feederFactory.userFeeder())
    .inject(rampUsers(UserCount) during InjectionDuration)
    .protocols(HttpSettings.httpProtocol))
    .maxDuration(MaxDuration)
}
