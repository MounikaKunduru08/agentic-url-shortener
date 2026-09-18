package com.schwab.assignment.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasLength;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc
class ApiIntegrationTest {
  @Autowired MockMvc mvc; @Autowired ObjectMapper mapper;
  @Test void urlWorkflowAndApprovalWorkEndToEnd() throws Exception {
    MvcResult url=mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON).content("{\"destination\":\"https://example.com/products\"}"))
      .andExpect(status().isCreated()).andExpect(jsonPath("$.code",hasLength(8))).andReturn();
    String code=mapper.readTree(url.getResponse().getContentAsString()).get("code").asText();
    mvc.perform(get("/"+code)).andExpect(status().isFound());
    MvcResult workflow=mvc.perform(post("/api/workflows").contentType(MediaType.APPLICATION_JSON).content("{\"scenario\":\"greenfield\",\"requirement\":\"Build URL shortener\"}"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("AWAITING_APPROVAL")).andReturn();
    UUID id=UUID.fromString(mapper.readTree(workflow.getResponse().getContentAsString()).get("id").asText());
    mvc.perform(post("/api/workflows/{id}/approve",id).contentType(MediaType.APPLICATION_JSON).content("{\"approver\":\"release-manager\"}"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
  }

  @Test void ambiguousRequirementCanBeClarifiedAndReplanned() throws Exception {
    MvcResult created=mvc.perform(post("/api/workflows").contentType(MediaType.APPLICATION_JSON)
        .content("{\"scenario\":\"ambiguous\",\"requirement\":\"Make links expire\"}"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.planVersion").value(1)).andReturn();
    UUID id=UUID.fromString(mapper.readTree(created.getResponse().getContentAsString()).get("id").asText());
    mvc.perform(get("/api/workflows/{id}",id)).andExpect(status().isOk()).andExpect(jsonPath("$.scenario").value("ambiguous"));
    mvc.perform(post("/api/workflows/{id}/replan",id).contentType(MediaType.APPLICATION_JSON)
        .content("{\"changedRequirement\":\"Links expire after 30 days\"}"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.planVersion").value(2))
      .andExpect(jsonPath("$.requirement").value("Links expire after 30 days"))
      .andExpect(jsonPath("$.audit[*].action",hasItem("REPLANNED")))
      .andExpect(jsonPath("$.status").value("AWAITING_APPROVAL"))
      .andExpect(jsonPath("$.stages.release").value("AWAITING_APPROVAL"));
    mvc.perform(post("/api/workflows/{id}/approve",id).contentType(MediaType.APPLICATION_JSON).content("{\"approver\":\"release-manager\"}"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.planVersion").value(2)).andExpect(jsonPath("$.status").value("COMPLETED"));
  }
}
