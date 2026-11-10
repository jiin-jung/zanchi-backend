package com.zanchi.zanchi_backend.domain.reco.service;

import com.zanchi.zanchi_backend.domain.clip.Clip;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.personalizeevents.PersonalizeEventsClient;
import software.amazon.awssdk.services.personalizeevents.model.Item;
import software.amazon.awssdk.services.personalizeevents.model.PutItemsRequest;

import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class PersonalizeCatalogService {

    private final PersonalizeEventsClient eventsClient;

    @Value("${PERSONALIZE_ITEMS_DATASET_ARN}")
    private String itemsDatasetArn;

    public void upsertClip(Clip clip, String genresPipeSeparated) {
        long created = clip.getCreatedAt().atZone(ZoneId.of("UTC")).toEpochSecond();

        // Items 스키마: ITEM_ID(string), GENRES(string; categorical=true), CREATION_TIMESTAMP(long)
        String properties = String.format(
                "{\"GENRES\":\"%s\",\"CREATION_TIMESTAMP\":%d}",
                escape(genresPipeSeparated == null ? "" : genresPipeSeparated), created
        );

        eventsClient.putItems(PutItemsRequest.builder()
                .datasetArn(itemsDatasetArn)
                .items(Item.builder()
                        .itemId("clip_" + clip.getId())
                        .properties(properties)
                        .build())
                .build());
    }

    private String escape(String s) { return s.replace("\"", "\\\""); }
}