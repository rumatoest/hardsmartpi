package hspi;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;

public class HspiUwHandlerProvider implements Provider<HttpHandler> {

    HspiUwHttpHandler handler;

    @Inject
    public HspiUwHandlerProvider(HspiUwHttpHandler handler) {
        this.handler = handler;
    }

    @Override
    public HttpHandler get() {
        return new RoutingHandler().get("/", handler::root);
    }

}
