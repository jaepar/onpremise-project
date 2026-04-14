package dev.sonarqube.service;

import dev.sonarqube.dto.RecommendationResponse;
import dev.sonarqube.model.Product;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@Qualifier("v2RecommendationService")
public class V2RecommendationService implements RecommendationService {

    private final List<Product> products;

    public V2RecommendationService(List<Product> products) {
        this.products = products;
    }

    @Override
    public RecommendationResponse recommend(int size) {
        long start = System.currentTimeMillis();

        simulateDelay();

        List<RecommendationResponse.Item> items = products.stream()
                .map(product -> new ScoredProduct(product, calculateScore(product)))
                .sorted(Comparator.comparingInt(ScoredProduct::score).reversed())
                .limit(size)
                .map(scored -> new RecommendationResponse.Item(
                        scored.product().id(),
                        scored.product().name(),
                        scored.product().category(),
                        scored.product().price(),
                        scored.product().rating(),
                        scored.score(),
                        scored.product().tags()
                ))
                .toList();

        return new RecommendationResponse(
                "v2",
                System.currentTimeMillis() - start,
                size,
                items
        );
    }

    private int calculateScore(Product product) {
        int ratingScore = (int) Math.round(product.rating() * 20);
        int reviewScore = Math.min(product.reviewCount() / 20, 30);
        int shippingScore = product.fastShipping() ? 10 : 0;
        int popularityScore = product.popularityScore();
        return popularityScore + ratingScore + reviewScore + shippingScore;
    }

    private void simulateDelay() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("추천 처리 중 인터럽트가 발생했습니다.", e);
        }
    }

    private record ScoredProduct(Product product, int score) {
    }
}
