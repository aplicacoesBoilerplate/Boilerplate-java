package com.java.boilerplate.dto;

import lombok.Data;

@Data
public class DTOPushNotification {
    private String title;
    private String body;
    private String url;
    private String contextKey;
    private String icon;
    private String badge;
}
