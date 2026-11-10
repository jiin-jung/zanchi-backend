package com.zanchi.zanchi_backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 프리사인 URL은 로그인한 사용자에게만 발급되어야 한다 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class PresignSecurityTest {

    @Autowired MockMvc mvc;

    @Test
    void 비로그인_사용자는_프리사인_URL을_받을_수_없다() throws Exception {
        mvc.perform(get("/api/storage/presign").param("filename", "a.mp4").param("contentType", "video/mp4"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/s3/presign-put").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
