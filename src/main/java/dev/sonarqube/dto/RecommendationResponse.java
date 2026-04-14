package dev.sonarqube.dto;

import java.util.List;

public record RecommendationResponse(
        String version,
        long tookMs,
        int requestedSize,
        List<Item> items
) {
    public record Item(
            long id,
            String name,
            String category,
            int price,
            double rating,
            int score,
            List<String> tags
    ) {
    }
}