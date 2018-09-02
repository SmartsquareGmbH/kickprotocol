package de.smartsquare.kickprotocol

import de.smartsquare.kickprotocol.message.KickprotocolMessage

/**
 * Wrapper class for the combination of the [endpointId] a message was sent from and the actual [message].
 *
 * @author Ruben Gees
 */
data class KickprotocolMessageWithEndpoint<T : KickprotocolMessage>(val endpointId: String, val message: T)
