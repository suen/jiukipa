package com.daubajee.jiukipa.model;

import java.text.SimpleDateFormat;
import java.util.Date;

import io.vertx.core.json.JsonObject;

public class ImageMeta {
	
	private final String hash;
	
	private final int width;
	
	private final int height;
	
	private final Date dateCreation;
	
    final SimpleDateFormat httpDateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public ImageMeta(String hash, int width, int height, Date dateCreation) {
		this.hash = hash;
		this.width = width;
		this.height = height;
		this.dateCreation = dateCreation;
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

	public JsonObject toJson() {
		return new JsonObject()
				.put("hash", hash)
				.put("width", width)
				.put("height", height)
                .put("dateCreation", httpDateformat.format(dateCreation));
	}
	
}
