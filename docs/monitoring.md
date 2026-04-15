# 실행 방법

## 사전 준비

프로젝트 루트에 `.env` 파일을 생성합니다.

```

STABLE_TAG=<stable 커밋 해시>
CANARY_TAG=<canary 커밋 해시>

```

## 전체 서비스 실행

```

docker compose up -d

```

## 서비스 중지

```

docker compose down

```

---

# 모니터링 확인 방법

## 1. Prometheus 타겟 확인

- 접속 URL: http://localhost:8304/targets

| 상태 | 의미 |
|------|------|
| UP   | Prometheus가 정상적으로 메트릭을 수집 중 |
| DOWN | 메트릭 수집 실패 (에러 원인 확인 필요) |

---

## 2. Grafana 대시보드

- 접속 URL: http://localhost:8305
- 초기 계정: admin / admin
- Prometheus 연결 URL: http://sw_team4_prometheus:9090

### 주요 쿼리

#### Stable vs Canary 평균 응답시간 비교

```

sum(rate(http_server_requests_seconds_sum{uri="/recommendations"}[1m])) by (version)
/
sum(rate(http_server_requests_seconds_count{uri="/recommendations"}[1m])) by (version)

```

Canary 응답 시간이 급격히 증가하면 롤백의 근거로 활용할 수 있습니다.

#### Stable vs Canary 에러율 비교

```

sum(rate(http_server_requests_seconds_count{uri="/recommendations", status=~"5.."}[1m])) by (version)
/
sum(rate(http_server_requests_seconds_count{uri="/recommendations"}[1m])) by (version)

````

Canary에서 5xx 에러율이 증가하면 즉시 감지할 수 있습니다.

---

# 트러블슈팅

## Prometheus 타겟 DOWN (400 에러)

### 원인

Prometheus가 `sw_team4_spring_stable:8080`으로 요청을 보낼 때, Host 헤더에 언더스코어(`_`)가 포함됩니다.  
Tomcat은 RFC 표준에 따라 언더스코어가 포함된 호스트명을 허용하지 않아 `400 Bad Request`를 반환합니다.

### 해결 방법

Docker 네트워크에 언더스코어가 없는 alias를 추가하여 접근하도록 설정합니다.

### docker-compose.yml

```yaml
spring-stable:
  networks:
    sw_team4_app_net:
      aliases:
        - spring-stable   # 언더스코어 없는 alias 추가
````

### prometheus/prometheus.yml

```yaml
targets: ['spring-stable:8080', 'spring-canary:8080']
```

