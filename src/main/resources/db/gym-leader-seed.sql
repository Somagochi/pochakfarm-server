-- NPC 관장 마스터 데이터 시드 (관장 8명, 관장 동물 24마리, 관장 뱃지 8종)
-- 편성 근거: SOMA-206 확정표 / .ai/planning/SOMA-206-npc-gym-leaders.md
-- 실행 순서: battle-init-schema.sql -> battle-progress-schema.sql -> gym-leader-thumbnail-schema.sql
--            -> badges -> gym_leaders -> gym_leader_animals
-- code 는 마이그레이션/운영 스크립트의 식별자이므로 한 번 배포되면 절대 변경하지 않는다.
--   관장: GYM + 3자리 일련번호 / 뱃지: BDG + 3자리 일련번호 (achievement-seed.sql 의 BDG005 다음 번호)
-- badges.code / gym_leaders.code 는 유니크 제약이 있어 재실행 시 에러로 중단된다.
-- gym_leader_animals 는 not exists 가드로 재실행해도 중복 생성되지 않는다.
-- 동물 이름 컬럼은 GymLeaderAnimal 이 @Embedded AnimalName 을 쓰므로 animal_name 이다.
-- 요구 레벨(1/3/7/12/18/25/32/40)은 밸런스 패치 대상이라 시드 컬럼이 아닌 BattlePolicy 정책값으로 관리한다.
-- 밸런스 재조정으로 편성이 바뀌면 insert 를 추가하지 말고 이 파일 말미에 update 블록을 누적한다.

insert into badges (code, name, description, image_key, created_at, updated_at)
values
    ('BDG006', '새싹 뱃지', '새싹 관장 두더를 이겼다', null, now(6), now(6)),
    ('BDG007', '물결 뱃지', '물결 관장 포미를 이겼다', null, now(6), now(6)),
    ('BDG008', '바람 뱃지', '바람 관장 휘루를 이겼다', null, now(6), now(6)),
    ('BDG009', '별빛 뱃지', '별빛 관장 노바를 이겼다', null, now(6), now(6)),
    ('BDG010', '숲바람 뱃지', '숲바람 관장 라온을 이겼다', null, now(6), now(6)),
    ('BDG011', '밤바다 뱃지', '밤바다 관장 미르하를 이겼다', null, now(6), now(6)),
    ('BDG012', '순환 뱃지', '순환 관장 하울을 이겼다', null, now(6), now(6)),
    ('BDG013', '챔피언 뱃지', '챔피언 아스트라를 이겼다', null, now(6), now(6));

insert into gym_leaders (code, name, challenge_order, badge_code, image_key, created_at, updated_at)
values
    ('GYM001', '두더', 1, 'BDG006', null, now(6), now(6)),
    ('GYM002', '포미', 2, 'BDG007', null, now(6), now(6)),
    ('GYM003', '휘루', 3, 'BDG008', null, now(6), now(6)),
    ('GYM004', '노바', 4, 'BDG009', null, now(6), now(6)),
    ('GYM005', '라온', 5, 'BDG010', null, now(6), now(6)),
    ('GYM006', '미르하', 6, 'BDG011', null, now(6), now(6)),
    ('GYM007', '하울', 7, 'BDG012', null, now(6), now(6)),
    ('GYM008', '아스트라', 8, 'BDG013', null, now(6), now(6));

