package com.schwab.assignment.shortener;

import com.schwab.assignment.shortener.UrlShortenerController;
import com.schwab.assignment.shortener.UrlShortenerService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UrlControllerTest {
    private final UrlShortenerService service = Mockito.mock(UrlShortenerService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new UrlShortenerController(service)).setControllerAdvice(new com.schwab.assignment.ApiExceptionHandler()).build();

    @Test
    void unknownCodeReturnsNotFoundJson() throws Exception {
        Mockito.when(service.analytics("missing")).thenThrow(new UrlShortenerService.UrlNotFoundException("missing"));
        mvc.perform(get("/api/urls/missing/analytics")).andExpect(status().isNotFound()).andExpect(jsonPath("$.message").exists());
    }

    @Test
    void malformedRequestReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON).content("not-json")).andExpect(status().isBadRequest());
    }
}
