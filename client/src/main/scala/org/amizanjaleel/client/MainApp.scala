// client/src/main/scala/org/amizanjaleel/client/MainApp.scala
package org.amizanjaleel.client

import org.scalajs.dom // Provides document, window, Element, fetch, Response, etc.
// No longer need: import org.scalajs.dom.ext.Ajax

import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Failure, Success}
import scala.scalajs.js // Required for JS Promise <-> Future conversion

// Import Circe parser and shared data model
import io.circe.parser.decode
import org.amizanjaleel.DataModel
import org.amizanjaleel.DataModel.Item

object MainApp {

  // Use the standard Scala.js execution context for Futures
  implicit val ec: ExecutionContext = scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  // Import implicit converter from js.Promise to Future
  import scala.scalajs.js.Thenable.Implicits._

  def main(args: Array[String]): Unit = {
    val targetDiv = dom.document.getElementById("items-container")
    Option(targetDiv) match {
      case Some(container) =>
        fetchAndDisplayItems(container)
      case None =>
        dom.console.error("Could not find target element with ID 'items-container'")
    }
  }

  private def fetchAndDisplayItems(container: dom.Element): Unit = {
    val apiUrl = "/api/items"

    container.innerHTML = "<p>Fetching items...</p>" // Update status

    // Use dom.fetch - it returns a js.Promise[dom.Response]
    // The implicit converter turns it into Future[dom.Response]
    val responseFuture: Future[dom.Response] = dom.fetch(apiUrl)

    // Process the Future chain
    val itemsFuture: Future[List[DataModel.Item]] = responseFuture.flatMap { response =>
      if (response.ok) {
        // If response status is 2xx, attempt to get the body as text.
        // response.text() returns js.Promise[String], which converts to Future[String]
        val textFuture: Future[String] = response.text()
        textFuture.flatMap { responseText =>
          // Try to decode the JSON text using Circe
          decode[List[DataModel.Item]](responseText) match {
            case Right(items) => Future.successful(items) // Parsing succeeded
            case Left(error)  => Future.failed(error) // Parsing failed
          }
        }
      } else {
        // If response status is not 2xx (e.g., 404, 500), fail the Future
        Future.failed(new Exception(s"HTTP Error: ${response.status} ${response.statusText}"))
      }
    }

    // Handle the final result (success or any failure in the chain)
    itemsFuture.onComplete {
      case Success(items) =>
        renderItems(container, items)
      case Failure(exception) =>
        // Handles: network errors, non-OK HTTP status, text decoding errors, JSON parsing errors
        dom.console.error(s"Failed to fetch or process items: ${exception.getMessage}", exception) // Log full exception
        renderError(container, s"Could not load items: ${exception.getMessage}")
    }
  }

  // renderItems and renderError methods remain the same as before
  private def renderItems(container: dom.Element, items: List[DataModel.Item]): Unit = {
    container.innerHTML = ""
    if (items.isEmpty) {
      container.appendChild(dom.document.createElement("p")).textContent = "No items found."
    } else {
      val ul = dom.document.createElement("ul")
      items.foreach { item =>
        val li = dom.document.createElement("li")
        li.textContent = s"${item.name} (ID: ${item.id}) - ${item.description}"
        ul.appendChild(li)
      }
      container.appendChild(ul)
    }
  }

  private def renderError(container: dom.Element, message: String): Unit = {
    container.innerHTML = ""
    val p = dom.document.createElement("p")
    //p.className = "error"
    p.textContent = message
    container.appendChild(p)
  }
}