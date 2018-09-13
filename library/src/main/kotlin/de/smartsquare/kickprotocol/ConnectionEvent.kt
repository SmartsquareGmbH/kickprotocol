package de.smartsquare.kickprotocol

/**
 * Base type of the possible events related to connecting to other devices.
 * The [endpointId] is the id of the other endpoint in question.
 *
 * @author Ruben Gees
 */
sealed class ConnectionEvent(open val endpointId: String) {

    /**
     * Event signaling a successful connection. Messages can be sent and received after this event is emitted.
     */
    data class Connected(override val endpointId: String) : ConnectionEvent(endpointId)

    /**
     * Event signaling a disconnection. No messages can be sent or received after this event is emitted.
     */
    data class Disconnected(override val endpointId: String) : ConnectionEvent(endpointId)

    /**
     * Event signaling an error related to connections. The [error] property contains the cause.
     */
    data class Error(
        override val endpointId: String,
        val error: KickprotocolConnectionException
    ) : ConnectionEvent(endpointId)
}
