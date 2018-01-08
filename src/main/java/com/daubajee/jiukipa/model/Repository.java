package com.daubajee.jiukipa.model;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.daubajee.jiukipa.EventTopics;
import com.daubajee.jiukipa.batch.Config;
import com.google.common.base.Charsets;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class Repository {

    private List<ImageMeta> images = new ArrayList<>();

    final FileFilter filter = (File file) -> file.isDirectory()
            || file.getName().endsWith(".meta");

    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

    private EventBus eventBus;

    private Config config;

    public Repository(Config config, EventBus eventBus) {
        this.eventBus = eventBus;
        this.config = config;
    }

    public void init() {
        File root = new File(config.imageRepoHome());
        processDirectory(root);
        eventBus.consumer(EventTopics.NEW_IMAGE, message -> onNewImage(message));
    }

    private void onNewImage(Message<Object> message) {
        Path metafilePath = (Path) message.body();
        ImageMeta imageMeta = createImageMeta(metafilePath.toFile());
        images.add(imageMeta);
    }

    public List<ImageMeta> getImages() {
        return images;
    }

    private void processDirectory(File dir) {
        File[] files = dir.listFiles(filter);
        for (File file : files) {
            if (file.isFile()) {
                ImageMeta image = createImageMeta(file);
                images.add(image);
            } else {
                processDirectory(file);
            }
        }
    }

    private ImageMeta createImageMeta(File file) {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String str = new String(bytes, Charsets.UTF_8);
            JsonObject json = new JsonObject(str);
            Integer width = Integer.parseInt(json.getString("tiff:ImageWidth"));
            Integer height = Integer
                    .parseInt(json.getString("tiff:ImageLength"));
            String createDateStr = json.getString("Date/Time");
            Date creationDate = sdf.parse(createDateStr);

            String filename = file.getName();
            String hash = filename.substring(0, filename.length() - 5);
            ImageMeta image = new ImageMeta(hash, width, height,
                    creationDate);
            return image;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

}

