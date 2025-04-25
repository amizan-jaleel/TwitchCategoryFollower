package controllers

import org.amizanjaleel.DataModel.Item

import javax.inject._
import play.api.mvc._
import play.api.libs.json._ // Play's JSON library


@Singleton // Tells Play's dependency injection to create only one instance
class ApiController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  // Define how to convert our Item case class to JSON using Play JSON
  // This implicit needs to be in scope where Json.toJson is called.
  implicit val itemWrites: Writes[Item] = Json.writes[Item]
  // Alternatively:
  // implicit val itemWrites: Writes[Item] = (
  //   (JsPath \ "id").write[Int] and
  //   (JsPath \ "name").write[String] and
  //   (JsPath \ "description").write[String]
  // )(unlift(Item.unapply))

  // Sample in-memory data
  private val sampleItems = List(
    Item(1, "Widget", "A useful widget."),
    Item(2, "Gadget", "A fancy gadget."),
    Item(3, "Thingamajig", "You know, that thing.")
  )

  /**
   * An Action that returns a list of items as JSON.
   * Mapped via the routes file.
   */
  def getItems: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    // Convert the list of items to JSON
    val jsonResponse = Json.toJson(sampleItems)

    // Return an HTTP 200 OK status with the JSON payload
    Ok(jsonResponse)
  }

  /**
   * Example of an endpoint to get a single item by ID (demonstrates path parameters)
   */
  def getItem(id: Int): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    sampleItems.find(_.id == id) match {
      case Some(item) => Ok(Json.toJson(item)) // Found: return 200 OK with item JSON
      case None       => NotFound(Json.obj("error" -> s"Item with id $id not found")) // Not Found: return 404 Not Found
    }
  }
}