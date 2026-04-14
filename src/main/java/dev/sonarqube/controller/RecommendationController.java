package dev.sonarqube.controller;

import dev.sonarqube.dto.RecommendationResponse;
import dev.sonarqube.service.RecommendationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recommendations")
public class RecommendationController {

    private static final int DEFAULT_SIZE = 5;
    private static final int MAX_SIZE = 20;
    @Value("${APP_VERSION:v1}")
    private String appVersion; // final 제거, 초기화 값 제거

    private final RecommendationService v1RecommendationService;
    private final RecommendationService v2RecommendationService;

    public RecommendationController(
            @Qualifier("v1RecommendationService") RecommendationService v1RecommendationService,
            @Qualifier("v2RecommendationService") RecommendationService v2RecommendationService
    ) {
        this.v1RecommendationService = v1RecommendationService;
        this.v2RecommendationService = v2RecommendationService;
    }

    @GetMapping
    public ResponseEntity<RecommendationResponse> recommend(
            @RequestParam(defaultValue = "5") int size,
            @RequestHeader(name = "X-Recommendation-Version", required = false) String versionHeader
    ) {
        int normalizedSize = normalizeSize(size);
        String version = (versionHeader != null) ? versionHeader : appVersion;

        // NginX에서 헤더를 전달해주는 경우
//        RecommendationResponse response = switch (versionHeader == null ? "v1" : versionHeader.toLowerCase()) {
//            case "v2" -> v2RecommendationService.recommend(normalizedSize);
//            default -> v1RecommendationService.recommend(normalizedSize);
//        };

        // NginX에서 헤더를 전달해주지 않는 경우, 환경변수에서 버전 추출
        RecommendationResponse response = "v2".equalsIgnoreCase(version)
                ? v2RecommendationService.recommend(normalizedSize)
                : v1RecommendationService.recommend(normalizedSize);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> recommendationHealth() {
        return ResponseEntity.ok("recommendation-api ok");
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
