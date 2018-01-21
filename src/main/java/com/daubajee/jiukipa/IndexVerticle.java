package com.daubajee.jiukipa;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.beust.jcommander.internal.Lists;
import com.daubajee.jiukipa.batch.ImageSize;
import com.daubajee.jiukipa.model.ImageMeta;
import com.daubajee.jiukipa.model.ImageMetaIndex;
import com.google.common.collect.Sets;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.TimeoutStream;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class IndexVerticle extends AbstractVerticle {

    private ImageMetaIndex index;

    final Predicate<? super ImageMeta> alwaysTrue = img -> true;

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexVerticle.class);

    @Override
    public void start() throws Exception {

        EventBus eventBus = vertx.eventBus();
        index = new ImageMetaIndex();

        
        eventBus.consumer(EventTopics.NEW_IMAGE_META, message -> onNewImage(message));

        eventBus.consumer(EventTopics.REPLAY_IMAGE_META,
                message -> onNewImage(message));

        eventBus.consumer(EventTopics.IMAGE_GET_SIZE,
                message -> onGetSize(message));

        TimeoutStream periodicStream = vertx.periodicStream(1000);
        periodicStream.handler(tick -> {
            if (index.getImages().isEmpty()) {
                LOGGER.info("ImageMeta index is empty, sending REQUEST_REPLAY_IMAGE_META");
                eventBus.publish(EventTopics.REQUEST_REPLAY_IMAGE_META, "IMAGE_META_INDEX");
            } else {
                periodicStream.cancel();
            }
        });

        eventBus.consumer(EventTopics.EVENT_STREAM, this::onEvent);
        
        eventBus.consumer(EventTopics.GET_IMAGE_META, this::onGetImage);

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
        ImageMeta meta = index.get(hashcode);
        Optional<ImageSize> imageSize = Optional.ofNullable(meta)
                .map(m -> new ImageSize(m.getWidth(), m.getHeight()));

        message.reply(imageSize);
    }

    private void onNewImage(Message<Object> message) {
        JsonObject metaInfJson = (JsonObject) message.body();
        ImageMeta imageMeta = createImageMeta(metaInfJson);
        index.put(imageMeta.getHash(), imageMeta);
    }

    public void transformMetadata(String hash, JsonObject metadata) {
        JsonObject copy = metadata.copy();
        copy.remove("hash");
        copy.remove("width");
        copy.remove("height");
        ImageMeta imageMeta = index.get(hash);
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

    private void onGetImage(Message<JsonObject> handler) {
        JsonObject params = handler.body();

        Predicate<? super ImageMeta> datePredicate;
        if (!params.isEmpty()) {
            
            Optional<Date> beginDate = Optional.ofNullable(params.getLong("beginDate")).map(ts -> new Date(ts));
            Optional<Date> endDate = Optional.ofNullable(params.getLong("endDate")).map(ts -> new Date(ts));
            
            datePredicate = beginDate
                    .flatMap(begin -> {
                        return endDate.flatMap(end -> {
                            Predicate<? super ImageMeta> filterPredicate = img -> {
                                return img.getDateCreation().getTime() >= begin.getTime() && 
                                        img.getDateCreation().getTime() <= end.getTime();
                            };
                            Optional<Predicate<? super ImageMeta>> of = Optional.of(filterPredicate);
                            return of;
                        });
                    })
                    .orElse(alwaysTrue);
        } else {
            datePredicate = alwaysTrue;
        }
        
        List<JsonObject> filtered = index.getImages()
            .stream()
            .filter(datePredicate)
            .map(img -> img.toJson())
            .collect(Collectors.toList());
        
        JsonObject reply = new JsonObject().put("images",
                new JsonArray(filtered));
        
        handler.reply(reply);
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
