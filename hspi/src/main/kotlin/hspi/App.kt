package hspi

import com.google.inject.Provides
import io.bootique.config.ConfigurationFactory
import io.bootique.kotlin.config.modules.KotlinConfigModuleProvider
import io.bootique.kotlin.core.KotlinBQModuleProvider
import io.bootique.kotlin.core.KotlinBootique
import io.bootique.kotlin.extra.config
import io.bootique.kotlin.guice.KotlinBinder
import io.bootique.kotlin.guice.KotlinModule
import io.bootique.undertow.UndertowModule
import io.bootique.undertow.UndertowModuleProvider
import io.bootique.undertow.handlers.RootHandler
import io.undertow.server.HttpHandler

import javax.inject.Singleton

fun main(args: Array<String>) {
    KotlinBootique(args)
            .args("--server", "--config=classpath:config.kts")
            .module(ApplicationModuleProvider())
            .exec()
            .exit()
}


/**
 * Define class with application config.
 */
data class ApplicationConfiguration(
        val message: String = ""
)

/**
 * Define provider for Application module, and define modules dependencies explicitly, in contrast with [KotlinBootique.autoLoadModules].
 */
class ApplicationModuleProvider : KotlinBQModuleProvider {
    override val module = ApplicationModule()
    override val overrides = listOf(UndertowModule::class)
    override val dependencies = listOf(
            UndertowModuleProvider(),
            KotlinConfigModuleProvider()
    )
}
/**
 * Wire interfaces via GUICE
 */
class ApplicationModule : KotlinModule {
    override fun configure(binder: KotlinBinder) {
        binder.bind(HttpHandler::class).annotatedWith(RootHandler::class).toProvider(RootHandlerProvider::class).asSingleton()
    }

    @Provides
    @Singleton
    fun configuration(configurationFactory: ConfigurationFactory): ApplicationConfiguration {
        return configurationFactory.config("application")
    }
}

