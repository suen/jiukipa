package com.daubajee.jiukipa.model;

import java.util.Date;

import io.vertx.core.json.JsonObject;

public class AlbumListing {

	private long id;
	
	private String name;
	
	private String description;
	
	private Date dateCreation;
	
	private String thumbnailUrl;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Date getDateCreation() {
		return dateCreation;
	}

	public void setDateCreation(Date dateCreation) {
		this.dateCreation = dateCreation;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}
	
	public JsonObject toJson() {
		return new JsonObject()
				.put("id", id)
				.put("name", name)
				.put("description", description)
				.put("dateCreation", dateCreation)
				.put("thumbnailUrl", thumbnailUrl);
	}
	
}
