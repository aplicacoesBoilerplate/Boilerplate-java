package com.java.boilerplate.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @description Configuração responsável por complementar a documentação OpenAPI gerada pelo Springdoc.
 */
@Configuration
public class COpenApiConfiguration {
    private static final String CONTENT_TYPE_ACTUATOR = "application/vnd.spring-boot.actuator.v3+json";

    /**
     * @description Documenta o health check nativo do Actuator, que não é registrado como controller MVC.
     * @returns Customizador da documentação OpenAPI.
     */
    @Bean
    public OpenApiCustomizer healthCheckOpenApiCustomizer() {
        return pOpenApi -> pOpenApi.path("/actuator/health-check", new PathItem().get(new Operation()
                .tags(List.of("Actuator"))
                .summary("Consultar health check")
                .description("Retorna o status operacional da aplicação exposto pelo Spring Actuator.")
                .responses(new ApiResponses()
                        .addApiResponse("200", criarResposta("Aplicação operacional"))
                        .addApiResponse("503", criarResposta("Aplicação indisponível"))
                )));
    }

    private ApiResponse criarResposta(String pDescricao) {
        return new ApiResponse()
                .description(pDescricao)
                .content(new Content().addMediaType(
                        CONTENT_TYPE_ACTUATOR,
                        new MediaType().schema(criarSchemaHealthCheck())
                ));
    }

    private Schema<?> criarSchemaHealthCheck() {
        ObjectSchema schema = new ObjectSchema();
        schema.addProperty("status", new StringSchema().example("UP"));
        schema.addProperty("groups", new ArraySchema().items(new StringSchema()).example(List.of("liveness", "readiness")));
        return schema;
    }
}
