package hspi;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.bootique.BQModule;

import io.bootique.config.ConfigurationFactory;
import io.bootique.undertow.handlers.RootHandler;
import io.undertow.server.HttpHandler;

public class HspiModule implements Module {

    @Override
    public void configure(Binder binder) {
        binder.bind(HttpHandler.class).annotatedWith(RootHandler.class).toProvider(HspiUwHandlerProvider.class);

    }

    @Provides
    @Singleton
    public HspiConfiguration configuration(ConfigurationFactory cf) {
        return cf.config(HspiConfiguration.class, "hardsmartpi");
    }
}
