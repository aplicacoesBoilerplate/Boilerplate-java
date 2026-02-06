package com.java.boilerplate.dto.auth;

import com.java.boilerplate.dto.users.DTOUser;

public record DTOLoginResponse(String token, DTOUser user) {}
