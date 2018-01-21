package com.daubajee.jiukipa;

import java.util.Arrays;
import java.util.List;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start() throws Exception {

        List<AbstractVerticle> verticles = Arrays.asList(
                new WebVerticle(), 
                new IndexVerticle());

        verticles.forEach(verticle -> {
            vertx.deployVerticle(verticle, result -> {
                String verticleName = verticle.getClass().getSimpleName();
                if (result.succeeded()) {
                    LOGGER.info(verticleName + " deployed");
                } else {
                    LOGGER.error(verticleName + " deployment failed");
                }
            });
        });
    }

}
