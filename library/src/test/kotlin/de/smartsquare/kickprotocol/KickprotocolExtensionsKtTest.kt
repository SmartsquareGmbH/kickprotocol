package de.smartsquare.kickprotocol

import de.smartsquare.kickprotocol.message.IdleMessage
import de.smartsquare.kickprotocol.message.StartGameMessage
import io.reactivex.Observable
import org.junit.jupiter.api.Test
import java.io.IOException

/**
 * @author Ruben Gees
 */
class KickprotocolExtensionsKtTest {

    @Test
    fun `filtering only messages`() {
        Observable
            .just(
                MessageEvent.Message("123", IdleMessage()),
                MessageEvent.Error("123", IOException()),
                MessageEvent.Message("123", StartGameMessage())
            )
            .filterMessages()
            .test()
            .assertValues(
                MessageEvent.Message("123", IdleMessage()),
                MessageEvent.Message("123", StartGameMessage())
            )
    }

    @Test
    fun `filtering only errors`() {
        Observable
            .just(
                MessageEvent.Message("123", IdleMessage()),
                MessageEvent.Error("123", IOException()),
                MessageEvent.Message("123", StartGameMessage())
            )
            .filterErrors()
            .test()
            .assertValueCount(1)
            .assertValue { it.endpointId == "123" && it.error is IOException }
    }
}
