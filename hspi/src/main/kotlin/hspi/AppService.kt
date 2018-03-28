package hspi

import com.google.inject.Inject
import com.google.inject.Provider
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.server.RoutingHandler
import io.undertow.util.Headers

class ApplicationService
@Inject
constructor(
        private val configuration: ApplicationConfiguration
) {

    fun message() = configuration.message
}

class RootHandlerProvider
@Inject
constructor( val messageHandler: MessageHandler ) : Provider<HttpHandler> {

    override fun get(): HttpHandler {
        return RoutingHandler()
                .get("/", messageHandler::get)
    }

}

class MessageHandler @Inject constructor(
        private val applicationService: ApplicationService
) {
    fun get(exchange: HttpServerExchange) {
        val start = System.currentTimeMillis()
//        delay(1000L)
        exchange.responseHeaders.put(Headers.CONTENT_TYPE, "text/html")
        exchange.responseSender.send("""${applicationService.message()} <br/> Total time: ${System.currentTimeMillis() - start}ms""")
    }
}