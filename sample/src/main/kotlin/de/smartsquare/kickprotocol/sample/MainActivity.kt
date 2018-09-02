package de.smartsquare.kickprotocol.sample

import android.Manifest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import de.smartsquare.kickprotocol.Kickprotocol
import de.smartsquare.kickprotocol.Lobby
import de.smartsquare.kickprotocol.message.IdleMessage
import de.smartsquare.kickprotocol.message.JoinLobbyMessage.TeamPosition
import de.smartsquare.kickprotocol.message.MatchmakingMessage
import de.smartsquare.kickprotocol.message.PlayingMessage

/**
 * @author Ruben Gees
 */
class MainActivity : AppCompatActivity() {

    private val kickprotocol by lazy { Kickprotocol(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        kickprotocol.connectionEvents
            .autoDisposable(this.scope())
            .subscribe { kickprotocol.send(it.endpointId, IdleMessage()) }

        kickprotocol.createGameMessageEvents
            .autoDisposable(this.scope())
            .subscribe { (_, message) ->
                val lobby = Lobby(message.username, "SampleLobby", listOf(message.username), emptyList(), 0, 0)

                kickprotocol.broadcast(MatchmakingMessage(lobby))
            }

        kickprotocol.joinLobbyMessageEvents
            .autoDisposable(this.scope())
            .subscribe { (_, message) ->
                val leftTeam = listOf("Sample").let {
                    if (message.position == TeamPosition.LEFT) {
                        it.plus(message.username)
                    } else {
                        it.plus("Te\$t")
                    }
                }

                val rightTeam = listOf("Somebody").let {
                    if (message.position == TeamPosition.RIGHT) {
                        it.plus(message.username)
                    } else {
                        it.plus("Te\$t")
                    }
                }

                val lobby = Lobby("Sample", "SampleLobby", leftTeam, rightTeam, 0, 0)

                kickprotocol.broadcast(MatchmakingMessage(lobby))
            }

        kickprotocol.leaveLobbyMessageEvents
            .autoDisposable(this.scope())
            .subscribe {
                kickprotocol.broadcast(IdleMessage())
            }

        kickprotocol.createGameMessageEvents
            .autoDisposable(this.scope())
            .subscribe {
                val leftTeam = listOf("Sample", "Somebody")
                val rightTeam = listOf("Else", "te\$t")
                val lobby = Lobby("Sample", "SampleLobby", leftTeam, rightTeam, 5, 7)

                kickprotocol.broadcast(PlayingMessage(lobby))
            }
    }

    override fun onStart() {
        super.onStart()

        RxPermissions(this)
            .requestEach(Manifest.permission.ACCESS_COARSE_LOCATION)
            .autoDisposable(this.scope())
            .subscribe {
                if (it.granted) {
                    kickprotocol
                        .advertise("sample")
                        .subscribe()
                } else {
                    finish()
                }
            }
    }

    override fun onStop() {
        kickprotocol.stop()

        super.onStop()
    }
}
