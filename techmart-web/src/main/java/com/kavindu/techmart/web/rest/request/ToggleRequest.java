package com.kavindu.techmart.web.rest.request;

public class ToggleRequest {

    private boolean enabled;

    public ToggleRequest() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
