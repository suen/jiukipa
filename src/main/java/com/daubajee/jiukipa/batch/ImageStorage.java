package com.daubajee.jiukipa.batch;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.daubajee.jiukipa.model.ImageMeta;

public class ImageStorage {

    Map<String, ImageMeta> imageHash = new ConcurrentHashMap<>();

    public void addJpegImage(byte[] image) {

    }

    public List<ImageMeta> getImageMetas() {
        return null;
    }

    public File getImageByHash(String hash) {

    }

    public File getImageByHash(String hash, int maxWidth, int maxHeight) {

    }

}
