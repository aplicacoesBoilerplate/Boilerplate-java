package com.java.boilerplate.service.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.java.boilerplate.config.TokensProperties;
import com.java.boilerplate.dto.infinitepay.DTOInfinitePayAddressLinkRequest;
import com.java.boilerplate.dto.users.DTOLocation;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GoogleServices {
    private final TokensProperties properties;

    public GoogleServices(TokensProperties properties) {
        this.properties = properties;
    }

    public DTOInfinitePayAddressLinkRequest fetchAddressFromGoogle(DTOLocation userLocation, RestTemplate restTemplate) {
        String latitude = String.valueOf(userLocation.latitude());
        String longitude = String.valueOf(userLocation.longitude());

        try {
            String googleApiKey = properties.getGoogleApiKey();
            String url = String.format("https://maps.googleapis.com/maps/api/geocode/json?latlng=%s,%s&key=%s",
                    latitude, longitude, googleApiKey);

            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
            JsonNode root = response.getBody();

            if (root != null && "OK".equals(root.path("status").asText())) {
                JsonNode addressComponents = root.path("results").get(0).path("address_components");
                String route = "", streetNumber = "", neighborhood = "", zip = "";

                for (JsonNode component : addressComponents) {
                    String type = component.path("types").get(0).asText();
                    String longName = component.path("long_name").asText();

                    switch (type) {
                        case "route": route = longName; break;
                        case "street_number": streetNumber = longName; break;
                        case "sublocality_level_1": neighborhood = longName; break;
                        case "postal_code": zip = longName; break;
                    }
                }

                return new DTOInfinitePayAddressLinkRequest(
                        zip.replace("-", ""),
                        route + ", " + streetNumber,
                        neighborhood,
                        "1000",
                        "Complement"
                );
            }
        } catch (Exception e) {
            System.err.println("Aviso: Falha ao obter endereço do Google: " + e.getMessage());
        }

        return null;
    }
}
