package hspi;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;

public class UndertowHandlerProvider implements Provider<HttpHandler> {

    UndertowHttpHandler handler;

    @Inject
    public UndertowHandlerProvider(UndertowHttpHandler handler) {
        this.handler = handler;
    }

    @Override
    public HttpHandler get() {
        return new RoutingHandler()
            .get("/", handler::root)
            .get("/state", handler::state)
            .get("/limits", handler::limits)
            .post("/limits", handler::limitsUpdate);

    }

}
