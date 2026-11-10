package com.zanchi.zanchi_backend.web.reco;

import com.zanchi.zanchi_backend.domain.clip.dto.ClipFeedRes;
import com.zanchi.zanchi_backend.domain.reco.service.RecoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reco")
@RequiredArgsConstructor
public class RecoController {

    private final RecoService recoService;

    /**
     * 내 개인화 추천 피드
     * GET /api/reco/my-feed?page=0&size=20
     */
    @GetMapping("/my-feed")
    public ResponseEntity<Page<ClipFeedRes>> myFeed(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (auth == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(recoService.getMyFeed(auth.getName(), page, size));
    }
}
