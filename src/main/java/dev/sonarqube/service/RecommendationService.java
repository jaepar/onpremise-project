package dev.sonarqube.service;

import dev.sonarqube.dto.RecommendationResponse;

public interface RecommendationService {

    RecommendationResponse recommend(int size);
}
