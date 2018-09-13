package de.smartsquare.kickprotocol

import de.smartsquare.kickprotocol.message.KickprotocolMessage

/**
 * Base type of the possible events related to messages.
 * The [endpointId] is the id of the other endpoint in question.
 *
 * @param T The type of the contained message.
 *
 * @author Ruben Gees
 */
@Suppress("unused")
sealed class MessageEvent<out T : KickprotocolMessage>(open val endpointId: String) {

    /**
     * Event containing an actual [message] from a connected device.
     */
    data class Message<T : KickprotocolMessage>(
        override val endpointId: String,
        val message: T
    ) : MessageEvent<T>(endpointId)

    /**
     * Event containing an [error] related to messaging.
     */
    data class Error(
        override val endpointId: String,
        val error: Throwable
    ) : MessageEvent<Nothing>(endpointId)
}
