package com.kavindu.techmart.web.rest.request;

public class BroadcastRequest {

    private String title;
    private String message;

    public BroadcastRequest() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
