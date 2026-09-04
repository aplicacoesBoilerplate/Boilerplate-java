package com.java.boilerplate.service.helpers;

import com.java.boilerplate.config.RAtivacaoProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class CAtivacaoTokenService {
    private static final String HMAC_SHA_256 = "HmacSHA256";
    private final RAtivacaoProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public CAtivacaoTokenService(RAtivacaoProperties pProperties) {
        this.properties = pProperties;
    }

    public String gerarToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String gerarHash(String pToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(properties.pepper().getBytes(StandardCharsets.UTF_8), HMAC_SHA_256));
            return java.util.HexFormat.of().formatHex(mac.doFinal(pToken.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException pException) {
            throw new IllegalStateException("HMAC de ativação indisponível", pException);
        }
    }
}
