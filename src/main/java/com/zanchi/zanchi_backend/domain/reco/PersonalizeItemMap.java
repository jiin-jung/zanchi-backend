package com.zanchi.zanchi_backend.domain.reco; // 패키지는 프로젝트 규칙에 맞게 조정

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "personalize_item_map")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PersonalizeItemMap {
    @Id
    @Column(name = "item_id", length = 255, nullable = false)
    private String itemId;

    @Column(name = "clip_id", nullable = false)
    private Long clipId;
}
