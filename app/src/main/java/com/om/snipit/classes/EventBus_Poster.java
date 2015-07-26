package com.om.snipit.classes;

public class EventBus_Poster {
    private String message;
    private String extra;

    public EventBus_Poster(String message) {
        this.message = message;
    }

    public EventBus_Poster(String message, String extra) {
        this.message = message;
        this.extra = extra;
    }

    public String getMessage() {
        return message;
    }

    public String getExtra() {
        return extra;
    }
}
