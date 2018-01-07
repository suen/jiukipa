package com.daubajee.jiukipa.model;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.json.JsonObject;

public class Album {
	
	private AlbumListing listing;
	
	private List<ImageMeta> photos = new ArrayList<>();

	public AlbumListing getListing() {
		return listing;
	}

	public void setListing(AlbumListing listing) {
		this.listing = listing;
	}

	public List<ImageMeta> getPhotos() {
		return photos;
	}

	public void setPhotos(List<ImageMeta> photos) {
		this.photos = photos;
	}

	public JsonObject toJson() {
		return new JsonObject()
			.put("listing", listing.toJson())
			.put("photos", photos.stream().map(ImageMeta::toJson));
	}
	
}
