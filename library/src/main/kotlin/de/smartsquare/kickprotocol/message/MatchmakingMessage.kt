package de.smartsquare.kickprotocol.message

import com.squareup.moshi.JsonClass
import de.smartsquare.kickprotocol.Lobby

/**
 * Message for reporting the current status of the server as matchmaking.
 *
 * @author Ruben Gees
 */
@JsonClass(generateAdapter = true)
data class MatchmakingMessage(val lobby: Lobby) : KickprotocolMessage()
