package de.smartsquare.kickprotocol.message

import com.squareup.moshi.JsonClass

/**
 * Message for starting the game while matchmaking.
 *
 * @author Ruben Gees
 */
@JsonClass(generateAdapter = true)
class StartGameMessage : KickprotocolMessage() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
