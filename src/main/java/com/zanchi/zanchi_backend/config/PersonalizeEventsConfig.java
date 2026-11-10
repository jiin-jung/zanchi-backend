package com.zanchi.zanchi_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.personalizeevents.PersonalizeEventsClient;

@Configuration
public class PersonalizeEventsConfig {
    @Value("${aws.region:${AWS_REGION}}")
    private String region;

    @Bean
    public PersonalizeEventsClient personalizeEventsClient() {
        return PersonalizeEventsClient.builder()
                .region(Region.of(region))
                .build();
    }
}