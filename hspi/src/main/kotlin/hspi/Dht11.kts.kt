package hspi

import com.pi4j.wiringpi.Gpio
import com.pi4j.wiringpi.GpioUtil

class DHT11 {
    private val dht11_dat = intArrayOf(0, 0, 0, 0, 0)

    init {
        // setup wiringPi
        if (Gpio.wiringPiSetup() == -1) {
            println(" ==>> GPIO SETUP FAILED")
        } else {
            GpioUtil.export(3, GpioUtil.DIRECTION_OUT)
        }
    }

    fun getTemperature(pin: Int) {
        var laststate = Gpio.HIGH
        var j = 0
        dht11_dat[4] = 0
        dht11_dat[3] = dht11_dat[4]
        dht11_dat[2] = dht11_dat[3]
        dht11_dat[1] = dht11_dat[2]
        dht11_dat[0] = dht11_dat[1]

        Gpio.pinMode(pin, Gpio.OUTPUT)
        Gpio.digitalWrite(pin, Gpio.LOW)
        Gpio.delay(18)

        Gpio.digitalWrite(pin, Gpio.HIGH)
        Gpio.pinMode(pin, Gpio.INPUT)

        for (i in 0 until MAXTIMINGS) {
            var counter = 0
            while (Gpio.digitalRead(pin) == laststate) {
                counter++
                Gpio.delayMicroseconds(1)
                if (counter == 255) {
                    break
                }
            }

            laststate = Gpio.digitalRead(pin)

            if (counter == 255) {
                break
            }

            /* ignore first 3 transitions */
            if (i >= 4 && i % 2 == 0) {
                /* shove each bit into the storage bytes */
                dht11_dat[j / 8] = dht11_dat[j / 8] shl 1
                if (counter > 16) {
                    dht11_dat[j / 8] = dht11_dat[j / 8] or 1
                }
                j++
            }
        }
        // check we read 40 bits (8bit x 5 ) + verify checksum in the last
        // byte
        if (j >= 40 && checkParity()) {
            var h = ((dht11_dat[0] shl 8) + dht11_dat[1]).toFloat() / 10
            if (h > 100) {
                h = dht11_dat[0].toFloat() // for DHT11
            }
            var c = ((dht11_dat[2] and 0x7F shl 8) + dht11_dat[3]).toFloat() / 10
            if (c > 125) {
                c = dht11_dat[2].toFloat() // for DHT11
            }
            if (dht11_dat[2] and 0x80 != 0) {
                c = -c
            }
            val f = c * 1.8f + 32
            println("Humidity = " + h + " Temperature = " + c + "(" + f + "f)")
        } else {
            println("Data not good, skip")
        }

    }

    private fun checkParity(): Boolean {
        return dht11_dat[4] == dht11_dat[0] + dht11_dat[1] + dht11_dat[2] + dht11_dat[3] and 0xFF
    }

    companion object {
        private val MAXTIMINGS = 85

        @Throws(Exception::class)
        @JvmStatic
        fun main(ars: Array<String>) {

            val dht = DHT11()

            for (i in 0..9) {
                Thread.sleep(2000)
                dht.getTemperature(21)
            }

            println("Done!!")

        }
    }
}