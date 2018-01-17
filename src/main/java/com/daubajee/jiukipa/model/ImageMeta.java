package com.daubajee.jiukipa.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ImageMeta {
	
	private final String hash;
	
	private final int width;
	
	private final int height;
	
	private final Date dateCreation;
	
    private JsonObject metaJson;

    public static final SimpleDateFormat EXIF_DATE_FORMAT = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

    public ImageMeta(String hash, int width, int height, Date dateCreation, JsonObject metaJson) {
		this.hash = hash;
		this.width = width;
		this.height = height;
		this.dateCreation = dateCreation;
        this.metaJson = metaJson;
	}

    public ImageMeta(JsonObject metaInfMap) {
        this.width = Integer.parseInt(metaInfMap.getString("tiff:ImageWidth"));
        this.height = Integer.parseInt(metaInfMap.getString("tiff:ImageLength"));
        String createDateStr = metaInfMap.getString("Date/Time");
        try {
            this.dateCreation = EXIF_DATE_FORMAT.parse(createDateStr);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
        this.hash = metaInfMap.getString("hashcode");
        this.metaJson = metaInfMap;
    }

    public String getHash() {
		return hash;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public Date getDateCreation() {
		return dateCreation;
	}

    public JsonObject getMetaJson() {
        return metaJson;
    }

    public void setMetaJson(JsonObject metaJson) {
        this.metaJson = metaJson;
    }

    public List<String> getTags() {
        JsonArray jsonArray = metaJson.getJsonArray("tags", new JsonArray());
        return jsonArray.getList();
    }

    public JsonObject toJson() {
		return new JsonObject()
				.put("hash", hash)
				.put("width", width)
				.put("height", height)
                .put("tags", new JsonArray(getTags()))
                .put("dateCreation", EXIF_DATE_FORMAT.format(dateCreation));
	}
	
}
