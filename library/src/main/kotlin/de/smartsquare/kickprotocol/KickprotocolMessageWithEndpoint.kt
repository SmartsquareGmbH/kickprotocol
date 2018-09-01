package de.smartsquare.kickprotocol

import de.smartsquare.kickprotocol.message.NearbyMessage

/**
 * @author Ruben Gees
 */
data class KickprotocolMessageWithEndpoint<T : NearbyMessage>(val endpointId: String, val message: T)
