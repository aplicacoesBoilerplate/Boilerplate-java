package com.java.boilerplate.dto;

import lombok.Data;

@Data
public class DTOPushSubscription {

    private String endpoint;
    private Keys keys;

    @Data
    public static class Keys {
        private String p256dh;
        private String auth;
    }

}
