package de.smartsquare.kickprotocol

import de.smartsquare.kickprotocol.message.KickprotocolMessage
import io.reactivex.Observable

@Suppress("UNCHECKED_CAST")
internal inline fun <reified T : KickprotocolMessage> Observable<MessageEvent<*>>.filterInstanceOf():
    Observable<MessageEvent<T>> {

    return this
        .filter { it is MessageEvent.Error || it is MessageEvent.Message && it.message is T }
        .map { it as MessageEvent<T> }
}

/**
 * Extension function for filtering all [MessageEvent.Message] instances from a stream.
 */
fun <T : KickprotocolMessage> Observable<MessageEvent<T>>.filterMessages(): Observable<MessageEvent.Message<T>> {
    return this
        .filter { it is MessageEvent.Message }
        .map { it as MessageEvent.Message<T> }
}

/**
 * Extension function for filtering all [MessageEvent.Error] instances from a stream.
 */
fun Observable<MessageEvent<*>>.filterErrors(): Observable<MessageEvent.Error> {
    return this
        .filter { it is MessageEvent.Error }
        .map { it as MessageEvent.Error }
}
