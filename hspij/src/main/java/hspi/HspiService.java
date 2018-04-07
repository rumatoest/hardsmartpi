package hspi;

import hspi.app.Configuration;

import com.google.inject.Inject;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.RaspiPinNumberingScheme;
import com.pi4j.io.gpio.RaspiGpioProvider;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

public class HspiService {

    private static final Logger logger = LoggerFactory.getLogger(HspiService.class);

    private GpioController gpio;

    private final ExecutorService exService = Executors.newSingleThreadExecutor();

    private HspiState state = new HspiState();

    private Dht dht11;

    private Configuration config;

    private GpioPinDigitalInput gpioLedH;

    private GpioPinDigitalInput gpioLedL;

    private GpioPinDigitalOutput gpioHPower;

    private GpioPinDigitalOutput gpioRelay;

    @Inject
    public HspiService(Configuration config) {
        this.config = config;

        if (config.isDevMode()) {
            exService.submit(this::loopDev);
        } else {
            GpioFactory.setDefaultProvider(new RaspiGpioProvider(RaspiPinNumberingScheme.BROADCOM_PIN_NUMBERING));
            this.gpio = GpioFactory.getInstance();

            this.dht11 = new Dht(config.getPinDht());
            this.provisionPins();
            exService.submit(this::loop);
        }

        logger.info("{}", config);
    }

    private final void provisionPins() {
        gpioLedH = gpio.provisionDigitalInputPin(RaspiPin.getPinByAddress(config.getPinLedHigh()));
        gpioLedL = gpio.provisionDigitalInputPin(RaspiPin.getPinByAddress(config.getPinLedLow()));
        gpioHPower = gpio.provisionDigitalOutputPin(RaspiPin.getPinByAddress(config.getPinPower()));
        gpioRelay = gpio.provisionDigitalOutputPin(RaspiPin.getPinByAddress(config.getPinRelay()));

        // Add gpio Listeners
        gpioLedL.addListener(getLedPowerLowListener());
    }

    public HspiState getState() {
        return state;
    }

    private void loopDev() {
        while (true) {
            try {
                state.setTemperature(20 + ThreadLocalRandom.current().nextInt(10));
                state.setHumidity(35 + ThreadLocalRandom.current().nextInt(30));
                state.setHumidifierLevel(ThreadLocalRandom.current().nextInt(0, 3));
                state.setHumidifierPowered(state.getHumidifierLevel() > 0);
                state.setFanPowered(ThreadLocalRandom.current().nextBoolean());
                Thread.sleep(10000);
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
    }

    private void loop() {
        while (true) {
            try {
                this.updateStateNoDht();
                this.updateDht();
                this.doLogick();
                this.updateStateNoDht();
                Thread.sleep(2000);
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
    }

    void updateStateNoDht() {
        this.state.setHumidifierPowered(gpioLedL.isHigh());
        if (this.state.isHumidifierPowered()) {
            this.state.setHumidifierLevel(gpioLedH.isHigh() ? 2 : 1);
        } else {
            this.state.setHumidifierLevel(0);
        }

        logger.info("UPDATED: {}", this.state);
        this.state.setFanPowered(gpioRelay.isHigh());
    }

    private GpioPinListenerDigital getLedPowerLowListener() {
        return new GpioPinListenerDigital() {

            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                logger.info("EVENT LED power low pin state is {} edge {}", event.getState(), event.getEdge());
                if (event.getState() == PinState.LOW) {
                    state.setHumidifierPowered(false);
                    state.setHumidifierLevel(0);
                }
            }
        };
    }

    void updateDht() throws InterruptedException {
        int retry = 5;
        while (retry > 0) {
            try {
                Dht.Value dval = this.dht11.read();
                logger.info("DHT {}", dval);
                this.state.setHumidity(dval.getHumidity());
                this.state.setTemperature(dval.getTemperature());
                retry = -1;
            } catch (IOException ex) {
                if (--retry == 0) {
                    logger.error("DHT READ FAILED {} ", ex.getMessage());
                } else {
                    Thread.sleep(750);
                }
            }
        }
    }

    void doLogick() {
        if (state.getHumidity() < config.getHumidOk()) {
            this.relayOff();
            this.humidifierOnHigh();
        } else if (state.getHumidity() < config.getHumidHigh()) {
            this.relayOff();
            this.humidifierOnLow();
        } else {
            this.relayOn();
            this.humidifierOff();
        }
    }

    private void relayOn() {
        gpioRelay.setState(true);
    }

    private void relayOff() {
        gpioRelay.setState(false);
    }

    private void humidifierOnHigh() {
        if (state.getHumidifierLevel() == 0) {
            humidifierPress(2);
        }
        if (state.getHumidifierLevel() == 1) {
            humidifierPress(1);
        }
    }

    private void humidifierOnLow() {
        if (state.getHumidifierLevel() == 0) {
            humidifierPress(1);
        }
        if (state.getHumidifierLevel() == 2) {
            humidifierPress(2);
        }
    }

    private void humidifierOff() {
        if (state.getHumidifierLevel() == 1) {
            humidifierPress(2);
        }
        if (state.getHumidifierLevel() == 2) {
            humidifierPress(1);
        }
    }

    private void humidifierPress(int times) {
        gpioHPower.setState(false);
        try {
            for (int i = 0; i < times; i++) {
                gpioHPower.setState(true);
                Thread.sleep(250);
                gpioHPower.setState(false);
                logger.info("Humidifier POWER pressed");
                Thread.sleep(50);
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            gpioHPower.setState(false);
        }
    }

    public int getHumidityOk() {
        return config.getHumidOk();
    }

    public int getHumidityHigh() {
        return config.getHumidHigh();
    }

    public void updateHumidityLimits(int ok, int high) {
        config.setHumidOk(ok);
        config.setHumidHigh(high);
        logger.info("Humidity levels ok:{} high:{}", ok, high);
    }
}
