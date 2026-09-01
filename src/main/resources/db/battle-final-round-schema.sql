-- 최종 승부 상태 스키마 마이그레이션 (SOMA-213)
-- 선행: battle-progress-schema.sql
-- 재실행해도 안전하며, 기존 final_move_distance 컬럼은 final_points 로 이름을 바꾼다.

set @battle_final_ready_at_ddl := (
    select if(
        count(*) = 0,
        'alter table battles add column final_ready_at datetime(6) null after last_action_at',
        'select 1')
    from information_schema.columns
    where table_schema = database()
      and table_name = 'battles'
      and column_name = 'final_ready_at');
prepare battle_final_ready_at_stmt from @battle_final_ready_at_ddl;
execute battle_final_ready_at_stmt;
deallocate prepare battle_final_ready_at_stmt;

set @battle_final_points_ddl := (
    select case
        when exists (
            select 1 from information_schema.columns
            where table_schema = database()
              and table_name = 'battles'
              and column_name = 'final_points')
            then 'select 1'
        when exists (
            select 1 from information_schema.columns
            where table_schema = database()
              and table_name = 'battles'
              and column_name = 'final_move_distance')
            then 'alter table battles rename column final_move_distance to final_points'
        else 'alter table battles add column final_points integer null after final_tap_count'
    end);
prepare battle_final_points_stmt from @battle_final_points_ddl;
execute battle_final_points_stmt;
deallocate prepare battle_final_points_stmt;
