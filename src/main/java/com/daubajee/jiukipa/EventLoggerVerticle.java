package com.daubajee.jiukipa;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class EventLoggerVerticle extends AbstractVerticle {

    private List<Event> inMemoryStore = new ArrayList<>();

    @Override
    public void start() throws Exception {
        EventBus eventBus = vertx.eventBus();

        eventBus.consumer(EventTopics.EVENT_STREAM, this::onEvent);
    }

    public void onEvent(Message<JsonObject> message) {
        JsonObject eventJson = message.body();
        Event event = Event.fromJson(eventJson);
        inMemoryStore.add(event);
    }

}
