package de.smartsquare.kickprotocol

import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import de.smartsquare.kickprotocol.message.IdleMessage
import de.smartsquare.kickprotocol.message.StartGameMessage
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.reactivex.observers.TestObserver
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldEqual
import org.junit.jupiter.api.Test

/**
 * @author Ruben Gees
 */
class KickprotocolTest {

    private val connectionsClient = mockk<ConnectionsClient>()
    private val kickprotocol = Kickprotocol(connectionsClient)

    @Test
    fun advertising() {
        val task = mockk<Task<Void>>()
        val successListener = slot<OnSuccessListener<Void>>()
        val testObserver = TestObserver<Unit>()

        every { task.addOnSuccessListener(capture(successListener)) } returns task
        every { task.addOnFailureListener(any()) } returns task

        every {
            connectionsClient.startAdvertising(
                "test",
                Kickprotocol.DEFAULT_SERVICE_ID,
                any(),
                any()
            )
        } returns task

        kickprotocol.advertise("test").subscribe(testObserver)
        successListener.captured.onSuccess(null)

        testObserver.assertComplete()
    }

    @Test
    fun `advertising with error`() {
        val task = mockk<Task<Void>>()
        val failureListener = slot<OnFailureListener>()
        val testObserver = TestObserver<Unit>()

        every { task.addOnSuccessListener(any()) } returns task
        every { task.addOnFailureListener(capture(failureListener)) } returns task

        every {
            connectionsClient.startAdvertising(
                "test",
                Kickprotocol.DEFAULT_SERVICE_ID,
                any(),
                any()
            )
        } returns task

        kickprotocol.advertise("test").subscribe(testObserver)
        failureListener.captured.onFailure(KickprotocolAdvertisementException())

        testObserver.assertError(KickprotocolAdvertisementException::class.java)
    }

    @Test
    fun discovering() {
        val task = mockk<Task<Void>>()
        val successListener = slot<OnSuccessListener<Void>>()
        val testObserver = TestObserver<Unit>()

        every { task.addOnSuccessListener(capture(successListener)) } returns task
        every { task.addOnFailureListener(any()) } returns task
        every { connectionsClient.startDiscovery(Kickprotocol.DEFAULT_SERVICE_ID, any(), any()) } returns task

        kickprotocol.discover().subscribe(testObserver)
        successListener.captured.onSuccess(null)

        testObserver.assertComplete()
    }

    @Test
    fun `discovering with error`() {
        val task = mockk<Task<Void>>()
        val failureListener = slot<OnFailureListener>()
        val testObserver = TestObserver<Unit>()

        every { task.addOnSuccessListener(any()) } returns task
        every { task.addOnFailureListener(capture(failureListener)) } returns task
        every { connectionsClient.startDiscovery(Kickprotocol.DEFAULT_SERVICE_ID, any(), any()) } returns task

        kickprotocol.discover().subscribe(testObserver)
        failureListener.captured.onFailure(KickprotocolDiscoveryException())

        testObserver.assertError(KickprotocolDiscoveryException::class.java)
    }

    @Test
    fun connecting() {
        val task = mockk<Task<Void>>()
        val successListener = slot<OnSuccessListener<Void>>()
        val testObserver = TestObserver<Unit>()

        every { task.addOnSuccessListener(capture(successListener)) } returns task
        every { task.addOnFailureListener(any()) } returns task
        every { connectionsClient.requestConnection("test", "endpoint", any()) } returns task

        kickprotocol.connect("test", "endpoint").subscribe(testObserver)
        successListener.captured.onSuccess(null)

        testObserver.assertComplete()
    }

    @Test
    fun `connecting with error`() {
        val task = mockk<Task<Void>>()
        val failureListener = slot<OnFailureListener>()
        val testObserver = TestObserver<Unit>()

        every { task.addOnSuccessListener(any()) } returns task
        every { task.addOnFailureListener(capture(failureListener)) } returns task
        every { connectionsClient.requestConnection("test", "endpoint", any()) } returns task

        kickprotocol.connect("test", "endpoint").subscribe(testObserver)
        failureListener.captured.onFailure(KickprotocolConnectionException("endpoint"))

        testObserver.assertError(KickprotocolConnectionException::class.java)
    }

