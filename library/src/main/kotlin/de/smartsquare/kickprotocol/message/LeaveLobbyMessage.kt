package de.smartsquare.kickprotocol.message

import com.squareup.moshi.JsonClass

/**
 * Message for leaving the current matchmaking lobby or for leaving an ongoing game.
 *
 * @author Ruben Gees
 */
@JsonClass(generateAdapter = true)
class LeaveLobbyMessage : KickprotocolMessage() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
