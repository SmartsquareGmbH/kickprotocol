package de.smartsquare.kickprotocol

/**
 * Base type of the possible events related to connecting to other devices.
 * The [endpointId] is the id of the other endpoint in question.
 *
 * @author Ruben Gees
 */
sealed class ConnectionEvent(val endpointId: String) {

    /**
     * Event signaling a successful connection. Messages can be sent and received after this event is emitted.
     */
    class Connected(endpointId: String) : ConnectionEvent(endpointId)

    /**
     * Event signaling a disconnection. No messages can be sent or received after this event is emitted.
     */
    class Disconnected(endpointId: String) : ConnectionEvent(endpointId)
}
