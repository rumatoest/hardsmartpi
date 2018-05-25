package hspi

import mu.KotlinLogging

import org.telegram.telegrambots.api.objects.Update
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.exceptions.TelegramApiException
import org.telegram.telegrambots.api.methods.send.SendMessage
import org.telegram.telegrambots.api.objects.Message

import kotlinx.coroutines.experimental.delay
import java.time.Instant

/**
 * Just to pass information about bot votes
 */
data class BotVotes(val humidifier: Int, val fan: Int)

class TelegramBot(val service: Service) : TelegramLongPollingBot() {

    val logger = KotlinLogging.logger {}

    // Telegram related code
    override fun getBotToken() = service.config.botToken

    override fun getBotUsername() = service.config.botName

    override fun onUpdateReceived(update: Update) {
        val message = update.message;
        if (!message.isUserMessage || message.text.isEmpty() || message.from == null) {
            return;
        }
        val reply = processCommand(message.from.id.toString(), message.text)
        if (reply.isNotBlank()) {
            sendMsg(message, reply)
        }
    }

    private fun sendMsg(message: Message, text: String) {
        val sendMessage = SendMessage()
        sendMessage.enableMarkdown(true)
        sendMessage.setChatId(message.chatId.toString())
        //sendMessage.replyToMessageId = message.messageId
        sendMessage.text = text
        try {
            sendApiMethod(sendMessage)
        } catch (e: TelegramApiException) {
            logger.error(e) { e.message }
        }
    }

    // End of Telegram related code

    fun canStartBot() = botToken.isNotBlank() && botUsername.isNotBlank()

    val voteDurationSec = 45L

    val lockServiceUpdateSec = 30L

    @Volatile
    var votePeriodStarted: Instant = Instant.now()

    var voteshOn: HashSet<String> = HashSet();

    var voteshOff: HashSet<String> = HashSet();

    var votesfOn: HashSet<String> = HashSet();

    var votesfOff: HashSet<String> = HashSet();

    val votes: BotVotes
        get() = BotVotes(voteshOn.size - voteshOff.size, votesfOn.size - votesfOff.size)

    @Synchronized
    fun processCommand(user: String, cmd: String): String {
        return when (cmd) {
            "hon" -> {
                voteshOn.add(user)
                voteshOff.remove(user)
                "Voted for humidifier ON"
            }
            "hoff" -> {
                voteshOff.add(user)
                voteshOn.remove(user)
                "Voted for humidifier OFF"
            }
            "fon" -> {
                votesfOn.add(user)
                votesfOff.remove(user)
                "Voted for fan ON"
            }
            "foff" -> {
                votesfOff.add(user)
                votesfOn.remove(user)
                "Voted for fan OFF"
            }
            "stat" -> {
                return statMessage()
            }
            else -> {
                "I can process only next messages: [stat|hon|hoff|fon|foff]"
            }
        }
    }

    @Synchronized
    suspend fun applyVotes() {
        val fan = votesfOn.size - votesfOff.size
        val hum = voteshOn.size - voteshOff.size

        votePeriodStarted = Instant.now()

        votesfOff.clear()
        votesfOn.clear()
        voteshOn.clear()
        voteshOff.clear()

        if (fan == 0 && hum == 0) {
            return;
        }

        val lockIntill = Instant.now().plusSeconds(lockServiceUpdateSec);
        service.delayUpdates(lockIntill)
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

        logger.info { "Votes applied H is ${hum} F is ${fan} . No changes for ${lockServiceUpdateSec} sec." }
    }

    fun statMessage() = "Humidity: ${service.state.humidity}% Temperature: ${service.state.temperature}C \n" +
            "Humidifier is ${service.state.isHumidifierPowered} Fan is ${service.state.isFanPowered}\n" +
            "Votes (${secondsLeft()} seconds left):\n" +
            " - humidifier ${voteshOn.size - voteshOff.size}\n" +
            " - fan: ${votesfOn.size - votesfOff.size}"

    fun secondsLeft(): Int {
        val secs = votePeriodStarted.plusSeconds(voteDurationSec).epochSecond - Instant.now().epochSecond
        return if (secs > 0) secs.toInt() else 0

    }

    suspend fun loop() {
        while (true) {
            if (Instant.now().isAfter(votePeriodStarted.plusSeconds(voteDurationSec))) {
                applyVotes()
            }
            delay(1000)
        }
    }
}