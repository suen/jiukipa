package com.daubajee.jiukipa.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.beust.jcommander.internal.Lists;
import com.daubajee.jiukipa.Event;
import com.daubajee.jiukipa.EventTopics;
import com.daubajee.jiukipa.batch.ImageSize;
import com.google.common.collect.Sets;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ImageMetaIndex {

    private Map<String, ImageMeta> images = new HashMap<>();

    private EventBus eventBus;

    public ImageMetaIndex(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void init() {
        eventBus.consumer(EventTopics.NEW_IMAGE_META, message -> onNewImage(message));

        eventBus.consumer(EventTopics.REPLAY_IMAGE_META,
                message -> onNewImage(message));

        eventBus.consumer(EventTopics.IMAGE_GET_SIZE,
                message -> onGetSize(message));

        if (images.isEmpty()) {
            eventBus.send(EventTopics.REQUEST_REPLAY_IMAGE_META,
                    "IMAGE_META_INDEX");
        }

        eventBus.consumer(EventTopics.EVENT_STREAM, this::onEvent);

    }

    private void onEvent(Message<JsonObject> message) {
        JsonObject json = message.body();
        if (!json.getString("type", "").equals(Event.ADD_METADATA)) {
            return;
        }

        Event event = Event.fromJson(json);
        JsonObject eventData = event.getData();
        String hash = eventData.getString("hash", "");
        JsonObject metadata = eventData.getJsonObject("metaData", new JsonObject());
        if (hash.isEmpty() || metadata.isEmpty()) {
            return;
        }

        transformMetadata(hash, metadata);
    }

    private void onGetSize(Message<Object> message) {
        String hashcode = (String) message.body();
        ImageMeta meta = images.get(hashcode);
        Optional<ImageSize> imageSize = Optional.ofNullable(meta)
                .map(m -> new ImageSize(m.getWidth(), m.getHeight()));

        message.reply(imageSize);
    }

    private void onNewImage(Message<Object> message) {
        JsonObject metaInfJson = (JsonObject) message.body();
        ImageMeta imageMeta = createImageMeta(metaInfJson);
        images.put(imageMeta.getHash(), imageMeta);
    }

    public Collection<ImageMeta> getImages() {
        return images.values();
    }

    public void transformMetadata(String hash, JsonObject metadata) {
        JsonObject copy = metadata.copy();
        copy.remove("hash");
        copy.remove("width");
        copy.remove("height");
        ImageMeta imageMeta = images.get(hash);
        if (imageMeta == null) {
            return;
        }

        JsonObject metaJson = imageMeta.getMetaJson();
        Map<String, Object> map = metadata.getMap();
        map.entrySet().forEach(kv -> {
            Object value = kv.getValue();
            String key = kv.getKey();
            if (value instanceof JsonArray) {
                JsonArray existing = metaJson.getJsonArray(key, new JsonArray());
                Set newTagSet = Sets.newHashSet(existing.getList());
                newTagSet.addAll(((JsonArray) value).getList());
                metaJson.put(key, new JsonArray(Lists.newArrayList(newTagSet)));
            } else if (value instanceof JsonObject) {
                System.out.println("No implement !!");
            } else {
                metaJson.put(key, value);
            }
        });

    }

    private static ImageMeta createImageMeta(JsonObject metaInfMap) {
        try {
            ImageMeta image = new ImageMeta(metaInfMap);
            return image;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

}

