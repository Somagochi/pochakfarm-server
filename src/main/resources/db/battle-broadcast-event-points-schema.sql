-- 중계 이벤트 용어·코드 마이그레이션 (SOMA-211)
-- 선행: battle-progress-schema.sql
-- TERRITORY_EXPANDED를 BATTLE_POINT_APPLIED로 변경하고,
-- 미선택 이벤트와 param_points 컬럼을 추가한다.

set @battle_event_expand_codes_ddl := (
    select if(
        count(*) = 1,
        'alter table battle_broadcast_events
            modify column event_code enum (
                ''BATTLE_POINT_APPLIED'',''SKILL_FAILED'',''SKILL_NOT_SELECTED'',
                ''SKILL_OFFSET'',''SKILL_TRIGGERED'',''TERRITORY_EXPANDED'',
                ''TIER_ADVANTAGE'',''TYPE_ADVANTAGE'') not null',
        'select 1')
    from information_schema.tables
    where table_schema = database()
      and table_name = 'battle_broadcast_events');
prepare battle_event_expand_codes_stmt from @battle_event_expand_codes_ddl;
execute battle_event_expand_codes_stmt;
deallocate prepare battle_event_expand_codes_stmt;

set @battle_event_convert_code_ddl := (
    select if(
        count(*) = 1,
        'update battle_broadcast_events
            set event_code = ''BATTLE_POINT_APPLIED''
            where event_code = ''TERRITORY_EXPANDED''',
        'select 1')
    from information_schema.tables
    where table_schema = database()
      and table_name = 'battle_broadcast_events');
prepare battle_event_convert_code_stmt from @battle_event_convert_code_ddl;
execute battle_event_convert_code_stmt;
deallocate prepare battle_event_convert_code_stmt;

set @battle_event_points_column_ddl := (
    select if(
        count(*) = 1,
        'alter table battle_broadcast_events
            rename column param_distance to param_points',
        'select 1')
    from information_schema.columns
    where table_schema = database()
      and table_name = 'battle_broadcast_events'
      and column_name = 'param_distance');
prepare battle_event_points_column_stmt from @battle_event_points_column_ddl;
execute battle_event_points_column_stmt;
deallocate prepare battle_event_points_column_stmt;

set @battle_event_final_codes_ddl := (
    select if(
        count(*) = 1,
        'alter table battle_broadcast_events
            modify column event_code enum (
                ''BATTLE_POINT_APPLIED'',''SKILL_FAILED'',''SKILL_NOT_SELECTED'',
                ''SKILL_OFFSET'',''SKILL_TRIGGERED'',''TIER_ADVANTAGE'',
                ''TYPE_ADVANTAGE'') not null',
        'select 1')
    from information_schema.tables
    where table_schema = database()
      and table_name = 'battle_broadcast_events');
prepare battle_event_final_codes_stmt from @battle_event_final_codes_ddl;
execute battle_event_final_codes_stmt;
deallocate prepare battle_event_final_codes_stmt;
