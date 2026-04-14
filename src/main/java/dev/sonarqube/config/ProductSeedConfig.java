package dev.sonarqube.config;

import dev.sonarqube.model.Product;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ProductSeedConfig {

    @Bean
    public List<Product> products() {
        return List.of(
                new Product(1L, "무선 이어폰 A", "audio", 94, 4.7, 1280, 129000, true, List.of("wireless", "music", "best")),
                new Product(2L, "게이밍 마우스 X", "gaming", 88, 4.5, 920, 59000, true, List.of("gaming", "fps", "rgb")),
                new Product(3L, "기계식 키보드 Pro", "gaming", 90, 4.8, 1430, 149000, false, List.of("keyboard", "typing", "rgb")),
                new Product(4L, "휴대용 보조배터리 20000", "mobile", 84, 4.4, 800, 49000, true, List.of("battery", "travel", "mobile")),
                new Product(5L, "노이즈캔슬링 헤드폰 Z", "audio", 96, 4.9, 2010, 289000, true, List.of("premium", "audio", "noise-canceling")),
                new Product(6L, "4K 모니터 27인치", "display", 82, 4.6, 510, 319000, false, List.of("monitor", "office", "4k")),
                new Product(7L, "웹캠 Full HD", "office", 76, 4.2, 330, 69000, true, List.of("camera", "meeting", "office")),
                new Product(8L, "블루투스 스피커 Mini", "audio", 79, 4.3, 610, 79000, true, List.of("speaker", "portable", "music")),
                new Product(9L, "태블릿 11인치", "mobile", 87, 4.6, 715, 459000, false, List.of("tablet", "study", "media")),
                new Product(10L, "스마트워치 Fit", "wearable", 83, 4.4, 540, 199000, true, List.of("watch", "health", "fitness")),
                new Product(11L, "USB-C 허브 8in1", "accessory", 74, 4.1, 280, 45000, true, List.of("usb-c", "office", "laptop")),
                new Product(12L, "노트북 스탠드 Air", "accessory", 72, 4.0, 190, 35000, true, List.of("desk", "office", "ergonomic"))
        );
    }
}