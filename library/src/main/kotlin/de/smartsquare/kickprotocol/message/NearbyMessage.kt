package de.smartsquare.kickprotocol.message

import com.google.android.gms.nearby.connection.Payload
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import de.smartsquare.kickprotocol.KickprotocolInvalidMessageException

/**
 * @author Ruben Gees
 */
abstract class NearbyMessage {

    internal fun toPayload(moshi: Moshi): Payload {
        val head = javaClass.simpleName
        val body = moshi.adapter(javaClass).toJson(this)

        return Payload.fromBytes("$head\n$body".toByteArray())
    }
}

internal fun Payload.toNearbyMessage(moshi: Moshi): NearbyMessage {
    val content = this.asBytes()?.toString(Charsets.UTF_8)
        ?: throw KickprotocolInvalidMessageException("Message without content could not be parsed")

    val head = content.substringBefore('\n')
    val body = content.substringAfter('\n')

    if (head.isBlank()) {
        throw KickprotocolInvalidMessageException("Message without head could not be parsed: $content")
    } else if (body.isBlank()) {
        throw KickprotocolInvalidMessageException("Message without body could not be parsed: $content")
    }

    val type = try {
        Class.forName("de.smartsquare.kickprotocol.message.$head")
    } catch (exception: ClassNotFoundException) {
        throw KickprotocolInvalidMessageException("Message with unknown type $type could not be parsed: $content")
    }

    return try {
        moshi.adapter(type).fromJson(body) as? NearbyMessage
            ?: throw KickprotocolInvalidMessageException("Message could not be parsed: $content")
    } catch (exception: JsonDataException) {
        throw KickprotocolInvalidMessageException(
            "Message with invalid structure could not be parsed: $content",
            exception
        )
    } catch (exception: JsonEncodingException) {
        throw KickprotocolInvalidMessageException(
            "Message with invalid encoding could not be parsed: $content",
            exception
        )
    }
}