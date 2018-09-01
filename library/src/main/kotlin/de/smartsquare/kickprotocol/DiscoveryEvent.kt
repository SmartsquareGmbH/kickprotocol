package de.smartsquare.kickprotocol

/**
 * @author Ruben Gees
 */
sealed class DiscoveryEvent(val endpointId: String) {
    class Found(endpointId: String) : DiscoveryEvent(endpointId)
    class Lost(endpointId: String) : DiscoveryEvent(endpointId)
}
