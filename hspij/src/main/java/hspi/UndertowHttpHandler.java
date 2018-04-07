package hspi;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.IOException;

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


    public void root(HttpServerExchange exch) {
        exch.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
        exch.getResponseSender().send("Success");
    }

    public void state(HttpServerExchange exch) {
        exch.getResponseHeaders().put(Headers.CONTENT_TYPE, "applictaion/json");
        exch.getResponseSender().send(GSON.toJson(this.app.getState()));
    }

    public void limitsUpdate(HttpServerExchange exchange) throws IOException {
        exchange.getRequestReceiver().receiveFullString(
            (exch, body) -> {
                HumidLimits hl = GSON.fromJson(body, HumidLimits.class);
                app.updateHumidityLimits(hl.ok, hl.high);

            });
    }

    public void limits(HttpServerExchange exch) {
        exch.getResponseHeaders().put(Headers.CONTENT_TYPE, "applictaion/json");
        exch.getResponseSender().send(GSON.toJson(new HumidLimits(app.getHumidityOk(), app.getHumidityHigh())));
    }

    public static class HumidLimits {

        private int high;

        private int ok;

        public HumidLimits() {
        }

        public HumidLimits(int ok, int high) {
            this.ok = ok;
            this.high = high;
        }

        public int getHigh() {
            return high;
        }

        public void setHigh(int high) {
            this.high = high;
        }

        public int getOk() {
            return ok;
        }

        public void setOk(int ok) {
            this.ok = ok;
        }
    }
}
