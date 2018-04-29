package hspi

class State {

    var temperature: Int = 0

    var humidity: Int = 0

    @Volatile
    @set:Synchronized
    var isHumidifierPowered: Boolean = false

    @Volatile
    @set:Synchronized
    var humidifierLevel: Int = 0

    var isFanPowered: Boolean = false

    override fun toString(): String {
        return "HspiState{" +
                "temperature=" + temperature +
                ", humidity=" + humidity +
                ", humidifierPowered=" + isHumidifierPowered +
                ", humidifierLevel=" + humidifierLevel +
                ", fanPowered=" + isFanPowered +
                '}'.toString()
    }
}
