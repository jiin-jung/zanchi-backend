package com.zanchi.zanchi_backend.config.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URL;
import java.time.Duration;

/** S3 업로드용 프리사인 URL 발급과 객체 URL 생성을 한 곳에서 담당 */
@Service
@RequiredArgsConstructor
public class PresignService {

    public static final Duration EXPIRY = Duration.ofMinutes(10);

    private final S3Presigner presigner;
    @Value("${app.s3.bucket:${S3_BUCKET}}") private String bucket;
    @Value("${aws.region:${AWS_REGION:ap-northeast-2}}") private String region;

    public URL createPutUrl(String key, String contentType) {
        String resolvedContentType = (contentType == null || contentType.isBlank())
                ? "application/octet-stream"
                : contentType;

        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket).key(key).contentType(resolvedContentType).build();
        return presigner.presignPutObject(b -> b
                .signatureDuration(EXPIRY)
                .putObjectRequest(put)).url();
    }

    /** 업로드된 객체를 조회할 공개 URL (버킷 정책이 GET을 허용할 때) */
    public String objectUrl(String key) {
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }
}
