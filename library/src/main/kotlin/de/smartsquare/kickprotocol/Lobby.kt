package de.smartsquare.kickprotocol

/**
 * Domain class representing a game lobby.
 *
 * @param owner The username of the owner.
 * @param name The name of the lobby
 * @param leftTeam List of players participating in the left team of this lobby.
 * @param rightTeam List of players participating in the right team of this lobby.
 * @param scoreLeftTeam The current score of the left team.
 * @param scoreRightTeam The current score of the right team.
 *
 * @author Ruben Gees
 */
data class Lobby(
    val owner: String,
    val name: String,
    val leftTeam: List<String>,
    val rightTeam: List<String>,
    val scoreLeftTeam: Int,
    val scoreRightTeam: Int
)
