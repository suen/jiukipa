package com.daubajee.jiukipa;

import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Optional;

import org.apache.http.HttpStatus;

import com.daubajee.jiukipa.batch.Config;
import com.daubajee.jiukipa.image.ImageAlreadyExistsException;
import com.daubajee.jiukipa.image.ImageStorage;
import com.google.common.base.Throwables;
import com.google.common.hash.HashCode;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class WebVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebVerticle.class);

    final SimpleDateFormat requestDateformat = new SimpleDateFormat("yyyy-MM-dd");

    private ImageStorage imageStorage;

    private Config config;

    StaticHandler staticHandler;

    private EventBus eventBus;

    @Override
    public void start() throws Exception {
        config = new Config();
        imageStorage = new ImageStorage(config, vertx);
        eventBus = vertx.eventBus();
        HttpServer httpServer = vertx.createHttpServer();
		
        Router router = Router.router(vertx);
        
        staticHandler = StaticHandler.create();
        staticHandler.setWebRoot(config.imageRepoHome());

        router.route().handler(BodyHandler.create());

        router.route().method(HttpMethod.GET)
                .path("/images").handler(context -> handleGetImages(context));

        router.route().method(HttpMethod.GET)
                .path("/image/:imageHash/size/:width/:height")
                .handler(context -> handleGetImageByHash(context));
        router.route().method(HttpMethod.GET).path("/image/:imageHash/stdsize/:sizeName")
                .handler(context -> handleGetImageByHashStdSize(context));
        
        router.route().method(HttpMethod.POST).path("/image/:imageHash/addMetadata").handler(this::handlePostMetaData);

        router.route().method(HttpMethod.GET).path("/static/*")
                .handler(staticHandler);

        router.route().method(HttpMethod.POST).path("/image")
                .handler(context -> handlePostImage(context));

        httpServer.requestHandler(router::accept);
        httpServer.listen(8080);
        LOGGER.info("HTTP server started on port 8080");
    }


    private void handlePostImage(RoutingContext context) {
        Buffer body = context.getBody();
        byte[] jpegImageBytes = body.getBytes();

        HttpServerResponse response = context.response();
        response.setChunked(true);
        response.putHeader("Content-type", "text/plain");

        try {
            imageStorage.addNewImage(jpegImageBytes);
        } catch (ImageAlreadyExistsException e) {
            response.setStatusCode(200);
            response.write("Resource exists\n");
            response.close();
            return;
        }
        catch (Exception e) {
            response.setStatusCode(500);
            response.write(Throwables.getStackTraceAsString(e));
            response.close();
            return;
        }
        response.setStatusCode(201);
        response.write("Resource accepted\n");
        response.close();
    }

    private void handleGetImages(RoutingContext context) {
        HttpServerResponse response = context.response();
        
        String beginDateStr = context.request().getParam("beginDate");
        String endDateStr = context.request().getParam("endDate");

        try {
            JsonObject dateParam = Optional.ofNullable(beginDateStr)
                .flatMap(bdateStr -> Optional
                        .ofNullable(endDateStr)
                        .map(eDateStr -> {
                            try {
                                long beginDate = requestDateformat.parse(bdateStr).getTime();
                                long endDate = requestDateformat.parse(eDateStr).getTime();
                                return new JsonObject().put("beginDate", beginDate).put("endDate", endDate);
                            } catch (ParseException e) {
                                throw new IllegalArgumentException(e);
                            }
                        }))
                        .orElse(new JsonObject());

            eventBus.send(EventTopics.GET_IMAGE_META, dateParam, reply -> {
                if (reply.failed()){
                    response.setStatusCode(500);
                    response.close();
                    return;
                }
                LOGGER.info("GET_IMAGE_META response received");
                
                Message<Object> result = reply.result();
                JsonObject resultJson = (JsonObject) result.body();
                
                response.putHeader("Content-Type", "application/json");
                response.setChunked(true);
                response.write(resultJson.toString());
                response.close();
            });
            LOGGER.info("GET_IMAGE_META sent, waiting response");

        } catch (Exception e) {
            response.putHeader("Content-type", "text/plain");
            response.write(e.getMessage());
            response.close();
        }
   
    }

    private void handleGetImageByHash(RoutingContext context) {
        String reqImageHash = context.request().getParam("imageHash");
        String reqMaxWidth = context.request().getParam("width");
        String reqMaxHeight = context.request().getParam("height");

        HashCode hashCode;
        int width;
        int height;
        try {
            hashCode = HashCode.fromString(reqImageHash);
            width = Integer.parseInt(reqMaxWidth);
            height = Integer.parseInt(reqMaxHeight);
        } catch (Exception e) {
            HttpServerResponse response = context.response();
            String message = "Resource not found : " + e.getMessage();
            response.setStatusCode(404);
            response.putHeader("Content-Type", "text/plain");
            response.putHeader("Content-Length", String.valueOf(message.length()));
            response.write(message);
            response.close();
            return;
        }

        Path imagePath = imageStorage.getImage(hashCode, width, height);

        String fileName = imagePath.getFileName().toString();
        String dir = imagePath.getParent().getFileName().toString();
        
        String rerouteUri = "/static/" + dir + "/" + fileName;
        LOGGER.info("Reoute " + rerouteUri);
        context.reroute(rerouteUri);
    }

    private void handleGetImageByHashStdSize(RoutingContext context) {
        String reqImageHash = context.request().getParam("imageHash");
        String reqSizeName = context.request().getParam("sizeName");

        HashCode hashCode;
        try {
            hashCode = HashCode.fromString(reqImageHash);
        } catch (Exception e) {
            HttpServerResponse response = context.response();
            response.setChunked(true);
            response.putHeader("Content-Type", "text/plain");
            response.setStatusCode(400);
            response.write(e.getMessage());
            response.close();
            return;
        }

        Path imagePath = imageStorage.getImageByStdSize(hashCode, reqSizeName);
        String fileName = imagePath.getFileName().toString();
        String dir = imagePath.getParent().getFileName().toString();

        String rerouteUri = "/static/" + dir + "/" + fileName;
        LOGGER.info("Reoute " + rerouteUri);
        context.reroute(rerouteUri);
    }

    private void handlePostMetaData(RoutingContext context) {
        String imageHash = context.request().getParam("imageHash");

        JsonObject metaJson = context.getBodyAsJson();
        JsonObject eventdata = new JsonObject()
            .put("hash", imageHash)
            .put("metaData", metaJson);
        Event event = new Event(Event.ADD_METADATA, eventdata);
        DeliveryOptions options = new DeliveryOptions();
        eventBus.publish(EventTopics.EVENT_STREAM, event.toJson(), options);
        HttpServerResponse response = context.response();

        response.setStatusCode(HttpStatus.SC_ACCEPTED);
        response.setStatusMessage("ACCEPTED");
        response.close();
    }

}