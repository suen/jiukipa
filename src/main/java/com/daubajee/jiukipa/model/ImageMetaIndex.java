package com.daubajee.jiukipa.model;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.daubajee.jiukipa.EventTopics;
import com.daubajee.jiukipa.batch.ImageSize;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class ImageMetaIndex {

    public static final SimpleDateFormat EXIF_DATE_FORMAT = new SimpleDateFormat(
            "yyyy:MM:dd HH:mm:ss");

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

    private static ImageMeta createImageMeta(JsonObject metaInfMap) {
        try {
            Integer width = Integer.parseInt(metaInfMap.getString("tiff:ImageWidth"));
            Integer height = Integer.parseInt(metaInfMap.getString("tiff:ImageLength"));
            String createDateStr = metaInfMap.getString("Date/Time");
            Date creationDate = EXIF_DATE_FORMAT.parse(createDateStr);
            String hash = metaInfMap.getString("hashcode");
            ImageMeta image = new ImageMeta(hash, width, height, creationDate);
            return image;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

}

