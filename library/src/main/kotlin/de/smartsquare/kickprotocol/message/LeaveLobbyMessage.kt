package de.smartsquare.kickprotocol.message

import com.squareup.moshi.JsonClass

/**
 * @author Ruben Gees
 */
@JsonClass(generateAdapter = true)
class LeaveLobbyMessage : NearbyMessage()
