# On-Premise CI/CD Pipeline

> Spring Boot 기반 추천 API 서비스에 Jenkins + SonarQube + Docker를 활용한 CI/CD 파이프라인과 Nginx 카나리 배포 전략을 적용한 프로젝트입니다.

<br>

## 📁 Repository Structure

| Repository | 설명 | 링크 |
|------------|------|------|
| **Application (메인)** | Spring Boot 앱 코드 + Jenkinsfile | [jaepar/onpremise-project](https://github.com/jaepar/onpremise-project) |
| **CI/CD Server** | Jenkins + SonarQube Docker 구성 | [sene03/ci-practice-cicd-server](https://github.com/sene03/ci-practice-cicd-server) |
| **Deploy Server** | 배포 서버 Docker Compose + Nginx + deploy.sh | [sene03/ci-practice-deploy-server](https://github.com/sene03/ci-practice-deploy-server) |

<br>

## 🏗 Architecture

<img width="1250" height="482" alt="스크린샷 2026-04-15 오후 2 02 20" src="https://github.com/user-attachments/assets/113a16cf-ecfc-47ec-a76f-fe9a48d5f1c6" />


```
Developer
  └─ git push → GitHub (main)
       └─ Webhook → Jenkins (CI 서버 · 172.21.33.69)
            ├─ Gradle 빌드
            ├─ SonarQube 정적 분석 + Quality Gate
            ├─ Docker 이미지 빌드 & DockerHub push
            └─ SSH → 배포 서버 (172.21.33.26)
                 ├─ deploy.sh 실행
                 │    ├─ stable 헬스체크
                 │    ├─ canary 이미지 교체 & 재시작
                 │    ├─ canary 헬스체크 (실패 시 자동 롤백)
                 │    └─ Nginx reload (트래픽 분배 적용)
                 └─ Slack 알림 (#deploy)
```

<br>

## 📦 프로젝트 개요

### 애플리케이션

Spring Boot 3.3.5 기반의 상품 추천 API 서버입니다.

- **Language** : Java 21
- **Framework** : Spring Boot 3.3.5
- **Build Tool** : Gradle
- **DB** : MySQL 8.0
- **모니터링** : Spring Actuator + Micrometer (Prometheus 메트릭 노출)

**주요 엔드포인트**

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/recommendations` | 추천 목록 조회 |
| GET | `/actuator/health` | 헬스체크 |
| GET | `/actuator/prometheus` | Prometheus 메트릭 |

### 배포 전략 : 카나리 배포(Canary Deployment)

카나리 배포를 선택한 이유는 다음과 같습니다.

- **안전한 점진적 배포** : 전체 트래픽을 한 번에 전환하지 않고 소수 트래픽(10%)에 먼저 노출해 장애 영향 범위를 최소화할 수 있습니다.
- **실시간 검증** : 새 버전을 실제 트래픽으로 검증하면서 Grafana 대시보드에서 응답시간, 에러율을 모니터링할 수 있습니다.
- **즉각적 롤백** : 헬스체크 실패 시 자동으로 이전 버전으로 복구됩니다.
- **무중단 배포** : `nginx -s reload`는 기존 커넥션을 유지한 채 새 설정을 적용하므로 서비스 중단이 없습니다.

**트래픽 전환 단계**

```
신규 배포
  → canary 10% 적용 (자동)
    → 모니터링 후 이상 없으면
      → canary 50% 적용 (수동)
        → 최종 확인 후
          → canary 100% 전환 → stable 승격 (수동)
```

<br>

## 🔧 Jenkins Pipeline

Jenkinsfile은 애플리케이션 레포 루트에 위치하며, Jenkins가 GitHub Webhook을 통해 `main` 브랜치에 push가 발생하면 자동으로 실행됩니다.

### 파이프라인 흐름

```
① Checkout       — GitHub에서 소스코드 pull
② Build          — ./gradlew clean build -x test
③ SonarQube      — ./gradlew sonar (정적 분석)
④ Quality Gate   — SonarQube 분석 결과 대기 (5분 timeout, 실패 시 중단)
⑤ Docker Build   — 이미지 빌드 및 DockerHub push
                   태그: canary-{GIT_COMMIT 앞 7자리}
⑥ Image Cleanup  — Jenkins 서버 기존 이미지 정리
⑦ Deploy Canary  — SSH로 배포 서버 deploy.sh 실행
                   ① stable 헬스체크 (실패 시 배포 중단)
                   ② .env CANARY_TAG 업데이트
                   ③ canary 이미지 pull & 컨테이너 재시작
                   ④ canary 헬스체크 (실패 시 자동 롤백)
                   ⑤ Nginx conf 교체 & reload (10% 트래픽 적용)
                   → 자세한 내용: [Deploy Server README](https://github.com/sene03/ci-practice-deploy-server)
⑧ Slack 알림     — 성공/실패 여부 #deploy 채널로 전송
```

### 이미지 태그 전략

Git 커밋 해시 앞 7자리를 태그로 사용합니다. 매 커밋마다 고유한 태그가 자동 생성되므로 스크립트 수정 없이 버전이 관리됩니다.

```
canary-abc1234  →  canary-def5678  →  canary-ghi9012 ...
```

### SonarQube Quality Gate

`Sonar way` 기본 Quality Gate를 사용합니다.

- 신규 코드 커버리지, 버그, 취약점, 코드 스멜 기준을 충족하지 못하면 파이프라인이 자동 중단됩니다.
- Quality Gate를 통과해야만 Docker 빌드 및 배포 단계로 진행됩니다.

<br>

## 🐳 카나리 배포 적용 방법

### Nginx 볼륨 마운트 구조

배포 서버 호스트의 conf 파일을 Nginx 컨테이너에 마운트하는 방식을 사용합니다. 컨테이너를 재시작하지 않고 호스트 파일 교체 + `nginx -s reload` 만으로 트래픽 비율을 변경할 수 있습니다.

```
배포 서버 호스트
  /home/sw_team_4/ci-practice-deploy-server/
    └─ nginx/conf.d/
         ├─ default.conf      ← 현재 적용중 (컨테이너에 마운트됨)
         ├─ canary_10.conf    ← stable 90% / canary 10%
         ├─ canary_50.conf    ← stable 50% / canary 50%
         └─ canary_100.conf   ← canary 100% (전환 완료)
```

```yaml
# docker-compose.yml
nginx:
  volumes:
    - ./nginx/conf.d/default.conf:/etc/nginx/conf.d/default.conf:ro
```

### .env 기반 이미지 태그 관리

```bash
# /home/sw_team_4/ci-practice-deploy-server/.env
STABLE_TAG=abc1234   # 현재 운영중인 버전
CANARY_TAG=def5678   # 새로 배포되는 버전
```

`deploy.sh`가 `CANARY_TAG`를 새 커밋 해시로 교체하고 `spring-canary` 컨테이너만 재시작합니다. `STABLE_TAG`는 수동으로 승격시킵니다.

### 트래픽 비율 변경 (수동)

배포 후 모니터링을 통해 이상이 없으면 아래 명령어로 트래픽 비율을 단계적으로 높입니다.

```bash
# 배포 서버에 SSH 접속 후

# 50%로 상향
cp nginx/conf.d/canary_50.conf nginx/conf.d/default.conf
docker compose exec nginx nginx -s reload

# 100% 전환 (카나리 → stable 승격)
cp nginx/conf.d/canary_100.conf nginx/conf.d/default.conf
docker compose exec nginx nginx -s reload

# .env stable 태그 업데이트
sed -i "s/STABLE_TAG=.*/STABLE_TAG={새 커밋 해시}/" .env
sed -i "s/CANARY_TAG=.*/CANARY_TAG={새 커밋 해시}/" .env
```

### 롤백

```bash
# deploy.sh가 헬스체크 실패 시 자동 롤백
# 수동 롤백이 필요한 경우

sed -i "s/CANARY_TAG=.*/CANARY_TAG=${STABLE_TAG}/" .env
docker compose up -d --no-deps spring-canary
docker compose exec nginx nginx -s reload
```


<br>

## 🗂 Port Table

### CI 서버 (172.21.33.69)

| 서비스 | 호스트 포트 | 컨테이너 포트 |
|--------|------------|--------------|
| Jenkins UI | 8300 | 8080 |
| Jenkins Agent | 8301 | 50000 |
| SonarQube | 8302 | 9000 |

### 배포 서버 (172.21.33.26)

| 서비스 | 호스트 포트 | 컨테이너 포트 |
|--------|------------|--------------|
| Nginx (서비스 진입점) | 8303 | 80 |
| Prometheus | 8304 | 9090 |
| Grafana | 8305 | 3000 |
| Spring stable | internal | 8080 |
| Spring canary | internal | 8080 |
| MySQL | internal | 3306 |
