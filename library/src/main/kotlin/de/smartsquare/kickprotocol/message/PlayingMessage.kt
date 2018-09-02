package de.smartsquare.kickprotocol.message

import com.squareup.moshi.JsonClass
import de.smartsquare.kickprotocol.Lobby

/**
 * Message for reporting the current status of the server as playing.
 * The [lobby] contains information on the participating players, the owner, the name and the current scores of the
 * game.
 *
 * @author Ruben Gees
 */
@JsonClass(generateAdapter = true)
data class PlayingMessage(val lobby: Lobby) : KickprotocolMessage()
