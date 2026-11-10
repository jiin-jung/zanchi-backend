package com.zanchi.zanchi_backend.domain.preference.dto;

import lombok.*;

import java.util.List;

@Getter @Setter @AllArgsConstructor
public class PreferenceSurveyResultResponse {
    private Long memberId;
    private List<PreferenceTagResponse> selectedTags;
    private boolean completed;
}
