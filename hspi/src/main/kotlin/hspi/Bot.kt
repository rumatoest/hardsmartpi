package hspi

import kotlinx.coroutines.experimental.delay
import java.time.Instant

class TelegramBot(val service: Service) {

    val periodLengthSec = 45L

    val serviceLockSec = 30L

    var periodStart: Instant = Instant.now()

    var voteshOn: HashSet<String> = HashSet();

    var voteshOff: HashSet<String> = HashSet();

    var votesfOn: HashSet<String> = HashSet();

    var votesfOff: HashSet<String> = HashSet();

    val votes: BotVotes
        get() = BotVotes(voteshOn.size - voteshOff.size, votesfOn.size - votesfOff.size)

    @Synchronized
    fun processCommand(user: String, cmd: String): String? {
        when (cmd) {
            "hon" -> {
                voteshOn.add(user)
                voteshOff.remove(user)
            }
            "hoff" -> {
                voteshOff.add(user)
                voteshOn.remove(user)
            }
            "fon" -> {
                votesfOn.add(user)
                votesfOff.remove(user)
            }
            "foff" -> {
                votesfOff.add(user)
                votesfOn.remove(user)
            }
            "stat" -> {
                return statMessage()
            }
        }
        return null
    }

    @Synchronized
    suspend fun applyVotes() {
        val fan = votesfOn.size - votesfOff.size
        val hum = voteshOn.size - voteshOff.size

        votesfOff.clear()
        votesfOn.clear()
        voteshOn.clear()
        voteshOff.clear()

        periodStart = Instant.now()
        service.delayUpdates(Instant.now().plusSeconds(serviceLockSec))
        if (fan > 0) {
            service.relayOn()
        } else if (fan < 0) {
            service.relayOff()
        }

        if (hum > 0) {
            service.humidifierOnHigh()
        } else if (hum < 0) {
            service.humidifierOff()
        }
    }

    fun statMessage() = "Humidifier votes: ${voteshOn.size - voteshOff.size}\n" +
            "Fan votes: ${votesfOn.size - votesfOff.size}"


    suspend fun loop() {
        delay(1000)
    }
}

data class BotVotes(val humidifier: Int, val fan: Int)