package com.java.boilerplate.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "bootstrap.admin.enabled=true",
        "bootstrap.admin.email=admin-api-versionada@example.com",
        "bootstrap.admin.password=senha-admin-versionada-segura",
        "bootstrap.admin.name=ADMIN API VERSIONADA"
})
class CVersionedApiAuthenticationTests {
    @LocalServerPort
    private int port;

    @Test
    void loginVersionadoDeveAutenticarSemEnfraquecerOHealthPublico() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> csrfResponse = client.send(
                HttpRequest.newBuilder(URI.create(url("/api/v1/auth/csrf"))).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(csrfResponse.statusCode()).withFailMessage(csrfResponse.body()).isEqualTo(200);
        String csrfCookie = csrfResponse.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
        String csrfToken = csrfCookie.split("=", 2)[1];

        HttpResponse<String> loginResponse = client.send(
                HttpRequest.newBuilder(URI.create(url("/api/v1/auth/token/login")))
                        .header("Cookie", csrfCookie)
                        .header("Content-Type", "application/json")
                        .header("X-XSRF-TOKEN", csrfToken)
                        .POST(HttpRequest.BodyPublishers.ofString("""
                                {"identificacaoAcesso":"admin-api-versionada@example.com","senha":"senha-admin-versionada-segura"}
                                """))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(loginResponse.statusCode()).withFailMessage(loginResponse.body()).isEqualTo(200);
        assertThat(loginResponse.body()).contains("\"tokenJWT\":");

        HttpResponse<String> healthResponse = client.send(
                HttpRequest.newBuilder(URI.create(url("/api/v1/actuator/health-check/public"))).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(healthResponse.statusCode()).isEqualTo(200);
    }

    private String url(String pPath) {
        return "http://localhost:" + port + pPath;
    }
}
