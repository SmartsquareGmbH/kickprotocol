package de.smartsquare.kickprotocol.message

import com.squareup.moshi.JsonClass

/**
 * @author Ruben Gees
 */
@JsonClass(generateAdapter = true)
data class JoinLobbyMessage(val userId: String, val username: String, val position: TeamPosition) : NearbyMessage() {

    enum class TeamPosition {
        LEFT, RIGHT
    }
}
