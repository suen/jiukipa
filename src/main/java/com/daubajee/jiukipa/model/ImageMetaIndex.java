package com.daubajee.jiukipa.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ImageMetaIndex {

    private Map<String, ImageMeta> images = new HashMap<>();

    public ImageMetaIndex() {

    }

    public Collection<ImageMeta> getImages() {
        return images.values();
    }

    public ImageMeta get(String hash) {
        return images.get(hash);
    }

    public void put(String hash, ImageMeta imageMeta) {
        images.put(hash, imageMeta);
    }

}

