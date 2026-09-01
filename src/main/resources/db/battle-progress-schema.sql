-- 대전 진행 상태 스키마 마이그레이션 (SOMA-204)
-- 선행: battle-init-schema.sql (battles, battle_entries 등 코어 테이블이 이미 있어야 한다)
-- 신규 테이블: battle_actions, battle_broadcast_events
-- 기존 테이블 변경: battles 진행 상태 컬럼 6종 + uk_battles_user_id_client_request_id
-- 컬럼 정의는 JPA 매핑이 MySQLDialect 로 생성하는 DDL 과 동일하다.
--   운영 프로파일의 ddl-auto 가 validate 이므로 타입이 어긋나면 기동이 실패한다.
-- create table if not exists / information_schema 가드로 재실행해도 안전하다.
-- battles 에 이미 행이 있는 환경에서는 client_request_id 와 bar_position 이 not null 이므로
-- 아래 순서대로 nullable 로 추가 -> 기존 행 백필 -> not null 로 조인다.
--   update battles set client_request_id = uuid(), bar_position = 0 where client_request_id is null;
-- 현재 battles 는 아직 서비스에 노출되지 않아 행이 없으므로 곧바로 not null 로 추가한다.

set @battle_progress_columns_ddl := (
    select if(
        count(*) = 0,
        'alter table battles
            add column client_request_id varchar(36) not null,
            add column bar_position integer not null,
            add column last_action_at datetime(6) null,
            add column final_expires_at datetime(6) null,
            add column final_tap_count integer null,
            add column final_move_distance integer null',
        'select 1')
    from information_schema.columns
    where table_schema = database()
      and table_name = 'battles'
      and column_name = 'client_request_id');
prepare battle_progress_columns_stmt from @battle_progress_columns_ddl;
execute battle_progress_columns_stmt;
deallocate prepare battle_progress_columns_stmt;

-- 대전 생성 멱등성 키 (SOMA-204 2.3). uk_captures_user_id_client_request_id 와 같은 형태다.
-- 유저 행 비관적 락의 백스톱이라, 락이 서로 다른 커넥션에 걸려도 DB 가 최종 방어선을 잡는다.
set @battle_client_request_uk_ddl := (
    select if(
        count(*) = 0,
        'alter table battles
            add constraint uk_battles_user_id_client_request_id unique (user_id, client_request_id)',
        'select 1')
    from information_schema.statistics
    where table_schema = database()
      and table_name = 'battles'
      and index_name = 'uk_battles_user_id_client_request_id');
prepare battle_client_request_uk_stmt from @battle_client_request_uk_ddl;
execute battle_client_request_uk_stmt;
deallocate prepare battle_client_request_uk_stmt;

