package com.zanchi.zanchi_backend;

import com.jayway.jsonpath.JsonPath;
import com.zanchi.zanchi_backend.config.jwt.JwtTokenProvider;
import com.zanchi.zanchi_backend.domain.member.Member;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 내 경로 저장 API: 저장 → 목록 → 상세 → 삭제, 다른 회원 경로 접근 불가 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class MyRouteApiTest {

    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired JwtTokenProvider jwt;

    String meToken, otherToken;

    @BeforeEach
    void setUp() {
        em.persist(Member.builder().name("나").loginId("route-me").password("x").build());
        em.persist(Member.builder().name("남").loginId("route-other").password("x").build());
        em.flush();
        meToken = "Bearer " + jwt.createToken("route-me");
        otherToken = "Bearer " + jwt.createToken("route-other");
    }

    @Test
    void 경로_저장_조회_삭제() throws Exception {
        String body = """
                {"title":"데이트","startName":"홍대","startLat":37.5,"startLng":126.9,"tags":["카페"],
                 "steps":[{"label":"1","id":"a","role":"cafe","name":"카페A","lat":37.5,"lng":126.9}]}
                """;
        String created = mvc.perform(post("/api/me/routes").header("Authorization", meToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("데이트"))
                .andExpect(jsonPath("$.steps", hasSize(1)))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(created, "$.id");

        mvc.perform(get("/api/me/routes").header("Authorization", meToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/me/routes/{id}", id).header("Authorization", meToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[0]").value("카페"));

        // 다른 회원은 보거나 지울 수 없다
        mvc.perform(get("/api/me/routes/{id}", id).header("Authorization", otherToken))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/me/routes/{id}", id).header("Authorization", otherToken))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/api/me/routes/{id}", id).header("Authorization", meToken))
                .andExpect(status().isOk());
        em.flush();
        mvc.perform(get("/api/me/routes/{id}", id).header("Authorization", meToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void 내_예약_목록() throws Exception {
        mvc.perform(get("/api/reservations/me").header("Authorization", meToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
