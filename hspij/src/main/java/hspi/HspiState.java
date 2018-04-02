package hspi;

public class HspiState {

    private int temperature;

    private int humidity;

    private boolean humidifierPowered;

    private int humidifierLevel;

    private boolean fanPowered;

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public boolean isHumidifierPowered() {
        return humidifierPowered;
    }

    public void setHumidifierPowered(boolean humidifierPowered) {
        this.humidifierPowered = humidifierPowered;
    }

    public int getHumidifierLevel() {
        return humidifierLevel;
    }

    public void setHumidifierLevel(int humidifierLevel) {
        this.humidifierLevel = humidifierLevel;
    }

    public boolean isFanPowered() {
        return fanPowered;
    }

    public void setFanPowered(boolean fanPowered) {
        this.fanPowered = fanPowered;
    }

    @Override
    public String toString() {
        return "HspiState{" +
            "temperature=" + temperature +
            ", humidity=" + humidity +
            ", humidifierPowered=" + humidifierPowered +
            ", humidifierLevel=" + humidifierLevel +
            ", fanPowered=" + fanPowered +
            '}';
    }
}
