package org.amizanjaleel

// Import Circe core types and derivation helpers
import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto._ // Or use io.circe.generic.auto._ for fully automatic

object DataModel {
  // Your case class definition
  case class Item(id: Int, name: String, description: String)

  // Companion object where JSON format is defined
  object Item {

    // Implicitly derive an Encoder for Item -> JSON
    // This will be found automatically when .asJson is called on an Item
    implicit val itemEncoder: Encoder[Item] = deriveEncoder[Item]

    // Implicitly derive a Decoder for JSON -> Item
    // Needed if your server accepts Item JSON in request bodies, or for client-side
    implicit val itemDecoder: Decoder[Item] = deriveDecoder[Item]

  }

}