    @Test
    fun sending() {
        val task = mockk<Task<Void>>()
        val successListener = slot<OnSuccessListener<Void>>()
        val testObserver = TestObserver<Unit>()
        val payload = slot<Payload>()

        every { task.addOnSuccessListener(capture(successListener)) } returns task
        every { task.addOnFailureListener(any()) } returns task
        every { connectionsClient.sendPayload("endpoint", capture(payload)) } returns task

        kickprotocol.send("endpoint", IdleMessage()).subscribe(testObserver)
        successListener.captured.onSuccess(null)

        testObserver.assertComplete()
        payload.captured.asBytes()?.toString(Charsets.UTF_8) ?: "" shouldBeEqualTo "IdleMessage\n{}"
    }

    @Test
    fun `sending and awaiting`() {
        val connectionLifecycleCallback = slot<ConnectionLifecycleCallback>()
        val payloadCallback = slot<PayloadCallback>()
        val successListener = slot<OnSuccessListener<Void>>()
        val testObserver = TestObserver<Unit>()

        val task = mockk<Task<Void>>()
        val payload = slot<Payload>()

        every {
            connectionsClient.startAdvertising(
                any(),
                any(),
                capture(connectionLifecycleCallback),
                any()
            )
        } returns task

        every { task.addOnSuccessListener(capture(successListener)) } returns task
        every { task.addOnFailureListener(any()) } returns task
        every { connectionsClient.acceptConnection(any(), capture(payloadCallback)) } returns task
        every { connectionsClient.sendPayload("endpoint", capture(payload)) } returns task

        kickprotocol.advertise("").subscribe()
        connectionLifecycleCallback.captured.onConnectionInitiated("endpoint", mockk())

        kickprotocol.sendAndAwait("endpoint", IdleMessage()).subscribe(testObserver)
        successListener.captured.onSuccess(null)

        val transferUpdate = PayloadTransferUpdate.Builder()
            .setStatus(PayloadTransferUpdate.Status.SUCCESS)
            .setPayloadId(payload.captured.id)
            .build()

        testObserver.assertNotComplete()

        payloadCallback.captured.onPayloadTransferUpdate("endpoint", transferUpdate)

        testObserver.assertComplete()
    }

    @Test
    fun `sending and awaiting with error`() {
        val connectionLifecycleCallback = slot<ConnectionLifecycleCallback>()
        val payloadCallback = slot<PayloadCallback>()
        val successListener = slot<OnSuccessListener<Void>>()
        val testObserver = TestObserver<Unit>()

        val task = mockk<Task<Void>>()
        val payload = slot<Payload>()

        every {
            connectionsClient.startAdvertising(
                any(),
                any(),
                capture(connectionLifecycleCallback),
                any()
            )
        } returns task

        every { task.addOnSuccessListener(capture(successListener)) } returns task
        every { task.addOnFailureListener(any()) } returns task
        every { connectionsClient.acceptConnection(any(), capture(payloadCallback)) } returns task
        every { connectionsClient.sendPayload("endpoint", capture(payload)) } returns task

        kickprotocol.advertise("").subscribe()
        connectionLifecycleCallback.captured.onConnectionInitiated("endpoint", mockk())

        kickprotocol.sendAndAwait("endpoint", IdleMessage()).subscribe(testObserver)
        successListener.captured.onSuccess(null)

        val transferUpdate = PayloadTransferUpdate.Builder()
            .setStatus(PayloadTransferUpdate.Status.FAILURE)
            .setPayloadId(payload.captured.id)
            .build()

        testObserver.assertNotComplete()

        payloadCallback.captured.onPayloadTransferUpdate("endpoint", transferUpdate)

        testObserver.assertError(KickprotocolSendException::class.java)
    }

