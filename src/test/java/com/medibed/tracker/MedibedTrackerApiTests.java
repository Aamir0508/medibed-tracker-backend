package com.medibed.tracker;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class MedibedTrackerApiTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    private String loginAsSuperAdmin() throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"superadmin\",\"password\":\"Admin@12345\"}"))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = mapper.readTree(res.getResponse().getContentAsString());
        return body.get("token").asText();
    }

    @Test
    void listsAllSeededHospitalsWithoutLogin() throws Exception {
        mvc.perform(get("/api/hospitals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(28)))
                .andExpect(jsonPath("$[0].beds.ICU").exists());
    }

    @Test
    void unknownHospitalReturns404InApiErrorShape() throws Exception {
        mvc.perform(get("/api/hospitals/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void bedUpdateRequiresLogin() throws Exception {
        mvc.perform(patch("/api/hospitals/1/beds").contentType(MediaType.APPLICATION_JSON)
                .content("{\"generalBeds\":1,\"icuBeds\":1,\"oxygenBeds\":1,\"nicuBeds\":1,\"emergencyBeds\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void negativeBedCountIsRejectedWithFieldError() throws Exception {
        String token = loginAsSuperAdmin();
        mvc.perform(patch("/api/hospitals/1/beds").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"generalBeds\":-5,\"icuBeds\":1,\"oxygenBeds\":1,\"nicuBeds\":1,\"emergencyBeds\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.generalBeds").exists());
    }

    @Test
    void adminCanPushLiveBedUpdate() throws Exception {
        String token = loginAsSuperAdmin();
        mvc.perform(patch("/api/hospitals/2/beds").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"generalBeds\":7,\"icuBeds\":3,\"oxygenBeds\":2,\"nicuBeds\":1,\"emergencyBeds\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.beds.General").value(7))
                .andExpect(jsonPath("$.beds.ICU").value(3));
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"superadmin\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized());
    }
}
