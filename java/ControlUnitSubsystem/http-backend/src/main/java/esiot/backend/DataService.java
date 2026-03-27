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
import io.vertx.ext.web.handler.CorsHandler;

/*
 * Data Service as a vertx event-loop
 */
public class DataService extends AbstractVerticle {

    private int port;
    private static final int MAX_SIZE = 10;
    private LinkedList<DataPoint> values;

    // mode is the source of truth – UNCONNECTED logic lives in CUS, not here.
    // DataService just stores and returns whatever it is told.
    private String mode = "AUTOMATIC";
    private float valvePercent = 0;

    public DataService(int port) {
        values = new LinkedList<>();
        this.port = port;
    }

    @Override
    public void start() {
        Router router = Router.router(vertx);

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

        router.route().handler(corsHandler);
        router.route().handler(BodyHandler.create());

        router.post("/api/data").handler(this::handleAddNewData);
        router.get("/api/data").handler(this::handleGetData);
        router.get("/api/status").handler(this::handleGetStatus);
        router.post("/api/status").handler(this::handleSetStatus);
        router.get("/api/valve").handler(this::handleGetValve);
        router.post("/api/valve").handler(this::handleSetValve);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(port);

        log("Service ready on port: " + port);
    }

    private void handleAddNewData(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        JsonObject res = routingContext.getBodyAsJson();
        if (res == null) {
            sendError(400, response);
        } else {
            float value = res.getFloat("value");
            String place = res.getString("place");
            long time = System.currentTimeMillis();

            values.addFirst(new DataPoint(value, time, place));
            if (values.size() > MAX_SIZE) {
                values.removeLast();
            }

            log("New value: " + value + " from " + place + " on " + new Date(time));
            response.setStatusCode(200).end();
        }
    }

    private void handleGetData(RoutingContext routingContext) {
        JsonArray arr = new JsonArray();
        for (DataPoint p : values) {
            arr.add(new JsonObject()
                    .put("time", p.getTime())
                    .put("value", p.getValue())
                    .put("place", p.getPlace()));
        }
        routingContext.response()
                .putHeader("content-type", "application/json")
                .end(arr.encodePrettily());
    }

    // ------------------------------------------------------------------ //
    //  FIX 2: handleGetStatus NO LONGER injects UNCONNECTED based on
    //  lastReceived. That logic belongs in CUS which already monitors
    //  MQTT timeout and writes "UNCONNECTED" to /api/status when needed.
    //
    //  The old behaviour was: even if mode == "MANUAL", if MQTT was quiet
    //  for 3 seconds (totally normal during manual operation), this method
    //  would return "UNCONNECTED" to both the frontend AND to CUS's periodic
    //  timer, which would then overwrite Arduino's mode. Manual was
    //  impossible to hold for more than 3 seconds.
    // ------------------------------------------------------------------ //
    private void handleGetStatus(RoutingContext ctx) {
        ctx.response()
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("mode", mode).encodePrettily());
    }

    private void handleSetStatus(RoutingContext ctx) {
        JsonObject body = ctx.getBodyAsJson();
        if (body == null) { sendError(400, ctx.response()); return; }
        mode = body.getString("mode", mode);
        log("Mode set to: " + mode);
        ctx.response().setStatusCode(200).end();
    }

    private void handleGetValve(RoutingContext ctx) {
        ctx.response()
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("percent", valvePercent).encodePrettily());
    }

    private void handleSetValve(RoutingContext ctx) {
        JsonObject body = ctx.getBodyAsJson();
        if (body == null) { sendError(400, ctx.response()); return; }
        valvePercent = body.getFloat("percent", valvePercent);
        log("Valve set to: " + valvePercent + "%");
        ctx.response().setStatusCode(200).end();
    }

    private void sendError(int statusCode, HttpServerResponse response) {
        response.setStatusCode(statusCode).end();
    }

    private void log(String msg) {
        System.out.println("[DATA SERVICE] " + msg);
    }
}