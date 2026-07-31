package com.java.boilerplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class CBoilerplateApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void contextLoads() {
	}

	@Test
	void atualizaPerfilERenovaTokenQuandoEmailMuda() throws Exception {
		String token = autenticarUsuarioPadrao();
		String emailAtualizado = "perfil.atualizado@boilerplate.local";

		String resposta = mockMvc.perform(put("/auth/me")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "nome": "Perfil Atualizado",
							  "email": "%s",
							  "avatar": "https://example.com/avatar.png",
							  "telefone": "11999999999",
							  "notificar": true
							}
							""".formatted(emailAtualizado)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.usuario.email").value(emailAtualizado))
				.andExpect(jsonPath("$.usuario.papel").value("ADMIN"))
				.andExpect(jsonPath("$.tokenJWT").isNotEmpty())
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode respostaAtualizacao = objectMapper.readTree(resposta);
		String tokenRenovado = respostaAtualizacao.path("tokenJWT").asText();

		mockMvc.perform(get("/auth/me")
					.header("Authorization", "Bearer " + tokenRenovado))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(emailAtualizado))
				.andExpect(jsonPath("$.papel").value("ADMIN"));
	}

	private String autenticarUsuarioPadrao() throws Exception {
		String resposta = mockMvc.perform(post("/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "email": "boilerplate@gmail.com",
							  "password": "Boilerplate@123"
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenJWT").isNotEmpty())
				.andReturn()
				.getResponse()
				.getContentAsString();

		return objectMapper.readTree(resposta).path("tokenJWT").asText();
	}

}
