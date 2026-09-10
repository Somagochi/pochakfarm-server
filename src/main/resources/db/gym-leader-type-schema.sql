-- 관장 주력 타입 enum 확장 마이그레이션
-- 선행: gym-leader-content-schema.sql (gym_leaders.leader_type 컬럼이 이미 있어야 한다)
-- 기존 테이블 변경: gym_leaders.leader_type 을 CardType 에서 GymLeaderType 으로 바꾼다.
--   GymLeaderType 은 CardType 4종에 혼합 편성을 뜻하는 MIXED 를 더한 값이며,
--   MIXED 인 관장은 상세 응답의 추천 타입(suggestType)도 MIXED 라벨(복합)로 내려준다.
-- 컬럼 정의는 JPA 매핑이 MySQLDialect 로 생성하는 DDL 과 동일하다.
--   운영 프로파일의 ddl-auto 가 validate 이므로 타입이 어긋나면 기동이 실패한다.
-- information_schema 가드로 재실행해도 안전하다.

set @gym_leaders_leader_type_ddl := (
    select if(
        count(*) = 1,
        'alter table gym_leaders
            modify column leader_type enum (''GROUND'',''MIXED'',''SEA'',''SKY'',''SPACE'')',
        'select 1')
    from information_schema.columns
    where table_schema = database()
      and table_name = 'gym_leaders'
      and column_name = 'leader_type'
      and column_type not like '%MIXED%');
prepare gym_leaders_leader_type_stmt from @gym_leaders_leader_type_ddl;
execute gym_leaders_leader_type_stmt;
deallocate prepare gym_leaders_leader_type_stmt;
