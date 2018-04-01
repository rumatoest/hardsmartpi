package hspi;

import com.google.inject.Inject;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

public class HspiUwHttpHandler {

    private HspiAppService app;

    @Inject
    public HspiUwHttpHandler(HspiAppService app) {
        this.app = app;
    }

    public void root(HttpServerExchange exchange) {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
//            exchange.getResponseSender().send("""${applicationService.message()} <br/> Total time: ${System.currentTimeMillis() - start}ms""")
//        exchange.getResponseSender().send("Humidity " + app.readHumidity());
        exchange.getResponseSender().send("Sensors " + app.readPowers());
    }
}
