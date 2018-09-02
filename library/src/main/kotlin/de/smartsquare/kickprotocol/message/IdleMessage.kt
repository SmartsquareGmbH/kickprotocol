package de.smartsquare.kickprotocol.message

import com.squareup.moshi.JsonClass

/**
 * Message for reporting the current status of the server as idle.
 *
 * @author Ruben Gees
 */
@JsonClass(generateAdapter = true)
class IdleMessage : KickprotocolMessage() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
