package org.apache.james.gatling.jmap.rfc8621.scenari

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import org.apache.james.gatling.control.UserFeeder.UserFeederBuilder
import org.apache.james.gatling.jmap.rfc8621.JmapWebsocket.{echoPingWs, enablePush, websocketClose, websocketConnect}
import org.apache.james.gatling.jmap.rfc8621.{JmapMailbox, SessionStep}

import scala.concurrent.duration._

class WebsocketEchoScenario {
  def generateEcho(duration: Duration, userFeeder: UserFeederBuilder): ScenarioBuilder =
    scenario("WebsocketEchoScenario")
      .feed(userFeeder)
      .exec(SessionStep.retrieveAccountId)
      .exec(JmapMailbox.provisionSystemMailboxes())
      .exec(websocketConnect.onConnected(
        exec(enablePush)
          .during(duration.toSeconds.toInt) {
            exec(exec(echoPingWs.await(3 seconds)(
              ws.checkTextMessage("ping").check(jsonPath("$.methodResponses[0][1].ping").is("dummy")))))
              .pause(20 second)
          }))
      .exec(websocketClose)

  def generateNoEcho(duration: Duration, userFeeder: UserFeederBuilder): ScenarioBuilder =
    scenario("WebsocketNoEchoScenario")
      .feed(userFeeder)
      .exec(SessionStep.retrieveAccountId)
      .exec(JmapMailbox.provisionSystemMailboxes())
      .exec(websocketConnect.onConnected(
        exec(enablePush)
          .pause((duration.toSeconds - 60).seconds)))
      .exec(echoPingWs.await(3 seconds)(
        ws.checkTextMessage("ping").check(jsonPath("$.methodResponses[0][1].ping").is("dummy"))))
      .exec(websocketClose)
}
