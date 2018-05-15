package hspi

import java.beans.ConstructorProperties

/**
 * System configuration properties
 */
data class AppConfig
@ConstructorProperties("noPi",
        "pinRelay",
        "pinDht",
        "pinLedLow",
        "pinLedHigh",
        "pinPower",
        "humidOk",
        "humidHigh",
        "serverPort")
constructor(
        var noPi: Boolean = false,
        val pinRelay: Int,
        val pinDht: Int,
        val pinLedLow: Int,
        val pinLedHigh: Int,
        val pinPower: Int,
        var humidityLow: Int,
        var humidityHigh: Int,
        val serverPort: Int,
        val botName: String,
        val botToken: String
) {
    override fun toString(): String {
        return "AppConfig(noPi=$noPi, pinRelay=$pinRelay, pinDht=$pinDht, pinLedLow=$pinLedLow, pinLedHigh=$pinLedHigh, pinPower=$pinPower, humidityLow=$humidityLow, humidityHigh=$humidityHigh, serverPort=$serverPort)"
    }
}
