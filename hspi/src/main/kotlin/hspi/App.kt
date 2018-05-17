package hspi

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import mu.KotlinLogging
import org.apache.log4j.PropertyConfigurator

import java.io.FileInputStream

private val logger = KotlinLogging.logger {}

//@JvmName("App")
object App {

    @JvmStatic
    fun main(args: Array<String>) {
        loggerInit()
        ArgParser(args).parseInto(::AppArgs).run {

            val mapper = jacksonObjectMapper().apply {
                propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
            }

            var pBase = loadProperties(mapper, "classpath:config.json")

            if (this.propPath.isNotEmpty()) {
                val p = loadProperties(mapper, this.propPath)
                pBase.setAll(p)
            }

            var config = mapper.treeToValue(pBase, AppConfig::class.java);

            if (this.noPi) {
                config.noPi = true;
            }

            logger.debug { "$config" }

            val service = Service(config)
            service.run()

        }
    }

    private fun loggerInit() {
        PropertyConfigurator.configure(App.javaClass.classLoader.getResourceAsStream("log4j.properties"))
    };


    private fun loadProperties(m: ObjectMapper, path: String): ObjectNode {
        if (path.isBlank()) {
            return JsonNodeFactory.instance.objectNode();
        }

        return if (path.startsWith("classpath:")) {
            m.readTree(App.javaClass.classLoader.getResourceAsStream(path.removePrefix("classpath:"))) as ObjectNode
        } else {
            m.readTree(FileInputStream(path)) as ObjectNode
        }
    }
}

class AppArgs(parser: ArgParser) {
    val propPath by parser.storing("--config", help = "Properties path").default("")

    val noPi by parser.flagging("--nopi", help = "Dev mode to run app without RaspberryPi")
}