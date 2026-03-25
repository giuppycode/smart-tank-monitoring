package esiot.backend;

import java.util.Date;
import java.util.LinkedList;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

/*
 * Data Service as a vertx event-loop 
 */
public class DataService extends AbstractVerticle {

	private int port;
	private static final int MAX_SIZE = 10;
	private LinkedList<DataPoint> values;
	private String mode = "AUTOMATIC";
	private float valvePercent = 0;
	
	public DataService(int port) {
		values = new LinkedList<>();		
		this.port = port;
	}

	@Override
	public void start() {		
		Router router = Router.router(vertx);
		router.route().handler(BodyHandler.create());
		router.post("/api/data").handler(this::handleAddNewData);
		router.get("/api/data").handler(this::handleGetData);	
		router.get("/api/status").handler(this::handleGetStatus);
		router.post("/api/status").handler(this::handleSetStatus);
		router.get("/api/valve").handler(this::handleGetValve);
		router.post("/api/valve").handler(this::handleSetValve);

		vertx
			.createHttpServer()
			.requestHandler(router)
			.listen(port);

		log("Service ready on port: " + port);
	}
	
	private void handleAddNewData(RoutingContext routingContext) {
		HttpServerResponse response = routingContext.response();
		// log("new msg "+routingContext.getBodyAsString());
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
		for (DataPoint p: values) {
			JsonObject data = new JsonObject();
			data.put("time", p.getTime());
			data.put("value", p.getValue());
			data.put("place", p.getPlace());
			arr.add(data);
		}
		routingContext.response()
			.putHeader("content-type", "application/json")
			.putHeader("Access-Control-Allow-Origin", "*")
			.end(arr.encodePrettily());
	}
	
	private void sendError(int statusCode, HttpServerResponse response) {
		response.setStatusCode(statusCode).end();
	}

	private void log(String msg) {
		System.out.println("[DATA SERVICE] "+msg);
	}

	private void handleGetStatus(RoutingContext ctx) {
    JsonObject obj = new JsonObject();
    obj.put("mode", mode);
    ctx.response()
        .putHeader("content-type", "application/json")
        .putHeader("Access-Control-Allow-Origin", "*")
        .end(obj.encodePrettily());
}

private void handleSetStatus(RoutingContext ctx) {
    JsonObject body = ctx.getBodyAsJson();
    if (body == null) { sendError(400, ctx.response()); return; }
    mode = body.getString("mode", mode);
    log("Mode changed to: " + mode);
    ctx.response().putHeader("Access-Control-Allow-Origin", "*")
       .setStatusCode(200).end();
}

private void handleGetValve(RoutingContext ctx) {
    JsonObject obj = new JsonObject();
    obj.put("percent", valvePercent);
    ctx.response()
        .putHeader("content-type", "application/json")
        .putHeader("Access-Control-Allow-Origin", "*")
        .end(obj.encodePrettily());
}

private void handleSetValve(RoutingContext ctx) {
    JsonObject body = ctx.getBodyAsJson();
    if (body == null) { sendError(400, ctx.response()); return; }
    valvePercent = body.getFloat("percent", valvePercent);
    log("Valve set to: " + valvePercent + "%");
    ctx.response().putHeader("Access-Control-Allow-Origin", "*")
       .setStatusCode(200).end();
}
}