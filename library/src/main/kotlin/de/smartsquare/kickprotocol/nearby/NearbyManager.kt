package de.smartsquare.kickprotocol.nearby

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
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

/**
 * @author Ruben Gees
 */
class NearbyManager(
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
    val messageEvents: Observable<Pair<String, NearbyMessage>> get() = internalMessageSubject.hide()

    private val internalFoundEndpoints = mutableListOf<String>()
    private val internalConnectedEndpoints = mutableListOf<String>()

    private val internalDiscoverySubject = PublishSubject.create<DiscoveryEvent>()
    private val internalConnectionSubject = PublishSubject.create<ConnectionEvent>()
    private val internalMessageSubject = PublishSubject.create<Pair<String, NearbyMessage>>()

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
            nativeClient.startAdvertising(
                nickname,
                serviceId,
                DefaultConnectionLifecycleCallback(),
                advertisingOptions
            )
                .addOnFailureListener {
                    throw NearbyAdvertisementException(
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
            nativeClient.startDiscovery(
                serviceId,
                DefaultDiscoveryEndpointCallback(),
                discoveryOptions
            )
                .addOnFailureListener {
                    throw NearbyDiscoveryException(
                        "Starting discovery failed",
                        it
                    )
                }
        }
    }

    @CheckResult
    fun connect(nickname: String, endpointId: String): Completable {
        return Completable.fromAction {
            nativeClient.requestConnection(
                nickname,
                endpointId,
                DefaultConnectionLifecycleCallback()
            )
                .addOnFailureListener {
                    throw NearbyConnectionException(
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
                    NearbySendException(
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
                        NearbySendException(
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
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> throw NearbyConnectionException(
                    endpointId,
                    "Connection to endpoint $endpointId was rejected"
                )
                ConnectionsStatusCodes.STATUS_ERROR -> throw NearbyConnectionException(
                    endpointId,
                    "Could not connect to endpoint $endpointId"
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
                        internalMessageSubject.onNext(endpointId to payload.toNearbyMessage(moshi))
                    } catch (exception: NearbyException) {
                        internalMessageSubject.onError(exception)
                    }
                }

                override fun onPayloadTransferUpdate(
                    endpointId: String,
                    update: PayloadTransferUpdate
                ) = Unit
            })
        }
    }

    private inner class DefaultDiscoveryEndpointCallback : EndpointDiscoveryCallback() {
        override fun onEndpointFound(
            endpointId: String,
            discoveredEndpointInfo: DiscoveredEndpointInfo
        ) {
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
