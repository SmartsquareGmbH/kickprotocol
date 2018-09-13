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
import de.smartsquare.kickprotocol.message.KickprotocolMessage
import de.smartsquare.kickprotocol.message.LeaveLobbyMessage
import de.smartsquare.kickprotocol.message.MatchmakingMessage
import de.smartsquare.kickprotocol.message.PlayingMessage
import de.smartsquare.kickprotocol.message.StartGameMessage
import de.smartsquare.kickprotocol.message.toNearbyMessage
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

/**
 * Main class for interacting with the kickprotocol.
 * It contains various methods and properties for discovery of other devices, connecting to them
 * and exchanging messages.
 *
 * This class is tied to the lifecycle of a given [Activity] or [Context]. After the given lifecycle ends, the [stop]
 * method should be called, to clean up all used resources and ongoing connections.
 *
 * @param nativeClient The client to use for internal negotiation of connections.
 * @param moshi The Moshi instance to use for json serialization.
 * @param serviceId A custom service id to use.
 *
 * @author Ruben Gees
 */
class Kickprotocol(
    private val nativeClient: ConnectionsClient,
    private val moshi: Moshi = Moshi.Builder().build(),
    private val serviceId: String = DEFAULT_SERVICE_ID
) {

    companion object {

        /**
         * The default service id to use. Needs to be changed to the same on both
         * the server and client for devices to be discovered.
         */
        const val DEFAULT_SERVICE_ID = "de.smartsquare.kickprotocol"
    }

    /**
     * List containing all currently found endpoints.
     */
    val foundEndpoints get() = internalFoundEndpoints

    /**
     * List containing all currently connected endpoints.
     */
    val connectedEndpoints get() = internalConnectedEndpoints

    /**
     * Observable emitting all events related to discovery of other devices.
     */
    val discoveryEvents: Observable<DiscoveryEvent> get() = internalDiscoverySubject.hide()

    /**
     * Observable emitting all events related to connections to other devices.
     */
    val connectionEvents: Observable<ConnectionEvent> get() = internalConnectionSubject.hide()

    /**
     * Observable emitting all messages sent by other devices.
     * See the specialized Observables for simplified usage.
     */
    val messageEvents: Observable<MessageEvent<*>> get() = internalMessageSubject.hide()

    /**
     * Observable emitting all [IdleMessage]s sent by other devices.
     */
    val idleMessageEvents get() = messageEvents.filterInstanceOf<IdleMessage>()

    /**
     * Observable emitting all [MatchmakingMessage]s sent by other devices.
     */
    val matchmakingMessageEvents get() = messageEvents.filterInstanceOf<MatchmakingMessage>()

    /**
     * Observable emitting all [PlayingMessage]s sent by other devices.
     */
    val playingMessageEvents get() = messageEvents.filterInstanceOf<PlayingMessage>()

    /**
     * Observable emitting all [CreateGameMessage]s sent by other devices.
     */
    val createGameMessageEvents get() = messageEvents.filterInstanceOf<CreateGameMessage>()

    /**
     * Observable emitting all [StartGameMessage]s sent by other devices.
     */
    val startGameMessageEvents get() = messageEvents.filterInstanceOf<StartGameMessage>()

    /**
     * Observable emitting all [JoinLobbyMessage]s sent by other devices.
     */
    val joinLobbyMessageEvents get() = messageEvents.filterInstanceOf<JoinLobbyMessage>()

    /**
     * Observable emitting all [LeaveLobbyMessage]s sent by other devices.
     */
    val leaveLobbyMessageEvents get() = messageEvents.filterInstanceOf<LeaveLobbyMessage>()

    private val internalFoundEndpoints = mutableListOf<String>()
    private val internalConnectedEndpoints = mutableListOf<String>()

    private val internalDiscoverySubject = PublishSubject.create<DiscoveryEvent>()
    private val internalConnectionSubject = PublishSubject.create<ConnectionEvent>()
    private val internalMessageSubject = PublishSubject.create<MessageEvent<*>>()

    private val internalMessageSentSuccessSubject = PublishSubject.create<Pair<String, Long>>()
    private val internalMessageSentFailureSubject = PublishSubject.create<Pair<String, Long>>()

    /**
     * Alternate constructor allowing to simply pass a [Context] instead of a [ConnectionsClient].
     * The client is then bound to the given context.
     *
     * @param context The context to use for constructing the internal client for negotiation of connections.
     * @param moshi The Moshi instance to use for json serialization.
     * @param serviceId A custom service id to use.
     */
    constructor(
        context: Context,
        moshi: Moshi = Moshi.Builder().build(),
        serviceId: String = DEFAULT_SERVICE_ID
    ) : this(
        Nearby.getConnectionsClient(context), moshi, serviceId
    )

    /**
     * Alternate constructor allowing to simply pass an [Activity] instead of a [ConnectionsClient].
     * The client is then bound to the given activity.
     *
     * @param context The activity to use for constructing the internal client for negotiation of connections.
     * @param moshi The Moshi instance to use for json serialization.
     * @param serviceId A custom service id to use.
     */
    constructor(
        context: Activity,
        moshi: Moshi = Moshi.Builder().build(),
        serviceId: String = DEFAULT_SERVICE_ID
    ) : this(
        Nearby.getConnectionsClient(context), moshi, serviceId
    )

    /**
     * Method for advertising this device with the given [nickname].
     * Other devices can then [discover] this device when nearby and [connect] to it.
     *
     * Note that this method returns an [Completable],
     * which only does actual work after calling [Completable.subscribe].
     * Error-handling is done by implementing the onError callback in [Completable.subscribe].
     * The actual exceptions are wrapped in a [KickprotocolAdvertisementException] for this method.
     */
    @CheckResult
    fun advertise(
        nickname: String,
        advertisingOptions: AdvertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()
    ): Completable {
        return Completable.create { emitter ->
            nativeClient.startAdvertising(nickname, serviceId, DefaultConnectionLifecycleCallback(), advertisingOptions)
                .addOnSuccessListener { emitter.onComplete() }
                .addOnFailureListener {
                    emitter.onError(KickprotocolAdvertisementException("Starting advertisement failed", it))
                }
        }
    }

    /**
     * Method for discovering other nearby devices, which have called [advertise] before.
     * After a device is found, a connection can be initiated through the [connect] method.
     *
     * Note that this method returns an [Completable],
     * which only does actual work after calling [Completable.subscribe].
     * Error-handling is done by implementing the onError callback in [Completable.subscribe].
     * The actual exceptions are wrapped in a [KickprotocolDiscoveryException] for this method.
     */
    @CheckResult
    fun discover(
        discoveryOptions: DiscoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()
    ): Completable {
        return Completable.create { emitter ->
            nativeClient.startDiscovery(serviceId, DefaultDiscoveryEndpointCallback(), discoveryOptions)
                .addOnSuccessListener { emitter.onComplete() }
                .addOnFailureListener {
                    emitter.onError(KickprotocolDiscoveryException("Starting discovery failed", it))
                }
        }
    }

    /**
     * Method to connect to another nearby device with the given [nickname].
     * The other device is identified through the [endpointId]
     * This method should only be called, after another device was discovered through the [discover] method.
     *
     * Note that this method returns an [Completable],
     * which only does actual work after calling [Completable.subscribe].
     * Error-handling is done by implementing the onError callback in [Completable.subscribe].
     * The actual exceptions are wrapped in a [KickprotocolConnectionException] for this method.
     */
    @CheckResult
    fun connect(nickname: String, endpointId: String): Completable {
        return Completable.create { emitter ->
            nativeClient.requestConnection(nickname, endpointId, DefaultConnectionLifecycleCallback())
                .addOnSuccessListener { emitter.onComplete() }
                .addOnFailureListener {
                    emitter.onError(
                        KickprotocolConnectionException(endpointId, "Could not connect to endpoint $endpointId", it)
                    )
                }
        }
    }

    /**
     * Method for sending a [message] to another connected device, identified by the given [endpointId].
     *
     * Note that this method returns an [Completable],
     * which only does actual work after calling [Completable.subscribe].
     * Error-handling is done by implementing the onError callback in [Completable.subscribe].
     * The actual exceptions are wrapped in a [KickprotocolSendException] for this method.
     *
     * @see sendAndAwait
     * @see broadcast
     * @see broadcastAndAwait
     */
    @CheckResult
    fun send(endpointId: String, message: KickprotocolMessage): Completable {
        return send(endpointId, message.toPayload(moshi))
    }

    /**
     * Method for sending a [message] to another connected device, identified by the given [endpointId] and awaiting
     * the message to be completely sent.
     *
     * Note that this method returns an [Completable],
     * which only does actual work after calling [Completable.subscribe].
     * Error-handling is done by implementing the onError callback in [Completable.subscribe].
     * The actual exceptions are wrapped in a [KickprotocolSendException] for this method.
     *
     * @see send
     * @see broadcast
     * @see broadcastAndAwait
     */
    @CheckResult
    fun sendAndAwait(endpointId: String, message: KickprotocolMessage): Completable {
        val payload = message.toPayload(moshi)

        val successCompletable = internalMessageSentSuccessSubject
            .filter { (foundEndpointId, payloadId) -> foundEndpointId == endpointId && payloadId == payload.id }
            .firstOrError()
            .ignoreElement()

        val errorCompletable = internalMessageSentFailureSubject
            .filter { (foundEndpointId, payloadId) -> foundEndpointId == endpointId && payloadId == payload.id }
            .flatMapCompletable { Completable.error(KickprotocolSendException("Could not send message")) }

        return send(endpointId, payload)
            .andThen(Completable.amb(listOf(successCompletable, errorCompletable)))
    }

    /**
     * Method for sending a [message] to all connected devices.
     *
     * Note that this method returns an [Completable],
     * which only does actual work after calling [Completable.subscribe].
     * Error-handling is done by implementing the onError callback in [Completable.subscribe].
     * The actual exceptions are wrapped in a [KickprotocolSendException] for this method.
     *
     * @see broadcastAndAwait
     * @see send
     * @see sendAndAwait
     */
    @CheckResult
    fun broadcast(message: KickprotocolMessage): Completable {
        return Completable.merge(internalConnectedEndpoints.map { send(it, message) })
    }

    /**
     * Method for sending a [message] to all connected devices.
     *
     * Note that this method returns an [Completable],
     * which only does actual work after calling [Completable.subscribe].
     * Error-handling is done by implementing the onError callback in [Completable.subscribe].
     * The actual exceptions are wrapped in a [KickprotocolSendException] for this method.
     *
     * @see broadcast
     * @see send
     * @see sendAndAwait
     */
    @CheckResult
    fun broadcastAndAwait(message: KickprotocolMessage): Completable {
        return Completable.merge(internalConnectedEndpoints.map { sendAndAwait(it, message) })
    }

    @CheckResult
    private fun send(endpointId: String, payload: Payload): Completable {
        return Completable.create { emitter ->
            nativeClient.sendPayload(endpointId, payload)
                .addOnSuccessListener { emitter.onComplete() }
                .addOnFailureListener {
                    emitter.onError(KickprotocolSendException(endpointId, "Could not send message", it))
                }
        }
    }

    /**
     * Method for stopping all kickprotocol activities.
     * This means stopping discovery, advertising and disconnecting from all endpoints.
     *
     * After this method has been called, no further events will be emitted.
     */
    fun stop() {
        nativeClient.stopDiscovery()
        nativeClient.stopAdvertising()
        nativeClient.stopAllEndpoints()

        internalFoundEndpoints.clear()
        internalConnectedEndpoints.clear()
    }

    private inner class DefaultConnectionLifecycleCallback : ConnectionLifecycleCallback() {
        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> internalConnectionSubject.onError(
                    KickprotocolConnectionException(endpointId, "Connection to endpoint $endpointId was rejected")
                )
                ConnectionsStatusCodes.STATUS_ERROR -> internalConnectionSubject.onError(
                    KickprotocolConnectionException(endpointId, "Could not connect to endpoint $endpointId")
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
                    val message = try {
                        MessageEvent.Message(endpointId, payload.toNearbyMessage(moshi))
                    } catch (exception: KickprotocolException) {
                        MessageEvent.Error(endpointId, exception)
                    }

                    internalMessageSubject.onNext(message)
                }

                override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
                    if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
                        internalMessageSentSuccessSubject.onNext(endpointId to update.payloadId)
                    } else if (update.status == PayloadTransferUpdate.Status.FAILURE) {
                        internalMessageSentFailureSubject.onNext(endpointId to update.payloadId)
                    }
                }
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
}
