package de.smartsquare.kickprotocol.message

import com.squareup.moshi.JsonClass

/**
 * Message for creating a game.
 * Contains the [userId] and [username] of the user, aiming to create a new lobby for matchmaking.
 *
 * @author Ruben Gees
 */
@JsonClass(generateAdapter = true)
data class CreateGameMessage(val userId: String, val username: String) : KickprotocolMessage()
