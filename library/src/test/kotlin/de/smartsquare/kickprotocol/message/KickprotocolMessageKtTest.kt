package de.smartsquare.kickprotocol.message

import android.os.ParcelFileDescriptor
import com.google.android.gms.nearby.connection.Payload
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import de.smartsquare.kickprotocol.KickprotocolInvalidMessageException
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.withCause
import org.junit.jupiter.api.Test

/**
 * @author Ruben Gees
 */
class KickprotocolMessageKtTest {

    private val moshi = Moshi.Builder().build()

    @Test
    fun `to payload with body`() {
        val message = CreateGameMessage("id", "name")

        val payloadAsString = message.toPayload(moshi).asBytes()?.toString(Charsets.UTF_8) ?: ""

        payloadAsString shouldBeEqualTo "CreateGameMessage\n{\"userId\":\"id\",\"username\":\"name\"}"
    }

    @Test
    fun `to payload without body`() {
        val message = IdleMessage()

        val payloadAsString = message.toPayload(moshi).asBytes()?.toString(Charsets.UTF_8) ?: ""

        payloadAsString shouldBeEqualTo "IdleMessage\n{}"
    }

    @Test
    fun `from payload with body`() {
        val payload = Payload.fromBytes("CreateGameMessage\n{\"userId\":\"id\",\"username\":\"name\"}".toByteArray())

        val message = payload.toNearbyMessage(moshi) as CreateGameMessage

        message.userId shouldBeEqualTo "id"
        message.username shouldBeEqualTo "name"
    }

    @Test
    fun `from payload with pretty printed body`() {
        val body = """
            CreateGameMessage
            {
              "userId": "id",
              "username": "name"
            }
        """.trimIndent()

        val payload = Payload.fromBytes(body.toByteArray())

        val message = payload.toNearbyMessage(moshi) as CreateGameMessage

        message.userId shouldBeEqualTo "id"
        message.username shouldBeEqualTo "name"
    }

    @Test
    fun `from payload without body`() {
        val payload = Payload.fromBytes("IdleMessage\n{}".toByteArray())

        val message = payload.toNearbyMessage(moshi)

        message shouldBeInstanceOf IdleMessage::class
    }

    @Test
    fun `from payload with null content`() {
        mockkStatic(ParcelFileDescriptor::class)

        every { ParcelFileDescriptor.open(any(), any()) } returns mockk()

        val payload = Payload.fromFile(createTempFile().apply { deleteOnExit() })

        val theFunction = { payload.toNearbyMessage(moshi) }

        theFunction shouldThrow KickprotocolInvalidMessageException::class
    }

    @Test
    fun `from payload with blank content`() {
        val payload = Payload.fromBytes(" ".toByteArray())

        val theFunction = { payload.toNearbyMessage(moshi) }

        theFunction shouldThrow KickprotocolInvalidMessageException::class
    }

    @Test
    fun `from payload with missing head`() {
        val payload = Payload.fromBytes("\n{}".toByteArray())

        val theFunction = { payload.toNearbyMessage(moshi) }

        theFunction shouldThrow KickprotocolInvalidMessageException::class
    }

    @Test
    fun `from payload with missing body`() {
        val payload = Payload.fromBytes("IdleMessage\n".toByteArray())

        val theFunction = { payload.toNearbyMessage(moshi) }

        theFunction shouldThrow KickprotocolInvalidMessageException::class
    }

    @Test
    fun `from payload with unknown message type`() {
        val payload = Payload.fromBytes("Unknown\n{}".toByteArray())

        val theFunction = { payload.toNearbyMessage(moshi) }

        theFunction shouldThrow KickprotocolInvalidMessageException::class
    }

    @Test
    fun `from payload with json missing fields as body`() {
        val payload = Payload.fromBytes("CreateGameMessage\n{}".toByteArray())

        val theFunction = { payload.toNearbyMessage(moshi) }

        theFunction shouldThrow KickprotocolInvalidMessageException::class withCause JsonDataException::class
    }

    @Test
    fun `from payload with invalid json as body`() {
        val payload = Payload.fromBytes("IdleMessage\n{?}".toByteArray())

        val theFunction = { payload.toNearbyMessage(moshi) }

        theFunction shouldThrow KickprotocolInvalidMessageException::class withCause JsonEncodingException::class
    }
}
