package org.amizanjaleel

import play.api.libs.json.{Json, OFormat}

object DataModel {
  case class Item(id: Int, name: String, description: String)
  object Item {
    implicit lazy val jsonFormat: OFormat[Item] = Json.format[Item]
  }
}
