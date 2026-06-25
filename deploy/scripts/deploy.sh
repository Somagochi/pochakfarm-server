#!/usr/bin/env bash
#
# 블루그린 무중단 배포 스크립트 (dev 서버에서 실행).
#
# 동작:
#   1) 현재 활성 색상을 nginx upstream.conf 에서 읽는다 (없으면 첫 배포 → blue).
#   2) 비활성 색상 컨테이너를 새 이미지로 띄운다.
#   3) 관리 포트(actuator readiness)로 헬스체크가 통과할 때까지 대기.
#      실패하면 비활성 컨테이너를 내리고 종료(기존 트래픽은 그대로 유지).
#   4) upstream.conf 를 새 색상으로 교체하고 nginx reload → 트래픽 전환.
#   5) 이전 색상 컨테이너를 정지하고 댕글링 이미지를 정리.
#
# 필요한 환경변수: IMAGE, REGISTRY, ECR_PASSWORD, DEPLOY_PATH
# 선택: SPRING_PROFILES_ACTIVE(기본 dev), HEALTH_RETRIES(기본 30), HEALTH_INTERVAL(기본 5)

set -euo pipefail

: "${IMAGE:?IMAGE is required}"
: "${REGISTRY:?REGISTRY is required}"
: "${ECR_PASSWORD:?ECR_PASSWORD is required}"
: "${DEPLOY_PATH:?DEPLOY_PATH is required}"

export IMAGE
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"
HEALTH_RETRIES="${HEALTH_RETRIES:-30}"
HEALTH_INTERVAL="${HEALTH_INTERVAL:-5}"

cd "$DEPLOY_PATH"

UPSTREAM_FILE="$DEPLOY_PATH/nginx/conf.d/upstream.conf"
mkdir -p "$(dirname "$UPSTREAM_FILE")"

echo "==> ECR 로그인"
echo "$ECR_PASSWORD" | docker login --username AWS --password-stdin "$REGISTRY"

echo "==> 새 이미지 pull: $IMAGE"
docker compose pull app-blue app-green

# 현재 활성 색상 판별 (upstream.conf 의 app-<color> 에서 추출)
current=""
if [ -f "$UPSTREAM_FILE" ]; then
  current="$(grep -oE 'app-(blue|green)' "$UPSTREAM_FILE" | head -1 | sed 's/app-//')"
fi

if [ "$current" = "blue" ]; then
  target="green"; probe_port=9092
else
  target="blue"; probe_port=9091
fi

echo "==> 현재 활성: ${current:-none} / 배포 대상: $target (probe :$probe_port)"

# 대상 색상 컨테이너를 새 이미지로 기동 (다른 색상은 건드리지 않음)
docker compose up -d --no-deps --force-recreate "app-$target"

echo "==> $target readiness 헬스체크 대기"
healthy=0
for i in $(seq 1 "$HEALTH_RETRIES"); do
  if curl -fsS "http://localhost:${probe_port}/actuator/health/readiness" 2>/dev/null | grep -q '"status":"UP"'; then
    healthy=1
    echo "    OK ($i/$HEALTH_RETRIES)"
    break
  fi
  echo "    대기 중... ($i/$HEALTH_RETRIES)"
  sleep "$HEALTH_INTERVAL"
done

if [ "$healthy" != "1" ]; then
  echo "!! $target 헬스체크 실패 — 롤백(기존 ${current:-none} 유지)"
  docker compose logs --tail=50 "app-$target" || true
  docker compose stop "app-$target" || true
  exit 1
fi

# upstream 을 새 색상으로 교체
echo "==> upstream → app-$target 으로 전환"
cat > "$UPSTREAM_FILE" <<EOF
upstream pochakfarm_backend {
    server app-${target}:8080;
}
EOF

# nginx 기동(첫 배포) 또는 이미 떠 있으면 no-op
docker compose up -d nginx

# 설정 검증 후 reload (무중단 전환)
docker compose exec -T nginx nginx -t
docker compose exec -T nginx nginx -s reload

echo "==> 트래픽이 $target 으로 전환됨"

# 이전 색상 정지
if [ -n "$current" ] && [ "$current" != "$target" ]; then
  echo "==> 이전 색상 정지: app-$current"
  docker compose stop "app-$current" || true
fi

docker image prune -f
echo "==> 배포 완료: active=$target"
