package de.smartsquare.kickprotocol.domain

/**
 * @author Ruben Gees
 */
data class PlayingLobby(
    val owner: String,
    val leftTeam: List<String>,
    val rightTeam: List<String>,
    val scoreLeftTeam: Int,
    val scoreRightTeam: Int
)
