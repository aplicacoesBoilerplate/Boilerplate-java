package com.java.boilerplate.service;

public record RAtivacaoEmailEvent(String email, String nome, String token, Integer expirationMinutes) {
}
