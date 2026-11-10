package com.zanchi.zanchi_backend.domain.preference.dto;

import lombok.*;

@Getter @Setter @AllArgsConstructor
public class PreferenceTagResponse {
    private Long id;
    private String code;
    private String name;
}
