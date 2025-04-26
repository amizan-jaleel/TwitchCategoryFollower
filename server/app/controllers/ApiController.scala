// server/app/controllers/ApiController.scala
package controllers

import org.amizanjaleel.DataModel.Item

import javax.inject._
import play.api.mvc._

// Import Circe <-> Play integration helper and Circe syntax
import play.api.libs.circe.Circe // Provided by the play-circe library
import io.circe.syntax._        // Provides the .asJson extension method
import io.circe.Json            // For creating custom JSON objects like error messages

@Singleton
class ApiController @Inject()(val controllerComponents: ControllerComponents)
  extends BaseController
    with Circe { // <<< Mix in the Circe trait

  // --- NO MORE Play-JSON implicits needed here ---
  // val itemWrites = ... (REMOVE this)

  private val sampleItems = List(
    Item(1, "Widget", "A useful widget."),
    Item(2, "Gadget", "A fancy gadget."),
    Item(4, "Thingamajig", "You know, that thing.")
  )

  // Action to get all items
  def getItems: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    // Convert List[Item] to circe.Json using the implicit encoder from Item companion
    // The Circe trait handles sending circe.Json as an HTTP response
    Ok(sampleItems.asJson)
  }

  // Action to get a single item by ID
  def getItem(id: Int): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    sampleItems.find(_.id == id) match {
      case Some(item) =>
        // Convert found Item to circe.Json and return Ok
        Ok(item.asJson)
      case None =>
        // Create a circe.Json error object and return NotFound
        NotFound(Json.obj("error" -> s"Item with id $id not found".asJson))
    }
  }
}