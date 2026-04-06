create table operational_audit_event (
    id bigserial primary key,
    actor_user_id bigint null,
    actor_username varchar(100) not null,
    action varchar(100) not null,
    entity_type varchar(100) not null,
    entity_id varchar(100) null,
    occurred_at timestamptz not null default now(),
    result varchar(20) not null,
    context_json text null
);

create index idx_operational_audit_event_occurred_at
    on operational_audit_event (occurred_at desc);

create index idx_operational_audit_event_action
    on operational_audit_event (action);

create index idx_operational_audit_event_entity_type
    on operational_audit_event (entity_type);

create index idx_operational_audit_event_actor_username
    on operational_audit_event (actor_username);

create index idx_operational_audit_event_result
    on operational_audit_event (result);
