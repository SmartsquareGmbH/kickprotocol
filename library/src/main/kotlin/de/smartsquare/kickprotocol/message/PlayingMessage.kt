package de.smartsquare.kickprotocol.message

import com.squareup.moshi.JsonClass
import de.smartsquare.kickprotocol.Lobby

/**
 * @author Ruben Gees
 */
@JsonClass(generateAdapter = true)
data class PlayingMessage(val lobby: Lobby) : NearbyMessage()
