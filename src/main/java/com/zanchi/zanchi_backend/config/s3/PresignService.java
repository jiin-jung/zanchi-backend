package com.zanchi.zanchi_backend.config.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.URL;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class PresignService {
    private final S3Presigner presigner;
    @Value("${app.s3.bucket:${S3_BUCKET}}") private String bucket;

    public URL createPutUrl(String key, String contentType) {
        String resolvedContentType = (contentType == null || contentType.isBlank())
                ? "application/octet-stream"
                : contentType;

        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket).key(key).contentType(resolvedContentType).build();
        PresignedPutObjectRequest pre = presigner.presignPutObject(b -> b
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(put));
        return pre.url();
    }
}