create table if not exists battle_actions (
    action_no_in_entry integer not null,
    action_seq integer not null,
    bar_position_after integer not null,
    bar_position_before integer not null,
    entry_order integer not null,
    net_move_distance integer not null,
    npc_move_distance integer not null,
    npc_skill_triggered bit not null,
    user_move_distance integer not null,
    user_skill_triggered bit not null,
    battle_id bigint not null,
    created_at datetime(6) not null,
    deleted_at datetime(6),
    id bigint not null auto_increment,
    updated_at datetime(6) not null,
    npc_skill enum ('GROUND_ACORN_SHOT','GROUND_BARK_SHIELD','GROUND_BERRY_BITE','GROUND_BLOSSOM_HEAL','GROUND_CLOVER_LUCK','GROUND_EARTH_SHAKE','GROUND_FOREST_HIDE','GROUND_GARDEN_REST','GROUND_GRASS_ROLL','GROUND_GREEN_FLASH','GROUND_HARVEST_BOOST','GROUND_HILL_JUMP','GROUND_HONEY_TRAP','GROUND_LEAF_GUARD','GROUND_LEAF_SLASH','GROUND_LOG_ROLL','GROUND_MOSS_CUSHION','GROUND_MOUNTAIN_POSE','GROUND_MUD_DASH','GROUND_NATURE_CALL','GROUND_PAW_STRIKE','GROUND_POLLEN_SPARK','GROUND_ROOT_BIND','GROUND_SOIL_CHARGE','GROUND_SPROUT_STEP','GROUND_STONE_TAP','GROUND_SUNNY_NAP','GROUND_TAIL_SEED','GROUND_VINE_PULL','GROUND_WILD_SNIFF','SEA_AQUA_FLASH','SEA_AQUA_HEAL','SEA_BLUE_CURRENT','SEA_BUBBLE_GUARD','SEA_CORAL_HIDE','SEA_DEEP_BLUE','SEA_DOLPHIN_STEP','SEA_DRIFT_FLOAT','SEA_FOAM_ROLL','SEA_ICE_SPLASH','SEA_KELP_WRAP','SEA_LAGOON_REST','SEA_MARINE_CALL','SEA_MIST_BREATH','SEA_OCEAN_NAP','SEA_PEARL_SHOT','SEA_RAIN_DROP','SEA_REEF_JUMP','SEA_RIPPLE_STEP','SEA_SALT_SPARK','SEA_SEASHELL_SHIELD','SEA_SHELL_BITE','SEA_SPLASH_PAW','SEA_STREAM_CUT','SEA_TIDAL_BOOST','SEA_TIDE_PULL','SEA_TURTLE_GUARD','SEA_WATER_TAIL','SEA_WAVE_DASH','SEA_WHIRLPOOL','SKY_AERIAL_POUNCE','SKY_AIR_SPIN','SKY_BIRDSONG_CALL','SKY_BLUE_FLASH','SKY_BREEZE_HEAL','SKY_CLEAR_MIND','SKY_CLOUD_BURST','SKY_CLOUD_CUSHION','SKY_CLOUD_HIDE','SKY_CLOUD_JUMP','SKY_COTTON_SHIELD','SKY_FEATHER_GUARD','SKY_FLOATING_STEP','SKY_GUST_PUSH','SKY_HALO_GLOW','SKY_HIGH_NOSE','SKY_LIGHT_RAINDROP','SKY_LIGHT_WING','SKY_MIST_WRAP','SKY_RAINBOW_TAIL','SKY_SKYLINE_RUN','SKY_SKY_PAW','SKY_SOFT_LANDING','SKY_STAR_BALLOON','SKY_SUNBEAM','SKY_TAILWIND','SKY_TWINKLE_EYE','SKY_UPDRAFT','SKY_WIND_CHIME','SKY_WIND_DASH','SPACE_ALIEN_WINK','SPACE_ASTRO_SHIELD','SPACE_AURORA_WAVE','SPACE_BLACKHOLE_PULL','SPACE_COMET_DASH','SPACE_COSMIC_DUST','SPACE_COSMIC_PAW','SPACE_DIMENSION_SKIP','SPACE_ECLIPSE_POSE','SPACE_GALAXY_SPARK','SPACE_GRAVITY_HOLD','SPACE_LUNAR_NAP','SPACE_METEOR_TAIL','SPACE_MILKY_WAY','SPACE_MOONBEAM','SPACE_MOON_GUARD','SPACE_NEBULA_HIDE','SPACE_NOVA_FLASH','SPACE_ORBIT_STEP','SPACE_PLANET_BOUNCE','SPACE_PULSAR_BEAT','SPACE_ROCKET_JUMP','SPACE_SATELLITE_SCAN','SPACE_STARDUST_HEAL','SPACE_STAR_PUNCH','SPACE_STAR_RING','SPACE_STAR_SEED','SPACE_UNIVERSE_CALL','SPACE_VOID_STEP','SPACE_ZERO_GRAVITY') not null,
    user_skill enum ('GROUND_ACORN_SHOT','GROUND_BARK_SHIELD','GROUND_BERRY_BITE','GROUND_BLOSSOM_HEAL','GROUND_CLOVER_LUCK','GROUND_EARTH_SHAKE','GROUND_FOREST_HIDE','GROUND_GARDEN_REST','GROUND_GRASS_ROLL','GROUND_GREEN_FLASH','GROUND_HARVEST_BOOST','GROUND_HILL_JUMP','GROUND_HONEY_TRAP','GROUND_LEAF_GUARD','GROUND_LEAF_SLASH','GROUND_LOG_ROLL','GROUND_MOSS_CUSHION','GROUND_MOUNTAIN_POSE','GROUND_MUD_DASH','GROUND_NATURE_CALL','GROUND_PAW_STRIKE','GROUND_POLLEN_SPARK','GROUND_ROOT_BIND','GROUND_SOIL_CHARGE','GROUND_SPROUT_STEP','GROUND_STONE_TAP','GROUND_SUNNY_NAP','GROUND_TAIL_SEED','GROUND_VINE_PULL','GROUND_WILD_SNIFF','SEA_AQUA_FLASH','SEA_AQUA_HEAL','SEA_BLUE_CURRENT','SEA_BUBBLE_GUARD','SEA_CORAL_HIDE','SEA_DEEP_BLUE','SEA_DOLPHIN_STEP','SEA_DRIFT_FLOAT','SEA_FOAM_ROLL','SEA_ICE_SPLASH','SEA_KELP_WRAP','SEA_LAGOON_REST','SEA_MARINE_CALL','SEA_MIST_BREATH','SEA_OCEAN_NAP','SEA_PEARL_SHOT','SEA_RAIN_DROP','SEA_REEF_JUMP','SEA_RIPPLE_STEP','SEA_SALT_SPARK','SEA_SEASHELL_SHIELD','SEA_SHELL_BITE','SEA_SPLASH_PAW','SEA_STREAM_CUT','SEA_TIDAL_BOOST','SEA_TIDE_PULL','SEA_TURTLE_GUARD','SEA_WATER_TAIL','SEA_WAVE_DASH','SEA_WHIRLPOOL','SKY_AERIAL_POUNCE','SKY_AIR_SPIN','SKY_BIRDSONG_CALL','SKY_BLUE_FLASH','SKY_BREEZE_HEAL','SKY_CLEAR_MIND','SKY_CLOUD_BURST','SKY_CLOUD_CUSHION','SKY_CLOUD_HIDE','SKY_CLOUD_JUMP','SKY_COTTON_SHIELD','SKY_FEATHER_GUARD','SKY_FLOATING_STEP','SKY_GUST_PUSH','SKY_HALO_GLOW','SKY_HIGH_NOSE','SKY_LIGHT_RAINDROP','SKY_LIGHT_WING','SKY_MIST_WRAP','SKY_RAINBOW_TAIL','SKY_SKYLINE_RUN','SKY_SKY_PAW','SKY_SOFT_LANDING','SKY_STAR_BALLOON','SKY_SUNBEAM','SKY_TAILWIND','SKY_TWINKLE_EYE','SKY_UPDRAFT','SKY_WIND_CHIME','SKY_WIND_DASH','SPACE_ALIEN_WINK','SPACE_ASTRO_SHIELD','SPACE_AURORA_WAVE','SPACE_BLACKHOLE_PULL','SPACE_COMET_DASH','SPACE_COSMIC_DUST','SPACE_COSMIC_PAW','SPACE_DIMENSION_SKIP','SPACE_ECLIPSE_POSE','SPACE_GALAXY_SPARK','SPACE_GRAVITY_HOLD','SPACE_LUNAR_NAP','SPACE_METEOR_TAIL','SPACE_MILKY_WAY','SPACE_MOONBEAM','SPACE_MOON_GUARD','SPACE_NEBULA_HIDE','SPACE_NOVA_FLASH','SPACE_ORBIT_STEP','SPACE_PLANET_BOUNCE','SPACE_PULSAR_BEAT','SPACE_ROCKET_JUMP','SPACE_SATELLITE_SCAN','SPACE_STARDUST_HEAL','SPACE_STAR_PUNCH','SPACE_STAR_RING','SPACE_STAR_SEED','SPACE_UNIVERSE_CALL','SPACE_VOID_STEP','SPACE_ZERO_GRAVITY') not null,
    primary key (id),
    unique key uk_battle_actions_battle_id_action_seq (battle_id, action_seq)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists battle_broadcast_events (
    action_seq integer,
    entry_order integer,
    event_seq integer not null,
    param_points integer,
    battle_id bigint not null,
    created_at datetime(6) not null,
    deleted_at datetime(6),
    id bigint not null auto_increment,
    updated_at datetime(6) not null,
    event_code enum ('BATTLE_POINT_APPLIED','SKILL_FAILED','SKILL_NOT_SELECTED','SKILL_OFFSET','SKILL_TRIGGERED','TIER_ADVANTAGE','TYPE_ADVANTAGE') not null,
    param_animal_side enum ('NPC','USER'),
    param_skill enum ('GROUND_ACORN_SHOT','GROUND_BARK_SHIELD','GROUND_BERRY_BITE','GROUND_BLOSSOM_HEAL','GROUND_CLOVER_LUCK','GROUND_EARTH_SHAKE','GROUND_FOREST_HIDE','GROUND_GARDEN_REST','GROUND_GRASS_ROLL','GROUND_GREEN_FLASH','GROUND_HARVEST_BOOST','GROUND_HILL_JUMP','GROUND_HONEY_TRAP','GROUND_LEAF_GUARD','GROUND_LEAF_SLASH','GROUND_LOG_ROLL','GROUND_MOSS_CUSHION','GROUND_MOUNTAIN_POSE','GROUND_MUD_DASH','GROUND_NATURE_CALL','GROUND_PAW_STRIKE','GROUND_POLLEN_SPARK','GROUND_ROOT_BIND','GROUND_SOIL_CHARGE','GROUND_SPROUT_STEP','GROUND_STONE_TAP','GROUND_SUNNY_NAP','GROUND_TAIL_SEED','GROUND_VINE_PULL','GROUND_WILD_SNIFF','SEA_AQUA_FLASH','SEA_AQUA_HEAL','SEA_BLUE_CURRENT','SEA_BUBBLE_GUARD','SEA_CORAL_HIDE','SEA_DEEP_BLUE','SEA_DOLPHIN_STEP','SEA_DRIFT_FLOAT','SEA_FOAM_ROLL','SEA_ICE_SPLASH','SEA_KELP_WRAP','SEA_LAGOON_REST','SEA_MARINE_CALL','SEA_MIST_BREATH','SEA_OCEAN_NAP','SEA_PEARL_SHOT','SEA_RAIN_DROP','SEA_REEF_JUMP','SEA_RIPPLE_STEP','SEA_SALT_SPARK','SEA_SEASHELL_SHIELD','SEA_SHELL_BITE','SEA_SPLASH_PAW','SEA_STREAM_CUT','SEA_TIDAL_BOOST','SEA_TIDE_PULL','SEA_TURTLE_GUARD','SEA_WATER_TAIL','SEA_WAVE_DASH','SEA_WHIRLPOOL','SKY_AERIAL_POUNCE','SKY_AIR_SPIN','SKY_BIRDSONG_CALL','SKY_BLUE_FLASH','SKY_BREEZE_HEAL','SKY_CLEAR_MIND','SKY_CLOUD_BURST','SKY_CLOUD_CUSHION','SKY_CLOUD_HIDE','SKY_CLOUD_JUMP','SKY_COTTON_SHIELD','SKY_FEATHER_GUARD','SKY_FLOATING_STEP','SKY_GUST_PUSH','SKY_HALO_GLOW','SKY_HIGH_NOSE','SKY_LIGHT_RAINDROP','SKY_LIGHT_WING','SKY_MIST_WRAP','SKY_RAINBOW_TAIL','SKY_SKYLINE_RUN','SKY_SKY_PAW','SKY_SOFT_LANDING','SKY_STAR_BALLOON','SKY_SUNBEAM','SKY_TAILWIND','SKY_TWINKLE_EYE','SKY_UPDRAFT','SKY_WIND_CHIME','SKY_WIND_DASH','SPACE_ALIEN_WINK','SPACE_ASTRO_SHIELD','SPACE_AURORA_WAVE','SPACE_BLACKHOLE_PULL','SPACE_COMET_DASH','SPACE_COSMIC_DUST','SPACE_COSMIC_PAW','SPACE_DIMENSION_SKIP','SPACE_ECLIPSE_POSE','SPACE_GALAXY_SPARK','SPACE_GRAVITY_HOLD','SPACE_LUNAR_NAP','SPACE_METEOR_TAIL','SPACE_MILKY_WAY','SPACE_MOONBEAM','SPACE_MOON_GUARD','SPACE_NEBULA_HIDE','SPACE_NOVA_FLASH','SPACE_ORBIT_STEP','SPACE_PLANET_BOUNCE','SPACE_PULSAR_BEAT','SPACE_ROCKET_JUMP','SPACE_SATELLITE_SCAN','SPACE_STARDUST_HEAL','SPACE_STAR_PUNCH','SPACE_STAR_RING','SPACE_STAR_SEED','SPACE_UNIVERSE_CALL','SPACE_VOID_STEP','SPACE_ZERO_GRAVITY'),
    param_winner_side enum ('NPC','USER'),
    primary key (id),
    unique key uk_battle_broadcast_events_battle_id_event_seq (battle_id, event_seq)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;
