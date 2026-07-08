-- PostgreSQL용 Spring Modulith Outbox 테이블 규격
CREATE TABLE IF NOT EXISTS event_publication
(
    id               UUID NOT NULL,
    listener_id      TEXT NOT NULL,
    event_type       TEXT NOT NULL,
    serialized_event TEXT NOT NULL,
    publication_date TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date  TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id)
);

create index idx_event_publication_completion_date on event_publication(completion_date);

-- 유저(users)를 참조하던 외래키 제약조건 삭제
ALTER TABLE playlist_subscriptions DROP CONSTRAINT IF EXISTS fk_sub_subscriber;
alter table playlist_subscriptions
    add constraint fk_playlist_subscriptions_user_id foreign key (subscriber_id) references users(id);
