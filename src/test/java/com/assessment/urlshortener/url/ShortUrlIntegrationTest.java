package com.assessment.urlshortener.url;

import com.assessment.urlshortener.AgenticUrlShortenerApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = AgenticUrlShortenerApplication.class)
@AutoConfigureMockMvc
class ShortUrlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void shouldCreateShortUrl() throws Exception {
        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalUrl": "https://example.com/products"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").isNotEmpty())
                .andExpect(jsonPath("$.shortUrl").isNotEmpty())
                .andExpect(jsonPath("$.originalUrl")
                        .value("https://example.com/products"));
    }

    @Test
    void shouldRejectInvalidUrl() throws Exception {
        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalUrl": "javascript:alert(1)"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid URL"));
    }

    @Test
    void shouldReturnNotFoundForUnknownShortCode()
            throws Exception {
        mockMvc.perform(get("/api/urls/unknown-code-123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRedirectAndTrackClick() throws Exception {
        String response = mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalUrl": "https://example.com/products"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = jsonMapper.readTree(response);
        String shortCode = json.get("code").asText();

        mockMvc.perform(get("/r/" + shortCode))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        "Location",
                        "https://example.com/products"));

        mockMvc.perform(get("/api/urls/" + shortCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clickCount").value(1));
    }

    @Test
    void shouldRejectExpiredShortUrl() throws Exception {
        String expiresAt = java.time.LocalDateTime.now()
                .plusSeconds(2)
                .toString();

        String response = mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "originalUrl": "https://example.com/expired",
                              "expiresAt": "%s"
                            }
                            """.formatted(expiresAt)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = jsonMapper.readTree(response);
        String code = json.get("code").asText();

        Thread.sleep(2500);

        mockMvc.perform(get("/r/" + code))
                .andExpect(status().isNotFound());
    }
}