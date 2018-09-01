package de.smartsquare.kickprotocol.message

import com.squareup.moshi.JsonClass

/**
 * @author Ruben Gees
 */
@JsonClass(generateAdapter = true)
data class CreateGameMessage(val userId: String, val username: String) : NearbyMessage()
