create extension if not exists "uuid-ossp";

create schema if not exists public;
create table public.invitations
(
    invitation_id        uuid     default uuid_generate_v4() not null constraint vendor_invitation_pk primary key,
    invitations_type  text,
    first_name       text,
    last_name        text,
    contact_id       uuid,
    company_name     text,
    company_id       uuid,
    country_abbr     text,
    email            text UNIQUE,
    invited_to       text,
    invitation_status         text,
    invitation_sent_at        timestamp,
    invitation_accepted_at    timestamp,
    created_at                timestamp default now() not null,
    state_province_abbr       text
);


