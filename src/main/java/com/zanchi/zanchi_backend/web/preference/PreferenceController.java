package com.zanchi.zanchi_backend.web.preference;

import com.zanchi.zanchi_backend.domain.preference.service.PreferenceService;
import com.zanchi.zanchi_backend.web.preference.dto.PreferenceSurveyRequest;
import com.zanchi.zanchi_backend.domain.preference.dto.PreferenceSurveyResultResponse;
import com.zanchi.zanchi_backend.domain.preference.dto.PreferenceTagResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/preferences")
public class PreferenceController {

    private final PreferenceService preferenceService;

    // 전체 태그 목록 조회 (회원가입 직전/직후 표출)
    @GetMapping("/tags")
    public ResponseEntity<List<PreferenceTagResponse>> getAllTags() {
        return ResponseEntity.ok(preferenceService.listAllTags());
    }

    // 내 선호 태그 조회
    @GetMapping("/me")
    public ResponseEntity<List<PreferenceTagResponse>> myTags(
            @AuthenticationPrincipal(expression = "id") Long memberId // 프로젝트 설정에 맞게 변경
    ) {
        return ResponseEntity.ok(preferenceService.myTags(memberId));
    }

    // 선호도 조사 제출 (회원가입 직후 1회)
    @PostMapping("/survey")
    public ResponseEntity<PreferenceSurveyResultResponse> submitSurvey(
            @AuthenticationPrincipal(expression = "id") Long memberId, // SecurityContext 에서 memberId 추출
            @Valid @RequestBody PreferenceSurveyRequest request
    ) {
        PreferenceSurveyResultResponse res = preferenceService.submitSurvey(memberId, request.getTagIds());
        return ResponseEntity.ok(res);
    }
}
