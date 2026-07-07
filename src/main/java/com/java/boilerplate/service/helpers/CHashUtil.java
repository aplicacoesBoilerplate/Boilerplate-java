package com.java.boilerplate.service.helpers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class CHashUtil {
    private CHashUtil() {
    }

    public static String gerarSha256(String pValor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(pValor.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte item : hash) {
                hex.append(String.format("%02x", item));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException pException) {
            throw new IllegalStateException("SHA-256 indisponível", pException);
        }
    }
}
