package de.smartsquare.kickprotocol.message

import com.squareup.moshi.JsonClass

/**
 * Message for joining an existing lobby during matchmaking.
 * The user identifies itself through the [userId] and [username]. The [position] indicates,
 * on which side the user wants to join.
 *
 * @author Ruben Gees
 */
@JsonClass(generateAdapter = true)
data class JoinLobbyMessage(
    val userId: String,
    val username: String,
    val position: TeamPosition
) : KickprotocolMessage() {

    enum class TeamPosition {
        LEFT, RIGHT
    }
}
