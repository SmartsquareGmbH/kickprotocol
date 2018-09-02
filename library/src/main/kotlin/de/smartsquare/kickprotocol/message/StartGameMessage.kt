package de.smartsquare.kickprotocol.message

import com.squareup.moshi.JsonClass

/**
 * Message for starting the game while matchmaking.
 *
 * @author Ruben Gees
 */
@JsonClass(generateAdapter = true)
class StartGameMessage : KickprotocolMessage()
