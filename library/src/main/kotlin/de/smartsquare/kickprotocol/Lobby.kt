package de.smartsquare.kickprotocol

/**
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
