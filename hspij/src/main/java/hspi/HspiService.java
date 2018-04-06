package hspi;

import com.google.inject.Inject;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.RaspiPinNumberingScheme;
import com.pi4j.io.gpio.RaspiGpioProvider;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import hspi.app.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HspiService {

    private static final Logger logger = LoggerFactory.getLogger(HspiService.class);

    private static final GpioController GPIO;

    static {
        GpioFactory.setDefaultProvider(new RaspiGpioProvider(RaspiPinNumberingScheme.BROADCOM_PIN_NUMBERING));
        GPIO = GpioFactory.getInstance();
    }

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

        this.dht11 = new Dht(config.getPinDht());
//        this.dht11 = new Dht(
//            GPIO.provisionDigitalMultipurposePin(RaspiPin.getPinByAddress(config.getPinDht()), PinMode.DIGITAL_INPUT)
//        );
//        GPIO.provisionDigitalInputPin(RaspiPin.getPinByAddress(config.getPinDht())).i
        this.provisionPins();
        exService.submit(this::loop);

        logger.info("{}", config);
    }

    private final void provisionPins() {
        gpioLedH = GPIO.provisionDigitalInputPin(RaspiPin.getPinByAddress(config.getPinLedHigh()));
        gpioLedL = GPIO.provisionDigitalInputPin(RaspiPin.getPinByAddress(config.getPinLedLow()));
        gpioHPower = GPIO.provisionDigitalOutputPin(RaspiPin.getPinByAddress(config.getPinPower()));
        gpioRelay = GPIO.provisionDigitalOutputPin(RaspiPin.getPinByAddress(config.getPinRelay()));

        // Add GPIO Listeners
//        gpioLedL.addListener(getLedPowerLowListener());
    }

    public HspiState getState() {
        return state;
    }

    public String readPowers() {
        return "Low " + gpioLedL.getState() + "    High " + gpioLedH.getState();
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
}
