package hspi;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

public class UndertowHttpHandler {

    private final static Gson GSON;

    static {
        GSON = new GsonBuilder()
            .setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .create();
    }

    private HspiService app;

    @Inject
    public UndertowHttpHandler(HspiService app) {
        this.app = app;
    }


    public void root(HttpServerExchange exchange) {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
//            exchange.getResponseSender().send("""${applicationService.message()} <br/> Total time: ${System.currentTimeMillis() - start}ms""")
//        exchange.getResponseSender().send("Humidity " + app.readHumidity());
        exchange.getResponseSender().send("Sensors " + app.readPowers());
    }

    public void state(HttpServerExchange exchange) {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "applictaion/json");
        exchange.getResponseSender().send(GSON.toJson(this.app.getState()));
    }
}
