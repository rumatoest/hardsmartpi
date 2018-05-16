package hspi

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.undertow.Handlers
import io.undertow.Undertow
import io.undertow.io.Sender
import io.undertow.server.HttpServerExchange
import io.undertow.util.Headers
import io.undertow.util.Methods

/**
 * Web API frontend to {@see hspi.Service}
 */
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

    private fun handler() = Handlers.path()
        .addExactPath("/", this::root)
        .addExactPath("/state", this::state)
        .addExactPath("/humidity/high", this::humidityHigh)
        .addExactPath("/humidity/low", this::humidityLow)
        .addExactPath("/votes/h", this::votesH)
        .addExactPath("/votes/f", this::votesF)

    fun sendText(exchange: HttpServerExchange): Sender {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "sendText/plain")
        return exchange.responseSender
    }

    fun start() {
        server.start();
    }

    fun root(exchange: HttpServerExchange) {
        sendText(exchange).send("SmartPi server is running");
    }

    fun state(exchange: HttpServerExchange) {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "applictaion/json")
        exchange.getResponseSender()
            .send(
                mapper.writeValueAsString(StateResponse(service.state))
            )
    }

    fun votesH(exchange: HttpServerExchange) {
        sendText(exchange).send("${service.votes.humidifier}")
    }

    fun votesF(exchange: HttpServerExchange) {
        sendText(exchange).send("${service.votes.fan}")
    }

    fun humidityHigh(exchange: HttpServerExchange) {
        if (exchange.requestMethod == Methods.POST && exchange.queryParameters.containsKey("level")) {
            exchange.queryParameters["level"].let {
                if (it != null && it.isNotEmpty()) {
                    service.updateHumidityLevels(service.config.humidityLow, it.first().toString().toInt());
                }
            }
        }
        sendText(exchange).send("${service.config.humidityHigh}")
    }

    fun humidityLow(exchange: HttpServerExchange) {
        if (exchange.requestMethod == Methods.POST && exchange.queryParameters.containsKey("level")) {
            exchange.queryParameters["level"].let {
                if (it != null && it.isNotEmpty()) {
                    service.updateHumidityLevels(it.first().toString().toInt(), service.config.humidityHigh);
                }
            }
        }
        sendText(exchange).send("${service.config.humidityLow}")
    }
}

data class StateResponse(
    val temperature: Int,
    val humidity: Int,
    val humidifierLevel: Int,
    val humidifierPowered: String,
    val fanPowered: String
) {
    constructor(state: State) : this(
        state.temperature,
        state.humidity,
        state.humidifierLevel,
        if (state.isHumidifierPowered) "ON" else "OFF",
        if (state.isFanPowered) "ON" else "OFF"
    )
}