-- 스킬 전투 유형은 CardSkill enum 이 단일 출처다. 아래 주석의 (안정/균형/승부)는 슬롯 구성 검증용 표기다.
-- 관장별 슬롯 구성(안정/균형/승부, 6슬롯): 1=5/1/0, 2=4/2/0, 3=3/3/0, 4=2/4/0, 5=2/3/1, 6=1/4/1, 7=1/3/2, 8=0/3/3
-- 슬롯 구성을 맞추기 위해 일부 동물은 스킬 2개가 같은 전투 유형이다 (정책서 4.3 "가능한" 조건).
insert into gym_leader_animals (gym_leader_id, order_no, animal_name, card_type, tier, skill_1, skill_2, image_key, created_at, updated_at)
select l.id, v.order_no, v.animal_name, v.card_type, v.tier, v.skill_1, v.skill_2, null, now(6), now(6)
from gym_leaders l
join (
    -- GYM001 새싹 관장 두더 / 땅 단일 / C·C·B
    select 'GYM001' as leader_code, 1 as order_no, '도톨' as animal_name, 'GROUND' as card_type, 'C' as tier, 'GROUND_LEAF_GUARD' as skill_1, 'GROUND_MOSS_CUSHION' as skill_2
    union all
    select 'GYM001', 2, '모리', 'GROUND', 'C', 'GROUND_BARK_SHIELD', 'GROUND_GARDEN_REST'
    union all
    select 'GYM001', 3, '바우', 'GROUND', 'B', 'GROUND_MOUNTAIN_POSE', 'GROUND_STONE_TAP'
    union all
    -- GYM002 물결 관장 포미 / 바다 단일 / C·B·B
    select 'GYM002', 1, '파도리', 'SEA', 'C', 'SEA_BUBBLE_GUARD', 'SEA_CORAL_HIDE'
    union all
    select 'GYM002', 2, '조개비', 'SEA', 'B', 'SEA_SEASHELL_SHIELD', 'SEA_FOAM_ROLL'
    union all
    select 'GYM002', 3, '미르', 'SEA', 'B', 'SEA_TURTLE_GUARD', 'SEA_DOLPHIN_STEP'
    union all
    -- GYM003 바람 관장 휘루 / 하늘 주력 2 + 바다 견제 1 / B·B·A
    select 'GYM003', 1, '구르미', 'SKY', 'B', 'SKY_FEATHER_GUARD', 'SKY_FLOATING_STEP'
    union all
    select 'GYM003', 2, '하늬', 'SKY', 'B', 'SKY_CLOUD_CUSHION', 'SKY_TAILWIND'
    union all
    select 'GYM003', 3, '여울', 'SEA', 'A', 'SEA_KELP_WRAP', 'SEA_TIDAL_BOOST'
    union all
    -- GYM004 별빛 관장 노바 / 우주 주력 2 + 땅 견제 1 / B·A·A
    select 'GYM004', 1, '별콩', 'SPACE', 'B', 'SPACE_MOON_GUARD', 'SPACE_ORBIT_STEP'
    union all
    select 'GYM004', 2, '코스미', 'SPACE', 'A', 'SPACE_ASTRO_SHIELD', 'SPACE_AURORA_WAVE'
    union all
    select 'GYM004', 3, '도리', 'GROUND', 'A', 'GROUND_VINE_PULL', 'GROUND_HARVEST_BOOST'
    union all
    -- GYM005 숲바람 관장 라온 / 혼합 3타입 / A·A·S
    select 'GYM005', 1, '이끼', 'GROUND', 'A', 'GROUND_NATURE_CALL', 'GROUND_CLOVER_LUCK'
    union all
    select 'GYM005', 2, '소리', 'SKY', 'A', 'SKY_CLEAR_MIND', 'SKY_WIND_CHIME'
    union all
    select 'GYM005', 3, '성운', 'SPACE', 'S', 'SPACE_PULSAR_BEAT', 'SPACE_NOVA_FLASH'
    union all
    -- GYM006 밤바다 관장 미르하 / 혼합 3타입 / A·S·S
    select 'GYM006', 1, '물비늘', 'SEA', 'A', 'SEA_MIST_BREATH', 'SEA_RIPPLE_STEP'
    union all
    select 'GYM006', 2, '은하', 'SPACE', 'S', 'SPACE_GALAXY_SPARK', 'SPACE_STAR_SEED'
    union all
    select 'GYM006', 3, '노을', 'SKY', 'S', 'SKY_HALO_GLOW', 'SKY_CLOUD_BURST'
    union all
    -- GYM007 순환 관장 하울 / 상성 순환 보완 / S·S·SS
    select 'GYM007', 1, '혜성', 'SPACE', 'S', 'SPACE_COSMIC_DUST', 'SPACE_METEOR_TAIL'
    union all
    select 'GYM007', 2, '심해', 'SEA', 'S', 'SEA_DEEP_BLUE', 'SEA_WHIRLPOOL'
    union all
    select 'GYM007', 3, '대지', 'GROUND', 'SS', 'GROUND_ROOT_BIND', 'GROUND_TAIL_SEED'
    union all
    -- GYM008 챔피언 아스트라 / 완성형 / S·SS·SSS
    select 'GYM008', 1, '창공', 'SKY', 'S', 'SKY_TWINKLE_EYE', 'SKY_UPDRAFT'
    union all
    select 'GYM008', 2, '해류', 'SEA', 'SS', 'SEA_MARINE_CALL', 'SEA_AQUA_FLASH'
    union all
    select 'GYM008', 3, '루메르', 'SPACE', 'SSS', 'SPACE_UNIVERSE_CALL', 'SPACE_DIMENSION_SKIP'
) v on v.leader_code = l.code
where not exists (
    select 1 from gym_leader_animals a where a.gym_leader_id = l.id and a.order_no = v.order_no
);

-- gym_leaders.image_key / gym_leader_animals.image_key / badges.image_key 는 디자인 에셋 업로드 후 update 로 채운다.
