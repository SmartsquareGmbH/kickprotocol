package de.smartsquare.kickprotocol

/**
 * Base type of all exceptions related to the kickprotocol.
 */
open class KickprotocolException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Wrapper exception for all errors related to receiving an invalid message.
 */
class KickprotocolInvalidMessageException(
    message: String? = null,
    cause: Throwable? = null
) : KickprotocolException(message, cause)

/**
 * Wrapper exception for all errors related to advertising.
 */
class KickprotocolAdvertisementException(
    message: String? = null,
    cause: Throwable? = null
) : KickprotocolException(message, cause)

/**
 * Wrapper exception for all errors related to discovery.
 */
class KickprotocolDiscoveryException(
    message: String? = null,
    cause: Throwable? = null
) : KickprotocolException(message, cause)

/**
 * Wrapper exception for all errors related to connections.
 */
class KickprotocolConnectionException(
    val endpointId: String,
    message: String? = null,
    cause: Throwable? = null
) : KickprotocolException(message, cause)

/**
 * Wrapper exception for all errors related to sending messages.
 */
class KickprotocolSendException(
    val endpointId: String,
    message: String? = null,
    cause: Throwable? = null
) : KickprotocolException(message, cause)
