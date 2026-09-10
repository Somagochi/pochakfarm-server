-- 관장 소개 콘텐츠 컬럼 마이그레이션
-- 선행: battle-init-schema.sql (gym_leaders 테이블이 이미 있어야 한다)
-- 기존 테이블 변경: gym_leaders 에 leader_type, difficulty, leader_description, tip_description 추가
--   leader_type 은 관장 주력 타입이며, 상세 응답의 추천 타입(suggestType)은 이 값의 상성 우위 타입으로 계산한다.
--   난이도 라벨과 소개·팁 문구는 기획 확정 전이라 값 없이 컬럼만 열어 두고 확정 후 update 로 채운다.
-- 컬럼 정의는 JPA 매핑이 MySQLDialect 로 생성하는 DDL 과 동일하다.
--   운영 프로파일의 ddl-auto 가 validate 이므로 타입이 어긋나면 기동이 실패한다.
-- information_schema 가드로 재실행해도 안전하다.

set @gym_leaders_leader_type_ddl := (
    select if(
        count(*) = 0,
        'alter table gym_leaders
            add column leader_type enum (''GROUND'',''SEA'',''SKY'',''SPACE'')',
        'select 1')
    from information_schema.columns
    where table_schema = database()
      and table_name = 'gym_leaders'
      and column_name = 'leader_type');
prepare gym_leaders_leader_type_stmt from @gym_leaders_leader_type_ddl;
execute gym_leaders_leader_type_stmt;
deallocate prepare gym_leaders_leader_type_stmt;

set @gym_leaders_difficulty_ddl := (
    select if(
        count(*) = 0,
        'alter table gym_leaders
            add column difficulty varchar(255)',
        'select 1')
    from information_schema.columns
    where table_schema = database()
      and table_name = 'gym_leaders'
      and column_name = 'difficulty');
prepare gym_leaders_difficulty_stmt from @gym_leaders_difficulty_ddl;
execute gym_leaders_difficulty_stmt;
deallocate prepare gym_leaders_difficulty_stmt;

set @gym_leaders_leader_description_ddl := (
    select if(
        count(*) = 0,
        'alter table gym_leaders
            add column leader_description varchar(255)',
        'select 1')
    from information_schema.columns
    where table_schema = database()
      and table_name = 'gym_leaders'
      and column_name = 'leader_description');
prepare gym_leaders_leader_description_stmt from @gym_leaders_leader_description_ddl;
execute gym_leaders_leader_description_stmt;
deallocate prepare gym_leaders_leader_description_stmt;

set @gym_leaders_tip_description_ddl := (
    select if(
        count(*) = 0,
        'alter table gym_leaders
            add column tip_description varchar(255)',
        'select 1')
    from information_schema.columns
    where table_schema = database()
      and table_name = 'gym_leaders'
      and column_name = 'tip_description');
prepare gym_leaders_tip_description_stmt from @gym_leaders_tip_description_ddl;
execute gym_leaders_tip_description_stmt;
deallocate prepare gym_leaders_tip_description_stmt;
