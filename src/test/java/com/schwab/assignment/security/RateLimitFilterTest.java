package com.schwab.assignment.security;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RateLimitFilterTest {
  @Controller static class PingController {
    final AtomicInteger calls=new AtomicInteger();
    @GetMapping("/ping") @ResponseBody String ping(){calls.incrementAndGet();return "ok";}
  }
  @Test void returnsTooManyRequestsAfterConfiguredLimit() throws Exception {
    MockMvc mvc=MockMvcBuilders.standaloneSetup(new PingController()).addFilters(new RateLimitFilter(1)).build();
    mvc.perform(get("/ping")).andExpect(status().isOk());
    mvc.perform(get("/ping")).andExpect(status().isTooManyRequests())
        .andExpect(content().contentType("application/json"))
        .andExpect(content().json("{\"message\":\"rate limit exceeded\"}"));
  }
  @Test void blockedRequestDoesNotReachTheController() throws Exception {
    PingController controller=new PingController();
    MockMvc mvc=MockMvcBuilders.standaloneSetup(controller).addFilters(new RateLimitFilter(1)).build();
    mvc.perform(get("/ping")).andExpect(status().isOk());
    mvc.perform(get("/ping")).andExpect(status().isTooManyRequests());
    assertEquals(1,controller.calls.get());
  }
  @Test void separateClientsReceiveSeparateBuckets() throws Exception {
    MockMvc mvc=MockMvcBuilders.standaloneSetup(new PingController()).addFilters(new RateLimitFilter(1)).build();
    mvc.perform(get("/ping").with(request->{request.setRemoteAddr("198.51.100.10");return request;})).andExpect(status().isOk());
    mvc.perform(get("/ping").with(request->{request.setRemoteAddr("198.51.100.11");return request;})).andExpect(status().isOk());
    mvc.perform(get("/ping").with(request->{request.setRemoteAddr("198.51.100.10");return request;})).andExpect(status().isTooManyRequests());
  }
}
