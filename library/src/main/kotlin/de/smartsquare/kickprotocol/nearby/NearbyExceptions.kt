package de.smartsquare.kickprotocol.nearby

open class NearbyException(message: String? = null, cause: Throwable? = null) :
    RuntimeException(message, cause)

class NearbyInvalidMessageException(message: String? = null, cause: Throwable? = null) :
    NearbyException(message, cause)

class NearbyAdvertisementException(message: String? = null, cause: Throwable? = null) :
    NearbyException(message, cause)

class NearbyDiscoveryException(message: String? = null, cause: Throwable? = null) :
    NearbyException(message, cause)

class NearbyConnectionException(
    val endpointId: String, message: String? = null, cause: Throwable? = null
) : NearbyException(message, cause)

class NearbySendException(
    val endpointId: String, message: String? = null, cause: Throwable? = null
) : NearbyException(message, cause)

