package de.smartsquare.kickprotocol.nearby

import com.google.android.gms.nearby.connection.Payload
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import de.smartsquare.kickprotocol.domain.PendingLobby
import de.smartsquare.kickprotocol.domain.PlayingLobby

private val typeMap = mapOf(
    "GET_STATUS" to NearbyMessage.GetStatus::class.java,
    "IDLE" to NearbyMessage.Idle::class.java,
    "CREATE_GAME" to NearbyMessage.CreateGame::class.java,
    "PLAYING" to NearbyMessage.Playing::class.java
)

sealed class NearbyMessage(val type: String) {

    /* Serializes the content of this message. An empty String indicates that the message does not have a body. */
    open fun serialize(moshi: Moshi): String = ""

    class GetStatus : NearbyMessage("GET_STATUS")
    class Idle : NearbyMessage("IDLE")

    @JsonClass(generateAdapter = true)
    class CreateGame(val lobby: PendingLobby) : NearbyMessage("CREATE_GAME") {

        override fun serialize(moshi: Moshi): String {
            return moshi.adapter(PendingLobby::class.java).toJson(lobby)
        }
    }

    @JsonClass(generateAdapter = true)
    class Playing(val lobby: PlayingLobby) : NearbyMessage("PLAYING") {

        override fun serialize(moshi: Moshi): String {
            return moshi.adapter(PlayingLobby::class.java).toJson(lobby)
        }
    }

    fun toPayload(moshi: Moshi): Payload {
        val body = serialize(moshi)
        val bodyPart = if (body.isNotEmpty()) "\n" + body else ""

        return Payload.fromBytes("$type$bodyPart".toByteArray())
    }
}

fun Payload.toNearbyMessage(moshi: Moshi): NearbyMessage {
    val content = this.asBytes()?.toString(Charsets.UTF_8) ?: ""
    val head = content.substringBefore('\n')

    if (head.isBlank()) {
        throw NearbyInvalidMessageException("Message without head could not be parsed: $content")
    }

    val messageType = typeMap[head]
        ?: throw NearbyInvalidMessageException("Message with unknown type $type could not be parsed: $content")

    return if (messageType.isAnnotationPresent(JsonClass::class.java)) {
        // This message requires a body.
        val body = content.substringAfter('\n', "")

        if (body.isBlank()) {
            throw NearbyInvalidMessageException("Message without body could not be parsed: $body")
        }

        try {
            moshi.adapter(messageType).fromJson(body)
                ?: throw NearbyInvalidMessageException("Message could not be parsed: $content")
        } catch (exception: JsonDataException) {
            throw NearbyInvalidMessageException(
                "Message with invalid structure could not be parsed: $content",
                exception
            )
        } catch (exception: JsonEncodingException) {
            throw NearbyInvalidMessageException(
                "Message with invalid encoding could not be parsed: $content",
                exception
            )
        }
    } else {
        // This messages does not require a body.
        messageType.newInstance()
    }
}
