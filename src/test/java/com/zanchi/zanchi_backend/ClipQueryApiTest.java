package com.zanchi.zanchi_backend;

import com.zanchi.zanchi_backend.config.jwt.JwtTokenProvider;
import com.zanchi.zanchi_backend.domain.clip.Clip;
import com.zanchi.zanchi_backend.domain.clip.ClipComment;
import com.zanchi.zanchi_backend.domain.clip.ClipLike;
import com.zanchi.zanchi_backend.domain.clip.ClipSave;
import com.zanchi.zanchi_backend.domain.member.Member;
import com.zanchi.zanchi_backend.domain.member_follow.MemberFollow;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 클립 조회 API 응답 고정 테스트.
 * 컨트롤러의 리포지토리 직접 호출을 서비스로 옮겨도 응답이 그대로인지 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class ClipQueryApiTest {

    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired JwtTokenProvider jwt;

    Member me, other;
    Clip liked, saved;

    @BeforeEach
    void setUp() {
        me = persist(Member.builder().name("나").loginId("me").password("x").build());
        other = persist(Member.builder().name("상대").loginId("other").password("x").build());

        liked = persist(Clip.builder().uploader(other).caption("c1").videoUrl("v1").likeCount(1).build());
        saved = persist(Clip.builder().uploader(other).caption("c2").videoUrl("v2").build());

        persist(ClipLike.builder().clip(liked).member(me).build());
        persist(ClipSave.builder().clip(saved).member(me).build());
        persist(MemberFollow.builder().follower(me).following(other).build());

        ClipComment parent = persist(ClipComment.builder().clip(liked).author(me).content("부모1").build());
        persist(ClipComment.builder().clip(liked).author(other).content("부모2").build());
        persist(ClipComment.builder().clip(liked).author(other).content("답글1").parent(parent).build());
        persist(ClipComment.builder().clip(liked).author(other).content("답글2").parent(parent).build());

        em.flush();
        em.clear();
    }

    private <T> T persist(T e) { em.persist(e); return e; }
    private String bearer(Member m) { return "Bearer " + jwt.createToken(m.getLoginId()); }

    @Test
    void 댓글_목록은_부모_댓글만_대댓글_수와_함께_반환한다() throws Exception {
        mvc.perform(get("/api/clips/{id}/comments", liked.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].content", contains("부모1", "부모2")))
                .andExpect(jsonPath("$.content[*].replyCount", contains(2, 0)));
    }

    @Test
    void 다른_회원_클립_목록에_내_좋아요_저장_여부가_표시된다() throws Exception {
        mvc.perform(get("/api/members/{id}/clips", other.getId()).header("Authorization", bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.id == %d)].likedByMe".formatted(liked.getId()), contains(true)))
                .andExpect(jsonPath("$.content[?(@.id == %d)].savedByMe".formatted(liked.getId()), contains(false)))
                .andExpect(jsonPath("$.content[?(@.id == %d)].savedByMe".formatted(saved.getId()), contains(true)));
    }

    @Test
    void 비로그인으로_다른_회원_클립을_보면_플래그는_모두_false() throws Exception {
        mvc.perform(get("/api/members/{id}/clips", other.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].likedByMe", everyItem(is(false))))
                .andExpect(jsonPath("$.content[*].savedByMe", everyItem(is(false))));
    }

    @Test
    void 내_클립_목록() throws Exception {
        mvc.perform(get("/api/me/clips").header("Authorization", bearer(other)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].likedByMe", everyItem(is(false))));
    }

    @Test
    void 좋아요_상위_클립() throws Exception {
        mvc.perform(get("/api/top-liked").param("size", "10").header("Authorization", bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clipId").value(liked.getId()))
                .andExpect(jsonPath("$[0].uploaderName").value("상대"))
                .andExpect(jsonPath("$[0].likeCount").value(1));
    }

    @Test
    void 회원_요약() throws Exception {
        mvc.perform(get("/api/members/{id}/summary", other.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(other.getId()))
                .andExpect(jsonPath("$.name").value("상대"));
    }

    @Test
    void 팔로잉_클립_피드() throws Exception {
        mvc.perform(get("/api/me/following/clips").header("Authorization", bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }
}
