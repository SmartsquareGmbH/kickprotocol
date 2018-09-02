package de.smartsquare.kickprotocol

/**
 * Base type of the possible events related to discovery of other devices.
 * The [endpointId] is the id of the other endpoint in question.
 *
 * @author Ruben Gees
 */
sealed class DiscoveryEvent(open val endpointId: String) {

    /**
     * Event signaling that a device is found. A connection attempt can be made, after this event is emitted.
     */
    data class Found(override val endpointId: String) : DiscoveryEvent(endpointId)

    /**
     * Event signaling that a device is lost. This typically means that the device is not in range or has closed
     * the application.
     */
    data class Lost(override val endpointId: String) : DiscoveryEvent(endpointId)
}
