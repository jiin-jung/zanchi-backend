package com.zanchi.zanchi_backend.domain.ai;

public record IdeaItem(
        String id,          // ← 추가: name+lat+lng 해시 등
        String role, String name, String address,
        double lat, double lng,
        String externalUrl, String mapLink,
        Double rating, Integer ratingCount) {
}
