package hspi

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.undertow.Undertow
import io.undertow.server.HttpServerExchange
import io.undertow.server.RoutingHandler
import io.undertow.util.Headers
import io.undertow.util.Methods

class Server(
        port: Int,
        val service: Service
) {

    val server: Undertow;

    val mapper = jacksonObjectMapper().apply {
        propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
    }

    init {
        server = Undertow.builder()
                .addHttpListener(port, "0.0.0.0")
                .setHandler(handler())
                .build();
    }

    private fun handler() = RoutingHandler()
            .get("/", this::root)
            .get("/state", this::state)
//            .get("humidity/hight", this::humidityHigh)
            .post("humidity/hight", this::humidityHigh)
//            .get("humidity/low", this::humidityLow)
            .post("humidity/low", this::humidityLow)


    fun start() {
        server.start();
    }

    fun root(exchange: HttpServerExchange) {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.getResponseSender().send("SmartPi server is running");
    }

    fun state(exchange: HttpServerExchange) {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "applictaion/json")
        exchange.getResponseSender()
                .send(
                        mapper.writeValueAsString(service.state)
                )
    }

    fun humidityHigh(exchange: HttpServerExchange) {
        if (exchange.requestMethod == Methods.POST && exchange.queryParameters.containsKey("level")) {
            exchange.queryParameters["level"].let {
                if (it != null && it.isNotEmpty()) {
                    service.updateHumidityLevels(service.config.humidityLow, it.first().toString().toInt());
                }
            }
        }
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "plain/text")
        exchange.getResponseSender().send("${service.config.humidityHigh}")
    }

    fun humidityLow(exchange: HttpServerExchange) {
        if (exchange.requestMethod == Methods.POST && exchange.queryParameters.containsKey("level")) {
            exchange.queryParameters["level"].let {
                if (it != null && it.isNotEmpty()) {
                    service.updateHumidityLevels(it.first().toString().toInt(), service.config.humidityHigh);
                }
            }
        }
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "plain/text")
        exchange.getResponseSender().send("${service.config.humidityLow}")
    }
}