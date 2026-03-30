package esiot.backend;

import java.util.Date;
import java.util.LinkedList;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.core.eventbus.EventBus;

/*
 * Data Service as a vertx event-loop
 */
public class DataService extends AbstractVerticle {

	private int port;
	private static final int MAX_SIZE = 10;
	private static final long TIMEOUT_MS = 3000;
	private LinkedList<DataPoint> values;
	private String mode = "AUTOMATIC";
	private float valvePercent = 0;
	private long lastReceived = System.currentTimeMillis();
	private EventBus eventBus;
	
	public DataService(int port) {
		values = new LinkedList<>();		
		this.port = port;
	}

	@Override
	public void start() {		
		eventBus = vertx.eventBus();
		
		Router router = Router.router(vertx);
		router.route().handler(BodyHandler.create());
		router.post("/api/data").handler(this::handleAddNewData);
		router.get("/api/data").handler(this::handleGetData);	
		router.get("/api/status").handler(this::handleGetStatus);
		router.post("/api/status").handler(this::handleSetStatus);
		router.get("/api/valve").handler(this::handleGetValve);
		router.post("/api/valve").handler(this::handleSetValve);

    public DataService(int port) {
        values = new LinkedList<>();
        this.port = port;
    }

private void handleSetStatus(RoutingContext ctx) {
    JsonObject body = ctx.getBodyAsJson();
    if (body == null) { sendError(400, ctx.response()); return; }
    String newMode = body.getString("mode", mode);
    if (!newMode.equals(mode)) {
        mode = newMode;
        JsonObject msg = new JsonObject();
        msg.put("type", "mode");
        msg.put("value", mode);
        eventBus.send("serial.commands", msg);
    }
    log("Mode changed to: " + mode);
    ctx.response().putHeader("Access-Control-Allow-Origin", "*")
       .setStatusCode(200).end();
}

        // ------------------------------------------------------------------ //
        //  FIX 1: CORS handler – must be registered BEFORE BodyHandler and
        //  all routes so that OPTIONS preflight requests are answered with
        //  200 and the right headers instead of falling through to a 404.
        //  The browser sends a preflight before every POST/PUT with a
        //  Content-Type: application/json header, so without this every
        //  write from the frontend (setMode, sendValve) silently fails.
        // ------------------------------------------------------------------ //
        CorsHandler corsHandler = CorsHandler
                .create("*")
                .allowedMethod(HttpMethod.GET)
                .allowedMethod(HttpMethod.POST)
                .allowedMethod(HttpMethod.OPTIONS)
                .allowedHeader("Content-Type")
                .allowedHeader("Accept");

private void handleSetValve(RoutingContext ctx) {
    JsonObject body = ctx.getBodyAsJson();
    if (body == null) { sendError(400, ctx.response()); return; }
    valvePercent = body.getFloat("percent", valvePercent);
    log("Valve set to: " + valvePercent + "%");
    
    JsonObject msg = new JsonObject();
    msg.put("type", "valve");
    msg.put("value", (int) valvePercent);
    eventBus.send("serial.commands", msg);
    
    ctx.response().putHeader("Access-Control-Allow-Origin", "*")
       .setStatusCode(200).end();
}
}