-- 관장 썸네일 이미지 컬럼 마이그레이션
-- 선행: battle-init-schema.sql (gym_leaders 테이블이 이미 있어야 한다)
-- 기존 테이블 변경: gym_leaders.thumbnail_key 컬럼 추가
--   image_key 는 상세 화면용 원본 이미지, thumbnail_key 는 목록 화면용 썸네일이다.
-- 컬럼 정의는 JPA 매핑이 MySQLDialect 로 생성하는 DDL 과 동일하다.
--   운영 프로파일의 ddl-auto 가 validate 이므로 타입이 어긋나면 기동이 실패한다.
-- information_schema 가드로 재실행해도 안전하다.

set @gym_leaders_thumbnail_key_ddl := (
    select if(
        count(*) = 0,
        'alter table gym_leaders
            add column thumbnail_key varchar(255)',
        'select 1')
    from information_schema.columns
    where table_schema = database()
      and table_name = 'gym_leaders'
      and column_name = 'thumbnail_key');
prepare gym_leaders_thumbnail_key_stmt from @gym_leaders_thumbnail_key_ddl;
execute gym_leaders_thumbnail_key_stmt;
deallocate prepare gym_leaders_thumbnail_key_stmt;
