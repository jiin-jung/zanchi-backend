// src/main/java/com/zanchi/zanchi_backend/reco/PersonalizeConfig.java
package com.zanchi.zanchi_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.personalizeruntime.PersonalizeRuntimeClient;

@Configuration
public class PersonalizeConfig {

    @Bean
    public PersonalizeRuntimeClient personalizeRuntimeClient(
            @Value("${reco.aws-region:ap-northeast-2}") String region
    ) {
        return PersonalizeRuntimeClient.builder()
                .region(Region.of(region))
                .build(); // 자격증명은 기본 프로바이더 체인 사용
    }
}
