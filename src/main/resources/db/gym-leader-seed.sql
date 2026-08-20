-- NPC 관장 마스터 데이터 시드 (관장 8명, 관장 동물 24마리, 관장 뱃지 8종)
-- 실행 순서: battle-init-schema.sql -> battle-progress-schema.sql -> badges -> gym_leaders -> gym_leader_animals
-- code 는 마이그레이션/운영 스크립트의 식별자이므로 한 번 배포되면 절대 변경하지 않는다.
--   관장: GYM + 3자리 일련번호 / 뱃지: BDG + 3자리 일련번호 (achievement-seed.sql 의 BDG005 다음 번호)
-- badges.code / gym_leaders.code 는 유니크 제약이 있어 재실행 시 에러로 중단된다.
-- gym_leader_animals 는 not exists 가드로 재실행해도 중복 생성되지 않는다.
-- 동물 이름 컬럼은 GymLeaderAnimal 이 @Embedded AnimalName 을 쓰므로 animal_name 이다.
-- 밸런스 재조정으로 편성이 바뀌면 insert 를 추가하지 말고 이 파일 말미에 update 블록을 누적한다.

insert into badges (code, name, description, image_key, created_at, updated_at)
values
    ('BDG006', '새싹 뱃지', '첫 번째 관장 흙담이를 이기고 받았다', null, now(6), now(6)),
    ('BDG007', '물방울 뱃지', '두 번째 관장 물결이를 이기고 받았다', null, now(6), now(6)),
    ('BDG008', '깃털 뱃지', '세 번째 관장 바람솔을 이기고 받았다', null, now(6), now(6)),
    ('BDG009', '별가루 뱃지', '네 번째 관장 별무리를 이기고 받았다', null, now(6), now(6)),
    ('BDG010', '소용돌이 뱃지', '다섯 번째 관장 네바람을 이기고 받았다', null, now(6), now(6)),
    ('BDG011', '오로라 뱃지', '여섯 번째 관장 돌개별을 이기고 받았다', null, now(6), now(6)),
    ('BDG012', '초승달 뱃지', '일곱 번째 관장 하늬달을 이기고 받았다', null, now(6), now(6)),
    ('BDG013', '온누리 뱃지', '여덟 번째 관장 온누리를 이기고 받았다', null, now(6), now(6));

insert into gym_leaders (code, name, challenge_order, badge_code, image_key, created_at, updated_at)
values
    ('GYM001', '흙담이', 1, 'BDG006', null, now(6), now(6)),
    ('GYM002', '물결이', 2, 'BDG007', null, now(6), now(6)),
    ('GYM003', '바람솔', 3, 'BDG008', null, now(6), now(6)),
    ('GYM004', '별무리', 4, 'BDG009', null, now(6), now(6)),
    ('GYM005', '네바람', 5, 'BDG010', null, now(6), now(6)),
    ('GYM006', '돌개별', 6, 'BDG011', null, now(6), now(6)),
    ('GYM007', '하늬달', 7, 'BDG012', null, now(6), now(6)),
    ('GYM008', '온누리', 8, 'BDG013', null, now(6), now(6));

