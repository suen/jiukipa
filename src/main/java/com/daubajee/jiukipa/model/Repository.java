package com.daubajee.jiukipa.model;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.common.base.Charsets;

import io.vertx.core.json.JsonObject;

public class Repository {

    private List<ImageMeta> images = new ArrayList<>();

    final FileFilter filter = (File file) -> file.isDirectory()
            || file.getName().endsWith(".meta");

    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

    public void init(String srcDirName) {
        File root = new File(srcDirName);
        processDirectory(root);
    }

    public List<ImageMeta> getImages() {
        return images;
    }

    private void processDirectory(File dir) {
        File[] files = dir.listFiles(filter);
        for (File file : files) {
            if (file.isFile()) {
                processMetaFile(file);
            } else {
                processDirectory(file);
            }
        }
    }

    private void processMetaFile(File file) {
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
            images.add(image);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

}

