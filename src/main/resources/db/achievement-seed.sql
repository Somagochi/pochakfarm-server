-- 업적/뱃지 마스터 데이터 시드 (MVP 업적 5종)
-- 실행 순서: badges -> achievements -> achievement_rewards
-- code 는 마이그레이션/운영 스크립트의 식별자이므로 한 번 배포되면 절대 변경하지 않는다.
--   업적: ACH + 3자리 일련번호 / 뱃지: BDG + 3자리 일련번호 (업적과 같은 번호로 페어링)
-- badges.code / achievements.code 는 유니크 제약이 있어 재실행 시 에러로 중단된다.
-- achievement_rewards 는 not exists 가드로 재실행해도 중복 생성되지 않는다.
-- 이후 추가되는 업적/뱃지도 이 파일에 누적한다.

insert into badges (code, name, description, image_key, created_at, updated_at)
values
    ('BDG001', '1기 포착단', '사전예약으로 포착팜에 합류했다', null, now(6), now(6)),
    ('BDG002', '첫 보금자리', '농장에 동물을 처음 배치했다', null, now(6), now(6)),
    ('BDG003', '한 우물 포착', '같은 타입의 동물을 10마리 이상 모았다', null, now(6), now(6)),
    ('BDG004', '골고루 수집가', '모든 타입의 동물을 모았다', null, now(6), now(6)),
    ('BDG005', '시작과 끝', '시작과 끝을 완성했다', null, now(6), now(6));

insert into achievements (code, title, description, category, metric, metric_param, target_value, enabled, hidden, unachieved_image_key, achieved_image_key, created_at, updated_at)
values
    ('ACH001', '1기 포착단', '사전예약 쿠폰을 사용해 포착팜에 합류한다', 'EVENT', 'PRE_REGISTRATION_CONVERTED', null, 1, true, false, null, null, now(6), now(6)),
    ('ACH002', '첫 보금자리', '농장에 동물을 처음 배치한다', 'FARM', 'PLACED_ANIMAL_COUNT', null, 1, true, false, null, null, now(6), now(6)),
    ('ACH003', '한 우물 포착', '같은 타입의 동물을 10마리 이상 보유한다', 'COLLECTION', 'MAX_OWNED_COUNT_PER_TYPE', null, 10, true, false, null, null, now(6), now(6)),
    ('ACH004', '골고루 수집가', '모든 타입의 동물을 1마리 이상 보유한다', 'COLLECTION', 'OWNED_TYPE_COUNT', null, 4, true, false, null, null, now(6), now(6)),
    ('ACH005', '시작과 끝', '1층 첫 번째 칸과 4층 네 번째 칸에만 동물을 배치한다', 'FARM', 'ONLY_START_END_PLACED', null, 1, true, true, null, null, now(6), now(6));

-- unachieved_image_key / achieved_image_key / badges.image_key 는 디자인 에셋 업로드 후 update 로 채운다.

-- 업적별 코인 1000 지급
insert into achievement_rewards (achievement_id, reward_type, reference_code, amount, created_at, updated_at)
select a.id, 'COIN', null, 1000, now(6), now(6)
from achievements a
where a.code in ('ACH001', 'ACH002', 'ACH003', 'ACH004', 'ACH005')
  and not exists (
    select 1
    from achievement_rewards r
    where r.achievement_id = a.id and r.reward_type = 'COIN'
  );

-- 업적별 동번호 뱃지 지급 (ACHnnn -> BDGnnn)
insert into achievement_rewards (achievement_id, reward_type, reference_code, amount, created_at, updated_at)
select a.id, 'BADGE', replace(a.code, 'ACH', 'BDG'), null, now(6), now(6)
from achievements a
where a.code in ('ACH001', 'ACH002', 'ACH003', 'ACH004', 'ACH005')
  and not exists (
    select 1
    from achievement_rewards r
    where r.achievement_id = a.id and r.reward_type = 'BADGE'
  );