    @Test
    fun `sending with error`() {
        val task = mockk<Task<Void>>()
        val failureListener = slot<OnFailureListener>()
        val testObserver = TestObserver<Unit>()
        val payload = slot<Payload>()

        every { task.addOnSuccessListener(any()) } returns task
        every { task.addOnFailureListener(capture(failureListener)) } returns task
        every { connectionsClient.sendPayload("endpoint", capture(payload)) } returns task

        kickprotocol.send("endpoint", IdleMessage()).subscribe(testObserver)
        failureListener.captured.onFailure(KickprotocolSendException("endpoint"))

        testObserver.assertError(KickprotocolSendException::class.java)
    }

    @Test
    fun broadcasting() {
        val okStatus = Status(ConnectionsStatusCodes.STATUS_OK)
        val connectionLifecycleCallback = slot<ConnectionLifecycleCallback>()

        val task1 = mockk<Task<Void>>()
        val task2 = mockk<Task<Void>>()
        val successListener1 = slot<OnSuccessListener<Void>>()
        val successListener2 = slot<OnSuccessListener<Void>>()
        val testObserver = TestObserver<Unit>()
        val payload1 = slot<Payload>()
        val payload2 = slot<Payload>()

        every {
            connectionsClient.startAdvertising(
                any(),
                any(),
                capture(connectionLifecycleCallback),
                any()
            )
        } returns mockk(relaxed = true)

        kickprotocol.advertise("").subscribe()

        connectionLifecycleCallback.captured.onConnectionResult("endpoint1", ConnectionResolution(okStatus))
        connectionLifecycleCallback.captured.onConnectionResult("endpoint2", ConnectionResolution(okStatus))

        every { task1.addOnSuccessListener(capture(successListener1)) } returns task1
        every { task2.addOnSuccessListener(capture(successListener2)) } returns task2
        every { task1.addOnFailureListener(any()) } returns task1
        every { task2.addOnFailureListener(any()) } returns task2
        every { connectionsClient.sendPayload("endpoint1", capture(payload1)) } returns task1
        every { connectionsClient.sendPayload("endpoint2", capture(payload2)) } returns task2

        kickprotocol.broadcast(IdleMessage()).subscribe(testObserver)
        successListener1.captured.onSuccess(null)
        successListener2.captured.onSuccess(null)

        testObserver.assertComplete()
        payload1.captured.asBytes()?.toString(Charsets.UTF_8) ?: "" shouldBeEqualTo "IdleMessage\n{}"
        payload2.captured.asBytes()?.toString(Charsets.UTF_8) ?: "" shouldBeEqualTo "IdleMessage\n{}"
    }

    @Test
    fun `broadcasting with error`() {
        val okStatus = Status(ConnectionsStatusCodes.STATUS_OK)
        val connectionLifecycleCallback = slot<ConnectionLifecycleCallback>()

        val task1 = mockk<Task<Void>>()
        val task2 = mockk<Task<Void>>()
        val successListener = slot<OnSuccessListener<Void>>()
        val failureListener = slot<OnFailureListener>()
        val testObserver = TestObserver<Unit>()

        every {
            connectionsClient.startAdvertising(
                any(),
                any(),
                capture(connectionLifecycleCallback),
                any()
            )
        } returns mockk(relaxed = true)

        kickprotocol.advertise("").subscribe()

        connectionLifecycleCallback.captured.onConnectionResult("endpoint1", ConnectionResolution(okStatus))
        connectionLifecycleCallback.captured.onConnectionResult("endpoint2", ConnectionResolution(okStatus))

        every { task1.addOnSuccessListener(capture(successListener)) } returns task1
        every { task2.addOnSuccessListener(any()) } returns task2
        every { task1.addOnFailureListener(any()) } returns task1
        every { task2.addOnFailureListener(capture(failureListener)) } returns task2
        every { connectionsClient.sendPayload("endpoint1", any()) } returns task1
        every { connectionsClient.sendPayload("endpoint2", any()) } returns task2

        kickprotocol.broadcast(IdleMessage()).subscribe(testObserver)
        successListener.captured.onSuccess(null)
        failureListener.captured.onFailure(KickprotocolSendException("endpoint2"))

        testObserver.assertError(KickprotocolSendException::class.java)
    }

