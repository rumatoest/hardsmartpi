package hspi

import com.pi4j.io.gpio.*
import com.pi4j.io.gpio.event.GpioPinListenerDigital
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import mu.KotlinLogging
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom

/**
 * This service provide all logick to our system
 */
class Service(val config: AppConfig) {
    val logger = KotlinLogging.logger {}

    var bot: TelegramBot
    val server: Server
    var state: State
    val dht: Dht
    val gpio: GpioController?
    val gpioLedLow: GpioPinDigitalInput?
    val gpioLedHight: GpioPinDigitalInput?
    val gpioPowerButton: GpioPinDigitalOutput?
    val gpioRelay: GpioPinDigitalOutput?

    @Volatile
    private var noUpdatesUntil = Instant.now().minusSeconds(10)

    init {
        bot = TelegramBot(this)
        state = State().apply {
            humidity = config.humidityLow + 1
            isFanPowered = false
            isHumidifierPowered = false
        }
        dht = Dht(config.pinDht)
        server = Server(config.serverPort, this);

        if (!config.noPi) {
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
        } else {
            gpio = null
            gpioLedHight = null
            gpioLedLow = null
            gpioPowerButton = null
            gpioRelay = null
        }
    }

    fun run() {
        launch {
            loop()
        }
        server.start();
    }

    /**
     * DEBUG loop to run without RaspberryPi
     */
    suspend fun loopStub() {
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
        if (config.noPi) {
            loopStub()
            logger.debug { "Stub loop started" }
            return;
        }

        // DHT update takes unpredicted amount of time
        // I do not want to mix it with other logic
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
            // Here we read current system state
            this.state.isHumidifierPowered = gpioLedLow!!.isHigh()
            if (this.state.isHumidifierPowered) {
                this.state.humidifierLevel = if (gpioLedHight!!.isHigh()) 2 else 1
            } else {
                this.state.humidifierLevel = 0
            }
            state.isFanPowered = gpioRelay!!.isHigh

            logger.info("UPDATED: ${this.state}")

            val skipUpdate = synchronized(this) {
                Instant.now().isBefore(noUpdatesUntil)
            }

            if (skipUpdate) {
                logger.info { "System updates locked until ${noUpdatesUntil}" }
            } else {
                // Now we can decide what should we do with humidifier and fan
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
            }
            delay(2000)
        }
    }

    val votes: BotVotes
        get() = bot.votes

    @Synchronized
    fun delayUpdates(until: Instant) {
        noUpdatesUntil = until;
    }

    fun relayOn() {
        gpioRelay?.setState(true)
    }

    fun relayOff() {
        gpioRelay?.setState(false)
    }

    suspend fun humidifierOnHigh() {
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

    suspend fun humidifierOff() {
        if (state.humidifierLevel === 1) {
            humidifierPress(2)
        }
        if (state.humidifierLevel === 2) {
            humidifierPress(1)
        }
    }

    private suspend fun humidifierPress(times: Int) {
        gpioPowerButton?.setState(false)
        try {
            for (i in 0 until times) {
                gpioPowerButton?.setState(true)
                delay(250)
                gpioPowerButton?.setState(false)
                logger.info("Humidifier POWER pressed")
                Thread.sleep(50)
            }
        } catch (ex: Exception) {
            logger.error(ex) { ex.message }
        } finally {
            gpioPowerButton?.setState(false)
        }
    }

}
