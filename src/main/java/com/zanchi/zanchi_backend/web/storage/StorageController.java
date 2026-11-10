package com.zanchi.zanchi_backend.web.storage;

import com.zanchi.zanchi_backend.config.s3.PresignService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * 프리사인 PUT URL 발급.
 * 두 URL은 프론트 호환을 위해 유지하고, 발급 로직은 PresignService 하나로 처리한다.
 */
@RestController
@RequiredArgsConstructor
public class StorageController {

    private final PresignService presignService;

    @GetMapping("/api/storage/presign")
    public Map<String, Object> presign(@RequestParam String filename,
                                       @RequestParam String contentType) {
        String ext = "mp4";
        int p = filename.lastIndexOf('.');
        if (p >= 0 && p + 1 < filename.length()) ext = filename.substring(p + 1);

        String key = "clips/%s.%s".formatted(UUID.randomUUID(), ext);

        return Map.of(
                "url", presignService.createPutUrl(key, contentType).toString(),
                "method", "PUT",
                "key", key,
                "expiresInSec", PresignService.EXPIRY.toSeconds()
        );
    }

    @PostMapping("/api/s3/presign-put")
    public Map<String, String> presignPut(@RequestBody PresignPutReq req) {
        String contentType = (req.contentType() == null || req.contentType().isBlank())
                ? "application/octet-stream"
                : req.contentType();

        // key 미전달 시 기본 경로/파일명 생성
        String key = (req.key() == null || req.key().isBlank())
                ? "uploads/%s/%s.%s".formatted(LocalDate.now(), UUID.randomUUID(), extFromContentType(contentType))
                : req.key();

        return Map.of(
                "uploadUrl", presignService.createPutUrl(key, contentType).toString(),
                "method", "PUT",
                "key", key,
                "objectUrl", presignService.objectUrl(key)
        );
    }

    public record PresignPutReq(String key, String contentType) {}

    private static String extFromContentType(String ct) {
        return switch (ct) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/gif" -> "gif";
            case "video/mp4" -> "mp4";
            case "text/plain" -> "txt";
            default -> "bin";
        };
    }
}
