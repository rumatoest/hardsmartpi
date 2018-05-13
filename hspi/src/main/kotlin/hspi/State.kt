package hspi

/**
 * Current system state.
 * Do not want to do 100% thread safe code here
 * because of long delays between logic and state updates.
 */
class State {

    @Volatile
    @set:Synchronized
    var temperature: Int = 0

    @Volatile
    @set:Synchronized
    var humidity: Int = 0

    @Volatile
    @set:Synchronized
    var isHumidifierPowered: Boolean = false

    @Volatile
    @set:Synchronized
    var humidifierLevel: Int = 0

    @Volatile
    @set:Synchronized
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
