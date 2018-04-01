package hspi;

import com.google.inject.Inject;
import com.pi4j.io.gpio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HspiAppService {

    private static final Logger logger = LoggerFactory.getLogger(HspiAppService.class);

    private static final GpioController gpio;

    static {
        GpioFactory.setDefaultProvider(new RaspiGpioProvider(RaspiPinNumberingScheme.BROADCOM_PIN_NUMBERING));
        gpio = GpioFactory.getInstance();
    }

    private Dht11 dht11 = new Dht11();

    private HspiConfiguration config;

    GpioPinDigitalInput gpioLedH;

    GpioPinDigitalInput gpioLedL;

    @Inject
    public HspiAppService(HspiConfiguration config) {
        this.config = config;
        logger.info("{}", config);
        this.provisionPins();
    }


    private final void provisionPins() {
        gpioLedH = gpio.provisionDigitalInputPin(RaspiPin.getPinByAddress(config.getPinLedHigh()));
        gpioLedL = gpio.provisionDigitalInputPin(RaspiPin.getPinByAddress(config.getPinLedLow()));
    }

    public String readPowers() {
        return "Low " + gpioLedL.getState() + "    High " + gpioLedH.getState();
    }

    public int readHumidity() {
        return dht11.getTemperature(gpio, config.getPinDht());
    }
}
