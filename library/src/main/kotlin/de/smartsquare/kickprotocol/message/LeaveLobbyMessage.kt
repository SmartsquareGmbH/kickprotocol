package de.smartsquare.kickprotocol.message

import com.squareup.moshi.JsonClass

/**
 * Message for leaving the current matchmaking lobby or for leaving an ongoing game.
 *
 * @author Ruben Gees
 */
@JsonClass(generateAdapter = true)
class LeaveLobbyMessage : KickprotocolMessage()
