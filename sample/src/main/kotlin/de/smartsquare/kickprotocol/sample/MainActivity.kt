package de.smartsquare.kickprotocol.sample

import android.Manifest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import de.smartsquare.kickprotocol.ConnectionEvent
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
            .filter { it is ConnectionEvent.Connected }
            .autoDisposable(this.scope())
            .subscribe {
                kickprotocol.sendAndAwait(it.endpointId, IdleMessage())
                    .autoDisposable(this.scope())
                    .subscribe()
            }

        kickprotocol.createGameMessageEvents
            .autoDisposable(this.scope())
            .subscribe { (_, message) ->
                val lobby = Lobby(message.username, "SampleLobby", listOf(message.username), emptyList(), 0, 0)

                kickprotocol.broadcastAndAwait(MatchmakingMessage(lobby))
                    .autoDisposable(this.scope())
                    .subscribe()
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

                kickprotocol.broadcastAndAwait(MatchmakingMessage(lobby))
                    .autoDisposable(this.scope())
                    .subscribe()
            }

        kickprotocol.leaveLobbyMessageEvents
            .autoDisposable(this.scope())
            .subscribe {
                kickprotocol.broadcastAndAwait(IdleMessage())
                    .autoDisposable(this.scope())
                    .subscribe()
            }

        kickprotocol.createGameMessageEvents
            .autoDisposable(this.scope())
            .subscribe {
                val leftTeam = listOf("Sample", "Somebody")
                val rightTeam = listOf("Else", "te\$t")
                val lobby = Lobby("Sample", "SampleLobby", leftTeam, rightTeam, 5, 7)

                kickprotocol.broadcastAndAwait(PlayingMessage(lobby))
                    .autoDisposable(this.scope())
                    .subscribe()
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
                        .autoDisposable(this.scope())
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
