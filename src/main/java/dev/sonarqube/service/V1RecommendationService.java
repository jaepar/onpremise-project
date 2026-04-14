package dev.sonarqube.service;

import dev.sonarqube.dto.RecommendationResponse;
import dev.sonarqube.model.Product;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Qualifier("v1RecommendationService")
public class V1RecommendationService implements RecommendationService {

    private final List<Product> products;

    public V1RecommendationService(List<Product> products) {
        this.products = products;
    }

    @Override
    public RecommendationResponse recommend(int size) {
        long start = System.currentTimeMillis();

        List<Product> shuffled = new ArrayList<>(products);
        Collections.shuffle(shuffled);

        List<RecommendationResponse.Item> items = shuffled.stream()
                .limit(size)
                .map(product -> new RecommendationResponse.Item(
                        product.id(),
                        product.name(),
                        product.category(),
                        product.price(),
                        product.rating(),
                        ThreadLocalRandom.current().nextInt(50, 80),
                        product.tags()
                ))
                .toList();

        return new RecommendationResponse(
                "v1",
                System.currentTimeMillis() - start,
                size,
                items
        );
    }
}
