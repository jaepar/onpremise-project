package dev.sonarqube.model;

import java.util.List;

public record Product(
        long id,
        String name,
        String category,
        int popularityScore,
        double rating,
        int reviewCount,
        int price,
        boolean fastShipping,
        List<String> tags
) {
}
