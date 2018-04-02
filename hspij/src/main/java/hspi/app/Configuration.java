package hspi.app;

import com.pi4j.io.gpio.PinProvider;
import com.pi4j.io.gpio.RaspiBcmPin;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;

@BQConfig
public class Configuration {

    private int pinRelay;

    private int pinDht;

    private int pinLedLow;

    private int pinLedHigh;

    private int pinPower;

    private int humidOk = 60;

    private int humidHigh = 80;

    public int getHumidOk() {
        return humidOk;
    }

    public void setHumidOk(int humidOk) {
        this.humidOk = humidOk;
    }

    public int getHumidHigh() {
        return humidHigh;
    }

    public void setHumidHigh(int humidHigh) {
        this.humidHigh = humidHigh;
    }

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
        return "Configuration{" +
            "pinRelay=" + pinRelay +
            ", pinDht=" + pinDht +
            ", pinLedLow=" + pinLedLow +
            ", pinLedHigh=" + pinLedHigh +
            ", pinPower=" + pinPower +
            ", humidOk=" + humidOk +
            ", humidHight=" + humidHigh +
            '}';
    }
}
