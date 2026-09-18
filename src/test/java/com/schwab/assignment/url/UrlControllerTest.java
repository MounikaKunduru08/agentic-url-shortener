package com.schwab.assignment.url;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UrlControllerTest {
    private final UrlService service = Mockito.mock(UrlService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new UrlController(service)).setControllerAdvice(new com.schwab.assignment.ApiExceptionHandler()).build();

    @Test
    void unknownCodeReturnsNotFoundJson() throws Exception {
        Mockito.when(service.analytics("missing")).thenThrow(new UrlService.UrlNotFoundException("missing"));
        mvc.perform(get("/api/urls/missing/analytics")).andExpect(status().isNotFound()).andExpect(jsonPath("$.message").exists());
    }

    @Test
    void malformedRequestReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON).content("not-json")).andExpect(status().isBadRequest());
    }
}
