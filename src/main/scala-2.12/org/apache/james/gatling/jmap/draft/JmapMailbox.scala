package org.apache.james.gatling.jmap.draft

import com.fasterxml.jackson.databind.JsonNode
import fabricator.Words
import io.gatling.core.Predef._
import io.gatling.core.check.CheckBuilder
import io.gatling.core.check.jsonpath.JsonPathCheckType
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import io.gatling.http.check.HttpCheck
import io.gatling.http.request.builder.HttpRequestBuilder
import org.apache.james.gatling.utils.RandomStringGenerator
import play.api.libs.json.{JsResult, JsValue, Json, Reads}

object MailboxId {
  implicit val reads = new Reads[MailboxId] {
    override def reads(json: JsValue): JsResult[MailboxId] = json.validate[String].map(MailboxId.apply)
  }

  def generate(): MailboxId =
    MailboxId(RandomStringGenerator.randomString)
}

case class MailboxId(id: String) extends AnyVal

object MailboxName {
  implicit val reads = new Reads[MailboxName] {
    override def reads(json: JsValue): JsResult[MailboxName] = json.validate[String].map(MailboxName.apply)
  }
  private val words = Words()

  def generate(): MailboxName =
    MailboxName(words.words(2).mkString("_"))
}

case class MailboxName(name: String) extends AnyVal

case class JmapMailbox(id: MailboxId, name: MailboxName)

object JmapMailbox {

  implicit val reads = Json.reads[JmapMailbox]

  val outboxMailboxIdSessionParam = "outboxMailboxId"

  private val mailboxListPath = "[0][1].list"
  private def mailboxesIdPathForMailboxesWithAtLeastMessages(nbMessages : Int) = s"$$$mailboxListPath[?(@.totalMessages >= $nbMessages)].id"
  private val inboxIdPath = s"$$$mailboxListPath[?(@.role == 'inbox')].id"
  private val outboxIdPath = s"$$$mailboxListPath[?(@.role == 'outbox')].id"
  private val sentIdPath = s"$$$mailboxListPath[?(@.role == 'sent')].id"
  private val draftsIdPath = s"$$$mailboxListPath[?(@.role == 'drafts')].id"
  private val trashIdPath = s"$$$mailboxListPath[?(@.role == 'trash')].id"
  private val spamIdPath = s"$$$mailboxListPath[?(@.role == 'spam')].id"
  private val archiveIdPath = s"$$$mailboxListPath[?(@.role == 'archive')].id"
  val numberOfSystemMailboxes = 7

  def getMailboxes(): HttpRequestBuilder =
    JmapAuthentication.authenticatedQuery("getMailboxes", "/jmap")
      .body(StringBody("""[["getMailboxes", {}, "#0"]]"""))

  def createMailbox(): HttpRequestBuilder =
    JmapAuthentication.authenticatedQuery("setMailboxes", "/jmap")
      .body(StringBody(
        s"""[["setMailboxes",
            {
              "create": {
                "$${createdId}": {
                  "name": "$${mailboxName}"
                }
              }
            }, "#0"]]"""))
      .check(saveMailboxId())

  def saveMailboxId(): CheckBuilder[JsonPathCheckType, JsonNode, Seq[String]] = {
    jsonPath(s"""$$[0][1].created..$${createdId}.id""").findAll.saveAs("mailboxId")
  }

  val getMailboxesChecks: Seq[HttpCheck] = List(
    status.is(200),
    JmapChecks.noError)

  def assertNumberOfMailboxes(numberOfMailboxes: Int): HttpCheck =
    jsonPath(s"$$$mailboxListPath[*].id").count.is(numberOfMailboxes)

  def getMailboxesChecks(expectedNumberOfMailboxes: Int): Seq[HttpCheck] =
    getMailboxesChecks :+ assertNumberOfMailboxes(expectedNumberOfMailboxes)

  def checkSystemMailboxIdsHaveNotChanged(): Seq[HttpCheck] =
    getMailboxesChecks ++ List[HttpCheck](
      jsonPath(inboxIdPath).is("${inboxMailboxId}"),
      jsonPath(outboxIdPath).is(s"$${$outboxMailboxIdSessionParam}"),
      jsonPath(sentIdPath).is("${sentMailboxId}"),
      jsonPath(draftsIdPath).is("${draftMailboxId}"),
      jsonPath(trashIdPath).is("${trashMailboxId}"),
      jsonPath(spamIdPath).is("${spamMailboxId}"),
      jsonPath(archiveIdPath).is("${archiveMailboxId}")
    )

  def saveInboxAs(key: String): Seq[HttpCheck] = List(jsonPath(inboxIdPath).saveAs(key))

  def saveRandomMailboxWithAtLeastMessagesAs(key: String, atLeastMessages : Int): Seq[HttpCheck] = List(jsonPath(mailboxesIdPathForMailboxesWithAtLeastMessages(atLeastMessages))
    .findRandom
    .saveAs(key))

  val getSystemMailboxesChecks: Seq[HttpCheck] = getMailboxesChecks ++
    saveInboxAs("inboxMailboxId") ++
    List[HttpCheck](
      jsonPath(inboxIdPath).saveAs("inboxMailboxId"),
      jsonPath(outboxIdPath).saveAs(outboxMailboxIdSessionParam),
      jsonPath(sentIdPath).saveAs("sentMailboxId"),
      jsonPath(draftsIdPath).saveAs("draftMailboxId"),
      jsonPath(trashIdPath).saveAs("trashMailboxId"),
      jsonPath(spamIdPath).saveAs("spamMailboxId"),
      jsonPath(archiveIdPath).saveAs("archiveMailboxId"))

  def storeMailboxIds(): Seq[HttpCheck] = getSystemMailboxesChecks

  def getSystemMailboxes(): HttpRequestBuilder = getMailboxes

  def getSystemMailboxesWithRetryAuthentication(): ChainBuilder = RetryAuthentication.execWithRetryAuthentication(getSystemMailboxes(), getSystemMailboxesChecks)

  def getSystemMailboxesWithChecks(): HttpRequestBuilder = getSystemMailboxes().check(getSystemMailboxesChecks: _*)
}
