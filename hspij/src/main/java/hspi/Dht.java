package hspi;

import com.pi4j.io.gpio.*;
import com.pi4j.wiringpi.Gpio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOError;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;

public class Dht {

    private static final Logger logger = LoggerFactory.getLogger(Dht.class);

    private final int dhtPin;

    public Dht(int dhtPin) {
        //Gpio.wiringPiSetupGpio();
        this.dhtPin = dhtPin;
    }

    public Value read() throws IOException {
        int[] data = {0, 0, 0, 0, 0};
        int[] cycles = new int[83];

        Gpio.pinMode(dhtPin, Gpio.OUTPUT);
        Gpio.digitalWrite(dhtPin, Gpio.HIGH);
        Gpio.delay(500);

        System.gc();
        Instant readStart = Instant.now();
        Gpio.digitalWrite(dhtPin, Gpio.LOW);
        Gpio.delay(18);
        Gpio.pinMode(dhtPin, Gpio.INPUT);

        int x = 0;
        int z = 0;
        while (x < 83) {
            boolean v = Gpio.digitalRead(dhtPin) == 1;
            if ((x % 2 == 0) == v) {
                // Instead of reading time we just count number of cycles until next level value
                cycles[x] += 1;
            } else {
                x += 1;
            }

            if (z++ % 7000 == 0 && Instant.now().isAfter(readStart.plusMillis(30))) {
                //logger.warn("Timout x={} z={} v={}  {}", x, z, v, cycles);
                throw new IOException("Reading time exceeded 10ms");
            }
        }


        for (int i = 0; i < 40; i++) {
            int lowCycle = cycles[2 * i + 3];
            int highCycle = cycles[2 * i + 4];

            data[i / 8] <<= 1;
            if (highCycle > lowCycle) {
                // High cycles are greater than 50us low cycle count, must be a 1.
                data[i / 8] |= 1;
            }
            // Else high cycles are less than (or equal to, a weird case) the 50us low
            // cycle count so this must be a zero.  Nothing needs to be changed in the
            // stored data.
        }

        // Check we read 40 bits and that the checksum matches.
        if (data[4] == ((data[0] + data[1] + data[2] + data[3]) & 0xFF)) {
            return new Value(data);
        }

        throw new IOException("Can not validate DHT read checksum");
    }

    public static class Value {

        @Override
        public String toString() {
            return "Value{" + Arrays.toString(value) + '}';
        }

        private final int[] value;

        public Value(int[] value) {
            this.value = value;
        }

        public int getTemperature() {
            int c = (((value[2] & 0x7F) << 8) + value[3]) / 10;
            if (c > 125) {
                c = value[2]; // for DHT11
            }
            if ((value[2] & 0x80) != 0) {
                c = -c;
            }
            return c;
        }

        public int getHumidity() {
            int h = ((value[0] << 8) + value[1]) / 10;
            if (h > 100) {
                h = value[0]; // for DHT11
            }
            return h;
        }
    }
}
