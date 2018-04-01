package hspi;

import com.pi4j.io.gpio.PinProvider;
import com.pi4j.io.gpio.RaspiBcmPin;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;

@BQConfig
public class HspiConfiguration {

    private int pinRelay;

    private int pinDht;

    private int pinLedLow;

    private int pinLedHigh;

    private int pinPower;

    public int getPinRelay() {
        return pinRelay;
    }

    @BQConfigProperty
    public void setPinRelay(int pinRelay) {
        this.pinRelay = pinRelay;
    }

//    public PinProvider getRelayGpio() {
//        RaspiBcmPin.getPinByAddress()
//    }

    public int getPinDht() {
        return pinDht;
    }

    @BQConfigProperty
    public void setPinDht(int pinDht) {
        this.pinDht = pinDht;
    }

    public int getPinLedLow() {
        return pinLedLow;
    }

    @BQConfigProperty
    public void setPinLedLow(int pinLedLow) {
        this.pinLedLow = pinLedLow;
    }

    public int getPinLedHigh() {
        return pinLedHigh;
    }

    @BQConfigProperty
    public void setPinLedHigh(int pinLedHigh) {
        this.pinLedHigh = pinLedHigh;
    }

    public int getPinPower() {
        return pinPower;
    }

    @BQConfigProperty
    public void setPinPower(int pinPower) {
        this.pinPower = pinPower;
    }

    @Override
    public String toString() {
        return "HspiConfiguration{" +
            "pinRelay=" + pinRelay +
            ", pinDht=" + pinDht +
            ", pinLedLow=" + pinLedLow +
            ", pinLedHigh=" + pinLedHigh +
            ", pinPower=" + pinPower +
            '}';
    }
}
