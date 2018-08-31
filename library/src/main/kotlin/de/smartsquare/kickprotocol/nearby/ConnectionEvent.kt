package de.smartsquare.kickprotocol.nearby

/**
 * @author Ruben Gees
 */
sealed class ConnectionEvent(val endpointId: String) {
    class Connected(endpointId: String) : ConnectionEvent(endpointId)
    class Disconnected(endpointId: String) : ConnectionEvent(endpointId)
}
