package de.smartsquare.kickprotocol.message

import com.squareup.moshi.JsonClass

/**
 * Message for reporting the current status of the server as idle.
 *
 * @author Ruben Gees
 */
@JsonClass(generateAdapter = true)
class IdleMessage : KickprotocolMessage()
