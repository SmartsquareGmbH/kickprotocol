package de.smartsquare.kickprotocol

open class KickprotocolException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)

class KickprotocolInvalidMessageException(
    message: String? = null, cause: Throwable? = null
) : KickprotocolException(message, cause)

class KickprotocolAdvertisementException(
    message: String? = null, cause: Throwable? = null
) : KickprotocolException(message, cause)

class KickprotocolDiscoveryException(
    message: String? = null, cause: Throwable? = null
) : KickprotocolException(message, cause)

class KickprotocolConnectionException(
    val endpointId: String, message: String? = null, cause: Throwable? = null
) : KickprotocolException(message, cause)

class KickprotocolSendException(
    val endpointId: String, message: String? = null, cause: Throwable? = null
) : KickprotocolException(message, cause)
