package com.schwab.assignment.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifies the registered Spring filter, configured property, and an actual HTTP endpoint work together. */
@SpringBootTest(properties = "app.rate-limit.requests-per-minute=2")
@AutoConfigureMockMvc
class RateLimitFilterIntegrationTest {
  @Autowired MockMvc mvc;

  @Test void applicationReturns429AfterTheConfiguredPerClientLimit() throws Exception {
    String client="198.51.100.42";
    mvc.perform(get("/actuator/health").with(request->{request.setRemoteAddr(client);return request;})).andExpect(status().isOk());
    mvc.perform(get("/actuator/health").with(request->{request.setRemoteAddr(client);return request;})).andExpect(status().isOk());
    mvc.perform(get("/actuator/health").with(request->{request.setRemoteAddr(client);return request;}))
        .andExpect(status().isTooManyRequests())
        .andExpect(content().json("{\"message\":\"rate limit exceeded\"}"));
  }
}
