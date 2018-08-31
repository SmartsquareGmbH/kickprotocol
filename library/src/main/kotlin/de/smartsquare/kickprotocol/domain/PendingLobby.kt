package de.smartsquare.kickprotocol.domain

/**
 * @author Ruben Gees
 */
data class PendingLobby(
    val owner: String,
    val name: String,
    val leftTeam: List<String>,
    val rightTeam: List<String>
)
