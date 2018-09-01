package de.smartsquare.kickprotocol

import android.app.Activity
import android.content.Context
import androidx.annotation.CheckResult
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.squareup.moshi.Moshi
import de.smartsquare.kickprotocol.message.CreateGameMessage
import de.smartsquare.kickprotocol.message.IdleMessage
import de.smartsquare.kickprotocol.message.JoinLobbyMessage
import de.smartsquare.kickprotocol.message.LeaveLobbyMessage
import de.smartsquare.kickprotocol.message.MatchmakingMessage
import de.smartsquare.kickprotocol.message.NearbyMessage
import de.smartsquare.kickprotocol.message.PlayingMessage
import de.smartsquare.kickprotocol.message.StartGameMessage
import de.smartsquare.kickprotocol.message.toNearbyMessage
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

/**
 * @author Ruben Gees
 */
class Kickprotocol(
    private val nativeClient: ConnectionsClient,
    private val moshi: Moshi = Moshi.Builder().build(),
    private val serviceId: String = DEFAULT_SERVICE_ID
) {

    companion object {
        const val DEFAULT_SERVICE_ID = "de.smartsquare.kickprotocol"
    }

    val foundEndpoints get() = internalFoundEndpoints
    val connectedEndpoints get() = internalConnectedEndpoints

    val discoveryEvents: Observable<DiscoveryEvent> get() = internalDiscoverySubject.hide()
    val connectionEvents: Observable<ConnectionEvent> get() = internalConnectionSubject.hide()
    val messageEvents: Observable<KickprotocolMessageWithEndpoint<*>> get() = internalMessageSubject.hide()

    val idleMessageEvents get() = messageEvents.filterInstanceOf<IdleMessage>()
    val matchmakingMessageEvents get() = messageEvents.filterInstanceOf<MatchmakingMessage>()
    val playingMessageEvents get() = messageEvents.filterInstanceOf<PlayingMessage>()
    val createGameMessageEvents get() = messageEvents.filterInstanceOf<CreateGameMessage>()
    val startGameMessageEvents get() = messageEvents.filterInstanceOf<StartGameMessage>()
    val joinLobbyMessageEvents get() = messageEvents.filterInstanceOf<JoinLobbyMessage>()
    val leaveLobbyMessageEvents get() = messageEvents.filterInstanceOf<LeaveLobbyMessage>()

    private val internalFoundEndpoints = mutableListOf<String>()
    private val internalConnectedEndpoints = mutableListOf<String>()

    private val internalDiscoverySubject = PublishSubject.create<DiscoveryEvent>()
    private val internalConnectionSubject = PublishSubject.create<ConnectionEvent>()
    private val internalMessageSubject = PublishSubject.create<KickprotocolMessageWithEndpoint<*>>()

    constructor(
        context: Context,
        moshi: Moshi = Moshi.Builder().build(),
        serviceId: String = DEFAULT_SERVICE_ID
    ) : this(
        Nearby.getConnectionsClient(context), moshi, serviceId
    )

    constructor(
        context: Activity,
        moshi: Moshi = Moshi.Builder().build(),
        serviceId: String = DEFAULT_SERVICE_ID
    ) : this(
        Nearby.getConnectionsClient(context), moshi, serviceId
    )

    @CheckResult
    fun advertise(
        nickname: String,
        advertisingOptions: AdvertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()
    ): Completable {
        return Completable.fromAction {
            nativeClient.startAdvertising(nickname, serviceId, DefaultConnectionLifecycleCallback(), advertisingOptions)
                .addOnFailureListener {
                    throw KickprotocolAdvertisementException(
                        "Starting advertisement failed",
                        it
                    )
                }
        }
    }

    @CheckResult
    fun discover(
        discoveryOptions: DiscoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()
    ): Completable {
        return Completable.fromAction {
            nativeClient.startDiscovery(serviceId, DefaultDiscoveryEndpointCallback(), discoveryOptions)
                .addOnFailureListener {
                    throw KickprotocolDiscoveryException(
                        "Starting discovery failed",
                        it
                    )
                }
        }
    }

    @CheckResult
    fun connect(nickname: String, endpointId: String): Completable {
        return Completable.fromAction {
            nativeClient.requestConnection(nickname, endpointId, DefaultConnectionLifecycleCallback())
                .addOnFailureListener {
                    throw KickprotocolConnectionException(
                        endpointId,
                        "Could not connect to endpoint $endpointId",
                        it
                    )
                }
        }
    }

    @CheckResult
    fun send(endpointId: String, message: NearbyMessage): Completable {
        return Completable.fromAction {
            nativeClient.sendPayload(endpointId, message.toPayload(moshi))
                .addOnFailureListener {
                    KickprotocolSendException(
                        endpointId,
                        "Could not send message",
                        it
                    )
                }
        }
    }

    @CheckResult
    fun broadcast(message: NearbyMessage): Completable {
        return Completable.fromAction {
            connectedEndpoints.forEach { endpointId ->
                nativeClient.sendPayload(endpointId, message.toPayload(moshi))
                    .addOnFailureListener {
                        KickprotocolSendException(
                            endpointId,
                            "Could not send message as part of broadcast",
                            it
                        )
                    }
            }
        }
    }

    fun destroy() {
        nativeClient.stopDiscovery()
        nativeClient.stopAdvertising()
        nativeClient.stopAllEndpoints()

        internalFoundEndpoints.clear()
        internalConnectedEndpoints.clear()
    }

    private inner class DefaultConnectionLifecycleCallback : ConnectionLifecycleCallback() {
        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> throw KickprotocolConnectionException(
                    endpointId, "Connection to endpoint $endpointId was rejected"
                )
                ConnectionsStatusCodes.STATUS_ERROR -> throw KickprotocolConnectionException(
                    endpointId, "Could not connect to endpoint $endpointId"
                )
                ConnectionsStatusCodes.STATUS_OK -> {
                    internalConnectedEndpoints += endpointId

                    internalConnectionSubject.onNext(ConnectionEvent.Connected(endpointId))
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            internalConnectedEndpoints -= endpointId

            internalConnectionSubject.onNext(ConnectionEvent.Disconnected(endpointId))
        }

        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            nativeClient.acceptConnection(endpointId, object : PayloadCallback() {
                override fun onPayloadReceived(endpointId: String, payload: Payload) {
                    try {
                        val message = KickprotocolMessageWithEndpoint(
                            endpointId,
                            payload.toNearbyMessage(moshi)
                        )

                        internalMessageSubject.onNext(message)
                    } catch (exception: KickprotocolException) {
                        internalMessageSubject.onError(exception)
                    }
                }

                override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
            })
        }
    }

    private inner class DefaultDiscoveryEndpointCallback : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, discoveredEndpointInfo: DiscoveredEndpointInfo) {
            internalFoundEndpoints += endpointId

            internalDiscoverySubject.onNext(DiscoveryEvent.Found(endpointId))
        }

        override fun onEndpointLost(endpointId: String) {
            internalConnectedEndpoints -= endpointId
            internalFoundEndpoints -= endpointId

            internalDiscoverySubject.onNext(DiscoveryEvent.Lost(endpointId))
        }
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : NearbyMessage> Observable<KickprotocolMessageWithEndpoint<*>>.filterInstanceOf() =
        this.filter { it.message is T }
            .map { it as KickprotocolMessageWithEndpoint<T> }
}
