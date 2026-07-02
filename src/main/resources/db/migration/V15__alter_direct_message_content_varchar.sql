-- DM 본문 길이 제한(1000자)에 맞춰 TEXT -> VARCHAR 변경
ALTER TABLE direct_messages
    ALTER COLUMN content TYPE VARCHAR(1000);
