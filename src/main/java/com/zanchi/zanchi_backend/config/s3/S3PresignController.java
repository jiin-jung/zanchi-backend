package com.zanchi.zanchi_backend.config.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.net.URL;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3")
public class S3PresignController {

    private final PresignService presignService;

    @Value("${app.s3.bucket:${S3_BUCKET}}") private String bucket;
    @Value("${aws.region:${AWS_REGION:ap-northeast-2}}") private String region;

    // 프리사인드 PUT URL 발급
    @PostMapping("/presign-put")
    public Map<String, String> presignPut(@RequestBody PresignPutReq req) {
        String contentType = (req.contentType() == null || req.contentType().isBlank())
                ? "application/octet-stream"
                : req.contentType();

        // key 미전달 시 기본 경로/파일명 생성
        String key = (req.key() == null || req.key().isBlank())
                ? "uploads/%s/%s.%s".formatted(
                LocalDate.now(), UUID.randomUUID(),
                extFromContentType(contentType))
                : req.key();

        URL uploadUrl = presignService.createPutUrl(key, contentType);

        // 업로드 후 브라우저로 바로 볼 수 있는 공개 URL (버킷 정책이 Get 허용일 때)
        String objectUrl = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;

        return Map.of(
                "uploadUrl", uploadUrl.toString(),
                "method", "PUT",
                "key", key,
                "objectUrl", objectUrl
        );
    }

    public record PresignPutReq(String key, String contentType) {}

    private static String extFromContentType(String ct) {
        if (ct == null) return "bin";
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
