package com.daubajee.jiukipa;

import io.vertx.core.json.JsonObject;

public class Event {

    private String type;

    private long timestamp;

    private JsonObject data;

    public static final String ADD_METADATA = "ADD_METADATA";

    public Event(String type, long timestamp, JsonObject data) {
        this.type = type;
        this.timestamp = timestamp;
        this.data = data;
    }

    public Event(String type, JsonObject data) {
        this.timestamp = System.currentTimeMillis();
        this.type = type;
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public JsonObject getData() {
        return data;
    }

    public void setData(JsonObject data) {
        this.data = data;
    }

    public JsonObject toJson(){
        return new JsonObject()
                .put("type", type)
                .put("timestamp", timestamp)
                .put("data", data);
    }
    
    public static Event fromJson(JsonObject json){
        return new Event(json.getString("type"), json.getLong("timestamp"), json.getJsonObject("data"));
    }
    
}
