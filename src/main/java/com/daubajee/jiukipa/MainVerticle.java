package com.daubajee.jiukipa;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.daubajee.jiukipa.batch.Config;
import com.daubajee.jiukipa.image.ImageAlreadyExistsException;
import com.daubajee.jiukipa.image.ImageStorage;
import com.daubajee.jiukipa.model.ImageMeta;
import com.daubajee.jiukipa.model.ImageMetaIndex;
import com.google.common.base.Throwables;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class MainVerticle extends AbstractVerticle {

    final SimpleDateFormat requestDateformat = new SimpleDateFormat("yyyy-MM-dd");

    private ImageMetaIndex repository;

    private ImageStorage imageStorage;

    private Config config;

    public MainVerticle(Vertx vertx) {
        config = new Config();
        EventBus eventBus = vertx.eventBus();
        imageStorage = new ImageStorage(config, vertx);
        repository = new ImageMetaIndex(eventBus);
        repository.init();
    }

    @Override
    public void start() throws Exception {
        HttpServer httpServer = vertx.createHttpServer();
		
        Router router = Router.router(vertx);
        
        router.route().handler(BodyHandler.create());

        Route imagesRoute = router.route().method(HttpMethod.GET)
                .path("/images");
        
        Route postRoute = router.route().method(HttpMethod.POST).path("/image");


        imagesRoute.handler(context -> handleGetImages(context));
        postRoute.handler(context -> handlePostImage(context));

        httpServer.requestHandler(router::accept);
        httpServer.listen(8080);
        System.out.println("HTTP server started on port 8080");
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
        String beginDateStr = context.request().getParam("beginDate");
        String endDateStr = context.request().getParam("endDate");

        Date beginDate;
        Date endDate;
        HttpServerResponse response = context.response();
        try {
            beginDate = requestDateformat.parse(beginDateStr);
            endDate = requestDateformat.parse(endDateStr);
        } catch (ParseException e) {
            response.putHeader("Content-type", "text/plain");
            response.write(e.getMessage());
            response.close();
            return;
        }

        Predicate<? super ImageMeta> filterPredicate = img -> {
            return img.getDateCreation().getTime() >= beginDate.getTime() && 
                    img.getDateCreation().getTime() <= endDate
                                .getTime();
        };

        List<JsonObject> filtered = repository.getImages().stream()
            .filter(filterPredicate)
            .map(img -> img.toJson())
            .collect(Collectors.toList());
        JsonObject reply = new JsonObject().put("images",
                new JsonArray(filtered));

        response.putHeader("Content-Type", "application/json");
        response.setChunked(true);
        response.write(reply.toString());
        response.close();
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle(vertx));
    }
}