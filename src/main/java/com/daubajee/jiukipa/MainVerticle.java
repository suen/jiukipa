package com.daubajee.jiukipa;

import java.nio.file.Path;
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
import com.google.common.hash.HashCode;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class MainVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

    final SimpleDateFormat requestDateformat = new SimpleDateFormat("yyyy-MM-dd");

    private ImageMetaIndex repository;

    private ImageStorage imageStorage;

    private Config config;

    StaticHandler staticHandler;


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
        
        staticHandler = StaticHandler.create();
        staticHandler.setWebRoot(config.imageRepoHome());

        router.route().handler(BodyHandler.create());

        router.route().method(HttpMethod.GET)
                .path("/images").handler(context -> handleGetImages(context));

        router.route().method(HttpMethod.GET)
                .path("/image/:imageHash/:width/:height")
                .handler(context -> handleGetImageByHash(context));
        
        router.route().method(HttpMethod.GET).path("/static/*")
                .handler(staticHandler);

        router.route().method(HttpMethod.POST).path("/image")
                .handler(context -> handlePostImage(context));

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
            response.setChunked(true);
            response.putHeader("Content-Type", "text/plain");
            response.setStatusCode(400);
            response.write(e.getMessage());
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

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle(vertx));
    }
}