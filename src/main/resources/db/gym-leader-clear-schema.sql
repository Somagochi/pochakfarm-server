-- 관장 최초 클리어 기록 스키마 마이그레이션 (SOMA-213)
-- 사용자-관장 유일성으로 보상 중복 지급을 막고, 최초 승리 시점의 보상·성장 결과를 보존한다.

create table if not exists gym_leader_clears (
    experience_reward bigint not null,
    gym_leader_coin_reward bigint not null,
    level_before integer not null,
    level_after integer not null,
    experience_after bigint not null,
    required_experience_for_next_level bigint not null,
    level_up_coin_reward bigint not null,
    coins_after bigint not null,
    battle_id bigint not null,
    gym_leader_id bigint not null,
    user_id bigint not null,
    created_at datetime(6) not null,
    deleted_at datetime(6),
    id bigint not null auto_increment,
    updated_at datetime(6) not null,
    badge_code varchar(64) not null,
    primary key (id),
    unique key uk_gym_leader_clears_user_leader (user_id, gym_leader_id),
    unique key uk_gym_leader_clears_battle_id (battle_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;
