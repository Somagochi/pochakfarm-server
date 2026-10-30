alter table captures
    add column processing_started_at datetime(6) null after generation_status;

update captures
set processing_started_at = updated_at
where generation_status = 'PROCESSING'
  and card_image is null;

create index idx_captures_generation_status_processing_started_at
    on captures (generation_status, processing_started_at);