    @Test
    fun stopping() {
        val okStatus = Status(ConnectionsStatusCodes.STATUS_OK)
        val discoveryCallback = slot<EndpointDiscoveryCallback>()
        val connectionLifecycleCallback = slot<ConnectionLifecycleCallback>()

        every {
            connectionsClient.startDiscovery(
                any(),
                capture(discoveryCallback),
                any()
            )
        } returns mockk(relaxed = true)

        every {
            connectionsClient.startAdvertising(
                any(),
                any(),
                capture(connectionLifecycleCallback),
                any()
            )
        } returns mockk(relaxed = true)

        kickprotocol.discover().subscribe()
        kickprotocol.advertise("").subscribe()

        discoveryCallback.captured.onEndpointFound("endpoint1", mockk())
        connectionLifecycleCallback.captured.onConnectionResult("endpoint2", ConnectionResolution(okStatus))

        every { connectionsClient.stopAdvertising() } just Runs
        every { connectionsClient.stopDiscovery() } just Runs
        every { connectionsClient.stopAllEndpoints() } just Runs

        kickprotocol.stop()

        kickprotocol.foundEndpoints.shouldBeEmpty()
        kickprotocol.connectedEndpoints.shouldBeEmpty()

        verify {
            connectionsClient.stopDiscovery()
            connectionsClient.stopAdvertising()
            connectionsClient.stopAllEndpoints()
        }
    }

    @Test
    fun `discovery events`() {
        val testObserver = TestObserver<DiscoveryEvent>()
        val discoveryCallback = slot<EndpointDiscoveryCallback>()
        val task = mockk<Task<Void>>()

        kickprotocol.discoveryEvents.subscribe(testObserver)

        every { task.addOnSuccessListener(any()) } returns task
        every { task.addOnFailureListener(any()) } returns task

        every {
            connectionsClient.startDiscovery(
                Kickprotocol.DEFAULT_SERVICE_ID,
                capture(discoveryCallback),
                any()
            )
        } returns task

        kickprotocol.discover().subscribe()

        discoveryCallback.captured.onEndpointFound("endpoint", mockk())
        kickprotocol.foundEndpoints shouldEqual listOf("endpoint")

        discoveryCallback.captured.onEndpointLost("endpoint")
        kickprotocol.foundEndpoints.shouldBeEmpty()

        testObserver
            .assertValueSequenceOnly(
                listOf(
                    DiscoveryEvent.Found("endpoint"),
                    DiscoveryEvent.Lost("endpoint")
                )
            )
            .assertNotComplete()
    }

    @Test
    fun `connection events`() {
        val okStatus = Status(ConnectionsStatusCodes.STATUS_OK)

        val testObserver = TestObserver<ConnectionEvent>()
        val connectionLifecycleCallback = slot<ConnectionLifecycleCallback>()
        val task = mockk<Task<Void>>()

        kickprotocol.connectionEvents.subscribe(testObserver)

        every { task.addOnSuccessListener(any()) } returns task
        every { task.addOnFailureListener(any()) } returns task

        every {
            connectionsClient.requestConnection(
                "test",
                "endpoint",
                capture(connectionLifecycleCallback)
            )
        } returns task

        kickprotocol.connect("test", "endpoint").subscribe()

        connectionLifecycleCallback.captured.onConnectionResult("endpoint", ConnectionResolution(okStatus))
        kickprotocol.connectedEndpoints shouldEqual listOf("endpoint")

        connectionLifecycleCallback.captured.onDisconnected("endpoint")
        kickprotocol.connectedEndpoints.shouldBeEmpty()

        testObserver
            .assertValueSequenceOnly(
                listOf(
                    ConnectionEvent.Connected("endpoint"),
                    ConnectionEvent.Disconnected("endpoint")
                )
            )
            .assertNotComplete()
    }

    @Test
    fun `connection rejected`() {
        val rejectedStatus = Status(ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED)
        val connectionLifecycleCallback = slot<ConnectionLifecycleCallback>()
        val testObserver = TestObserver<ConnectionEvent>()

        every {
            connectionsClient.startAdvertising(
                any(),
                any(),
                capture(connectionLifecycleCallback),
                any()
            )
        } returns mockk(relaxed = true)

        kickprotocol.connectionEvents.subscribe(testObserver)
        kickprotocol.advertise("").subscribe()
        connectionLifecycleCallback.captured.onConnectionResult("endpoint", ConnectionResolution(rejectedStatus))

        testObserver
            .assertNoValues()
            .assertError(KickprotocolConnectionException::class.java)
    }

