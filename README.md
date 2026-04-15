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

<img width="1245" height="478" alt="스크린샷 2026-04-15 오후 2 21 57" src="https://github.com/user-attachments/assets/d3314887-1675-44fe-a43d-6c726a08b807" />



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
| GET | `/recommendations` | 추천 목록 조회 |
| GET | `/actuator/health` | 헬스체크 |
| GET | `/actuator/prometheus` | Prometheus 메트릭 |

### 배포 전략 : 카나리 배포(Canary Deployment)


추천 API는 **추천 알고리즘의 품질이 사용자 경험에 직접 영향을 미치는 서비스**입니다.

이번 프로젝트에서는 두 버전 간 실질적인 차이를 만들어 카나리 배포의 효과를 검증할 수 있도록 설계했습니다.

| 구분 | stable (v1) | canary (v2) |
|------|------------|-------------|
| 추천 방식 | 랜덤 추천 | 점수 기반 추천 알고리즘 |
| 응답시간 | 빠름 | 알고리즘 연산으로 인한 지연 발생 |

알고리즘 도입은 응답시간 증가를 수반하기 때문에 전체 트래픽을 한 번에 전환하면 위험합니다. 소수 트래픽(10%)에 먼저 노출해 Grafana에서 응답시간·에러율을 실시간 비교하고, k6 부하 테스트로 트래픽이 몰리는 상황을 시뮬레이션하면서 점진적으로 검증합니다. 이상 감지 시 Nginx conf 교체 한 번으로 즉시 롤백할 수 있습니다.




<br>

## 🔧 Jenkins Pipeline

[Jenkinsfile](Jenkinsfile)은 GitHub Webhook을 통해 `main` 브랜치에 push가 발생하면 Jenkins를 통해 자동으로 실행됩니다.

### 파이프라인 흐름


| 단계 | Stage | 설명 |
|------|-------|------|
| 1. 코드 체크아웃 | `Checkout` | GitHub에서 소스코드 pull |
| 2. 빌드 | `Build` | `./gradlew clean build -x test` |
| 3. 정적 코드 분석 | `SonarQube Analysis` | `./gradlew sonar` 실행 → SonarQube에 분석 결과 전송 |
| 4. Quality Gate | `Quality Gate` | SonarQube 분석 결과 대기 (5분 timeout)<br>Sonar way 4가지 조건 미충족 시 파이프라인 중단 |
| 5. Docker 빌드 & 푸시 | `Docker Build & Push` | 이미지 빌드 후 DockerHub push<br>태그: `canary-{커밋해시 7자리}` |
| 6. 이미지 정리 | `Cleanup Jenkins Images` | CI 서버에서 현재 태그 제외한 기존 이미지 삭제 |
| 7. 카나리 배포 | `Deploy Canary` | SSH로 배포 서버 `deploy.sh` 실행 |

**post**
- 성공 → Slack `#deploy` 채널에 성공 알림
- 실패 → Slack `#deploy` 채널에 실패 단계명 + 빌드 URL 알림


`deploy.sh` 와 관련한 내용은 [Deploy Server README](https://github.com/sene03/ci-practice-deploy-server)에서 확인할 수 있습니다.


### 이미지 태그 전략

Git 커밋 해시 앞 7자리를 태그로 사용합니다. 매 커밋마다 고유한 태그가 자동 생성되므로 스크립트 수정 없이 버전이 관리됩니다.

```
canary-abc1234  →  canary-def5678  →  canary-ghi9012 ...
```

### SonarQube Quality Gate

`Sonar way` 기본 Quality Gate를 사용합니다.

- 신규 코드 커버리지, 버그, 취약점, 코드 스멜 기준을 충족하지 못하면 파이프라인이 자동 중단됩니다.
- Quality Gate를 통과해야만 Docker 빌드 및 배포 단계로 진행됩니다.


#### SonarQube Quality Gate · Sonar way 기준

Sonar way는 SonarSource가 기본 제공하는 read-only Quality Gate로, **신규 코드(New Code)** 에만 조건을 적용하는 **Clean as You Code** 방식을 따릅니다.

**신규 코드에 적용되는 4가지 조건:**

기준이 충족되지 않을 시 Jenkins 파이프라인이 중단됩니다.

| 조건 | 기준 |
|------|------|
| 신규 이슈 | 0건 초과 |
| Security Hotspot 검토율 | 100% 미만 |
| 신규 코드 테스트 커버리지 | 80% 미만 |
| 신규 코드 중복률 | 3% 초과 |

**선택 이유:**
- 기존 레거시 코드를 전부 고치는 데 리소스를 쏟는 대신, **새로 작성하는 코드의 품질을 유지**하는 데 집중하는 전략
- 별도 설정 없이 바로 적용 가능한 기본값

**출처:** [SonarQube Server 10.8 Docs — Quality Gates](https://docs.sonarsource.com/sonarqube-server/10.8/instance-administration/analysis-functions/quality-gates)


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