insert into gym_leader_animals (gym_leader_id, order_no, animal_name, card_type, tier, skill_1, skill_2, image_key, created_at, updated_at)
select l.id, v.order_no, v.animal_name, v.card_type, v.tier, v.skill_1, v.skill_2, null, now(6), now(6)
from gym_leaders l
join (
    select 'GYM001' as leader_code, 1 as order_no, '흙방울' as animal_name, 'GROUND' as card_type, 'C' as tier, 'GROUND_MOSS_CUSHION' as skill_1, 'GROUND_STONE_TAP' as skill_2
    union all
    select 'GYM001', 2, '도톨이', 'GROUND', 'C', 'GROUND_SUNNY_NAP', 'GROUND_TAIL_SEED'
    union all
    select 'GYM001', 3, '이끼뿔', 'GROUND', 'B', 'GROUND_BARK_SHIELD', 'GROUND_ROOT_BIND'
    union all
    select 'GYM002', 1, '물방울', 'SEA', 'C', 'SEA_BUBBLE_GUARD', 'SEA_RAIN_DROP'
    union all
    select 'GYM002', 2, '조개돌', 'SEA', 'B', 'SEA_SEASHELL_SHIELD', 'SEA_FOAM_ROLL'
    union all
    select 'GYM002', 3, '파도귀', 'SEA', 'B', 'SEA_OCEAN_NAP', 'SEA_WAVE_DASH'
    union all
    select 'GYM003', 1, '솜깃털', 'SKY', 'B', 'SKY_FEATHER_GUARD', 'SKY_FLOATING_STEP'
    union all
    select 'GYM003', 2, '바람새', 'SKY', 'B', 'SKY_AIR_SPIN', 'SKY_WIND_DASH'
    union all
    select 'GYM003', 3, '잔물결', 'SEA', 'A', 'SEA_DRIFT_FLOAT', 'SEA_WHIRLPOOL'
    union all
    select 'GYM004', 1, '달먼지', 'SPACE', 'B', 'SPACE_COSMIC_DUST', 'SPACE_MOONBEAM'
    union all
    select 'GYM004', 2, '별똥이', 'SPACE', 'A', 'SPACE_STAR_RING', 'SPACE_METEOR_TAIL'
    union all
    select 'GYM004', 3, '뿌리곰', 'GROUND', 'A', 'GROUND_MOUNTAIN_POSE', 'GROUND_EARTH_SHAKE'
    union all
    select 'GYM005', 1, '숲그늘', 'GROUND', 'A', 'GROUND_FOREST_HIDE', 'GROUND_LEAF_SLASH'
    union all
    select 'GYM005', 2, '노을깃', 'SKY', 'A', 'SKY_LIGHT_WING', 'SKY_SUNBEAM'
    union all
    select 'GYM005', 3, '물빛돌', 'SEA', 'S', 'SEA_DEEP_BLUE', 'SEA_AQUA_FLASH'
    union all
    select 'GYM006', 1, '성운이', 'SPACE', 'A', 'SPACE_GALAXY_SPARK', 'SPACE_NOVA_FLASH'
    union all
    select 'GYM006', 2, '심해귀', 'SEA', 'S', 'SEA_ICE_SPLASH', 'SEA_STREAM_CUT'
    union all
    select 'GYM006', 3, '바위솔', 'GROUND', 'S', 'GROUND_HARVEST_BOOST', 'GROUND_LOG_ROLL'
    union all
    select 'GYM007', 1, '하늘결', 'SKY', 'S', 'SKY_TAILWIND', 'SKY_SKYLINE_RUN'
    union all
    select 'GYM007', 2, '흙바람', 'GROUND', 'S', 'GROUND_VINE_PULL', 'GROUND_SOIL_CHARGE'
    union all
    select 'GYM007', 3, '은하솔', 'SPACE', 'SS', 'SPACE_PULSAR_BEAT', 'SPACE_MILKY_WAY'
    union all
    select 'GYM008', 1, '심해빛', 'SEA', 'S', 'SEA_TIDAL_BOOST', 'SEA_BLUE_CURRENT'
    union all
    select 'GYM008', 2, '은하달', 'SPACE', 'SS', 'SPACE_AURORA_WAVE', 'SPACE_COMET_DASH'
    union all
    select 'GYM008', 3, '새벽날', 'SKY', 'SSS', 'SKY_HALO_GLOW', 'SKY_CLOUD_BURST'
) v on v.leader_code = l.code
where not exists (
    select 1 from gym_leader_animals a where a.gym_leader_id = l.id and a.order_no = v.order_no
);

-- gym_leaders.image_key / gym_leader_animals.image_key / badges.image_key 는 디자인 에셋 업로드 후 update 로 채운다.