    @Test
    fun `connection error`() {
        val rejectedStatus = Status(ConnectionsStatusCodes.STATUS_ERROR)
        val connectionLifecycleCallback = slot<ConnectionLifecycleCallback>()
        val testObserver = TestObserver<ConnectionEvent>()

        every {
            connectionsClient.startAdvertising(
                any(),
                any(),
                capture(connectionLifecycleCallback),
                any()
            )
        } returns mockk(relaxed = true)

        kickprotocol.connectionEvents.subscribe(testObserver)
        kickprotocol.advertise("").subscribe()
        connectionLifecycleCallback.captured.onConnectionResult("endpoint", ConnectionResolution(rejectedStatus))

        testObserver
            .assertNoValues()
            .assertError(KickprotocolConnectionException::class.java)
    }

    @Test
    fun `connection accepting`() {
        val connectionLifecycleCallback = slot<ConnectionLifecycleCallback>()

        every {
            connectionsClient.startAdvertising(
                any(),
                any(),
                capture(connectionLifecycleCallback),
                any()
            )
        } returns mockk(relaxed = true)

        every { connectionsClient.acceptConnection("endpoint", any()) } returns mockk()

        kickprotocol.advertise("").subscribe()
        connectionLifecycleCallback.captured.onConnectionInitiated("endpoint", mockk())

        verify { connectionsClient.acceptConnection("endpoint", any()) }
    }

    @Test
    fun `receiving messages`() {
        val connectionLifecycleCallback = slot<ConnectionLifecycleCallback>()
        val payloadCallback = slot<PayloadCallback>()
        val testObserver = TestObserver<KickprotocolMessageWithEndpoint<*>>()

        every {
            connectionsClient.startAdvertising(
                any(),
                any(),
                capture(connectionLifecycleCallback),
                any()
            )
        } returns mockk(relaxed = true)

        every { connectionsClient.acceptConnection(any(), capture(payloadCallback)) } returns mockk()

        kickprotocol.advertise("").subscribe()
        connectionLifecycleCallback.captured.onConnectionInitiated("irrelevant", mockk())

        kickprotocol.messageEvents.subscribe(testObserver)

        payloadCallback.captured.onPayloadReceived("endpoint1", Payload.fromBytes("IdleMessage\n{}".toByteArray()))
        payloadCallback.captured.onPayloadReceived("endpoint2", Payload.fromBytes("StartGameMessage\n{}".toByteArray()))

        testObserver.assertValuesOnly(
            KickprotocolMessageWithEndpoint("endpoint1", IdleMessage()),
            KickprotocolMessageWithEndpoint("endpoint2", StartGameMessage())
        )
    }

    @Test
    fun `receiving invalid messages`() {
        val connectionLifecycleCallback = slot<ConnectionLifecycleCallback>()
        val payloadCallback = slot<PayloadCallback>()
        val testObserver = TestObserver<KickprotocolMessageWithEndpoint<*>>()

        every {
            connectionsClient.startAdvertising(
                any(),
                any(),
                capture(connectionLifecycleCallback),
                any()
            )
        } returns mockk(relaxed = true)

        every { connectionsClient.acceptConnection(any(), capture(payloadCallback)) } returns mockk()

        kickprotocol.advertise("").subscribe()
        connectionLifecycleCallback.captured.onConnectionInitiated("irrelevant", mockk())

        kickprotocol.messageEvents.subscribe(testObserver)

        payloadCallback.captured.onPayloadReceived("endpoint1", Payload.fromBytes("IdleMessage\n{}".toByteArray()))
        payloadCallback.captured.onPayloadReceived("endpoint2", Payload.fromBytes("invalid\n{}".toByteArray()))

        testObserver
            .assertValues(KickprotocolMessageWithEndpoint("endpoint1", IdleMessage()))
            .assertError(KickprotocolInvalidMessageException::class.java)
    }
}
