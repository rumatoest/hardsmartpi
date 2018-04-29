package hspi

import com.pi4j.wiringpi.Gpio
import mu.KotlinLogging
import java.io.IOException
import java.time.Instant

class Dht(val dhtPin: Int) {

    fun read(): DhtValue {
        var data = intArrayOf(0, 0, 0, 0, 0)
        var cycles = IntArray(83)

        Gpio.pinMode(dhtPin, Gpio.OUTPUT)
        Gpio.digitalWrite(dhtPin, Gpio.HIGH)
        Gpio.delay(500)

        System.gc()
        val readStart = Instant.now()
        Gpio.digitalWrite(dhtPin, Gpio.LOW)
        Gpio.delay(18)
        Gpio.pinMode(dhtPin, Gpio.INPUT)

        var x = 0
        var z = 0
        while (x < 83) {
            val v = Gpio.digitalRead(dhtPin) == 1
            if (x % 2 == 0 == v) {
                // Instead of reading time we just count number of cycles until next level value
                cycles[x] += 1
            } else {
                x += 1
            }

            if (z++ % 7000 == 0 && Instant.now().isAfter(readStart.plusMillis(30))) {
                //logger.warn("Timout x={} z={} v={}  {}", x, z, v, cycles);
                throw IOException("Reading time exceeded 10ms")
            }
        }


        for (i in 0..39) {
            val lowCycle = cycles[2 * i + 3]
            val highCycle = cycles[2 * i + 4]

            data[i / 8] = data[i / 8] shl 1
            if (highCycle > lowCycle) {
                // High cycles are greater than 50us low cycle count, must be a 1.
                data[i / 8] = data[i / 8] or 1
            }
            // Else high cycles are less than (or equal to, a weird case) the 50us low
            // cycle count so this must be a zero.  Nothing needs to be changed in the
            // stored data.
        }

        // Check we read 40 bits and that the checksum matches.
        if (data[4] == data[0] + data[1] + data[2] + data[3] and 0xFF) {
            return DhtValue(data)
        }

        throw IOException("Can not validate DHT read checksum")
    }
}

data class DhtValue(val value: IntArray) {
    val temperature: Int
        get() {
            var c = ((value[2] and 0x7F shl 8) + value[3]) / 10
            if (c > 125) {
                c = value[2] // for DHT11
            }
            if (value[2] and 0x80 != 0) {
                c = -c
            }
            return c
        }

    val humidity: Int
        get() {
            var h = ((value[0] shl 8) + value[1]) / 10
            if (h > 100) {
                h = value[0] // for DHT11
            }
            return h
        }
}