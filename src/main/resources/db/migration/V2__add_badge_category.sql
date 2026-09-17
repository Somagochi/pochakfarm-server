alter table badges
    add column category enum ('ACHIEVEMENT','GYM_LEADER') null after code;

update badges b
set b.category = case
    when exists (select 1 from gym_leaders g where g.badge_code = b.code) then 'GYM_LEADER'
    else 'ACHIEVEMENT'
end
where b.category is null;

alter table badges
    modify column category enum ('ACHIEVEMENT','GYM_LEADER') not null;
