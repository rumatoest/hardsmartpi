package hspi

import com.pi4j.io.gpio.*
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent
import com.pi4j.io.gpio.event.GpioPinListenerDigital
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import mu.KotlinLogging
import java.util.concurrent.ThreadLocalRandom

class Service(val config: AppConfig) {
    val logger = KotlinLogging.logger {}

    val server: Server
    var state: State
    val dht: Dht
    val gpio: GpioController
    val gpioLedLow: GpioPinDigitalInput
    val gpioLedHight: GpioPinDigitalInput
    val gpioPowerButton: GpioPinDigitalOutput
    val gpioRelay: GpioPinDigitalOutput

    init {
        state = State().apply {
            humidity = config.humidityLow + 1
            isFanPowered = false
            isHumidifierPowered = false
        }
        dht = Dht(config.pinDht)
        server = Server(config.serverPort, this);

        GpioFactory.setDefaultProvider(RaspiGpioProvider(RaspiPinNumberingScheme.BROADCOM_PIN_NUMBERING));
        gpio = GpioFactory.getInstance();
        gpioLedHight = gpio.provisionDigitalInputPin(RaspiPin.getPinByAddress(config.pinLedHigh))
        gpioLedLow = gpio.provisionDigitalInputPin(RaspiPin.getPinByAddress(config.pinLedLow))
        gpioPowerButton = gpio.provisionDigitalOutputPin(RaspiPin.getPinByAddress(config.pinPower))
        gpioRelay = gpio.provisionDigitalOutputPin(RaspiPin.getPinByAddress(config.pinRelay))

        // Add gpio Listeners
        gpioLedLow.addListener(GpioPinListenerDigital { event ->
            logger.info { "EVENT LED power low pin state is ${event.state} edge ${event.edge}" }
            if (event.state == PinState.LOW) {
                state.isHumidifierPowered = false
                state.humidifierLevel = 0
            }
        })
    }

    fun run() {
        launch {
            if (config.noPi) {
                loopStub()
            } else {
                loop()
            }
        }
        server.start();
    }

    suspend fun loopStub() {
        logger.debug { "Stub loop started" }
        while (true) {
            try {
                state.temperature = 20 + ThreadLocalRandom.current().nextInt(10)
                state.humidity = 35 + ThreadLocalRandom.current().nextInt(30)
                state.humidifierLevel = ThreadLocalRandom.current().nextInt(0, 3)
                state.isHumidifierPowered = state.humidifierLevel > 0
                state.isFanPowered = ThreadLocalRandom.current().nextBoolean()
                delay(10000)
            } catch (ex: Throwable) {
                logger.error(ex) { ex.message }
            }

        }
    }

    fun updateHumidityLevels(low: Int, hight: Int) {
        this.config.humidityHigh = hight;
        this.config.humidityLow = low;
        logger.info { "UPDATED humidity low ${low} high ${hight}" }
    }

    suspend fun loop() {
        launch {
            while (true) {
                var error = false
                var enum = 0
                try {
                    val readings = dht.read()
                    state.humidity = readings.humidity
                    state.temperature = readings.temperature
                } catch (ex: Throwable) {
                    if (++enum > 4) {
                        logger.error { ex.message }
                    }
                }
                if (!error) {
                    delay(2000)
                }
            }
        }

        while (true) {
            if (state.humidity < config.humidityLow) {
                this.relayOff()
                this.humidifierOnHigh()
            } else if (state.humidity < config.humidityHigh) {
                this.relayOff()
                this.humidifierOnLow()
            } else {
                this.relayOn()
                this.humidifierOff()
            }
            delay(2000)
        }
    }

    private fun relayOn() {
        gpioRelay.setState(true)
    }

    private fun relayOff() {
        gpioRelay.setState(false)
    }

    private suspend fun humidifierOnHigh() {
        if (state.humidifierLevel === 0) {
            humidifierPress(2)
        }
        if (state.humidifierLevel === 1) {
            humidifierPress(1)
        }
    }

    private suspend fun humidifierOnLow() {
        if (state.humidifierLevel === 0) {
            humidifierPress(1)
        }
        if (state.humidifierLevel === 2) {
            humidifierPress(2)
        }
    }

    private suspend fun humidifierOff() {
        if (state.humidifierLevel === 1) {
            humidifierPress(2)
        }
        if (state.humidifierLevel === 2) {
            humidifierPress(1)
        }
    }

    private suspend fun humidifierPress(times: Int) {
        gpioPowerButton.setState(false)
        try {
            for (i in 0 until times) {
                gpioPowerButton.setState(true)
                delay(250)
                gpioPowerButton.setState(false)
                logger.info("Humidifier POWER pressed")
                Thread.sleep(50)
            }
        } catch (ex: Exception) {
            logger.error(ex) { ex.message }
        } finally {
            gpioPowerButton.setState(false)
        }
    }

}
