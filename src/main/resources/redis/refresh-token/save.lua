-- 사용자별 Sorted Set에 리프레시 토큰을 저장한다.
-- 기존 토큰은 모두 제거하여 사용자당 하나의 활성 토큰만 유지한다.
--
-- KEYS[1]: 사용자별 리프레시 토큰 키
--
-- ARGV[1]: 현재 시간(epoch milliseconds)
-- ARGV[2]: 리프레시 토큰 해시
-- ARGV[3]: 토큰 만료 시간(epoch milliseconds)
--
-- 반환값:
-- 1: 저장 성공
-- 0: 이미 만료된 토큰이므로 저장 실패

local key = KEYS[1]

local now = tonumber(ARGV[1])
local tokenHash = ARGV[2]
local expiresAt = tonumber(ARGV[3])

if expiresAt <= now then
    return 0
end

-- 사용자당 하나의 활성 리프레시 토큰만 유지한다.
redis.call('DEL', key)

-- member: 토큰 해시
-- score: 토큰 만료 시각
redis.call('ZADD', key, expiresAt, tokenHash)

-- 저장된 유일한 토큰의 만료 시각에 키 만료 시각을 맞춘다.
redis.call('PEXPIREAT', key, expiresAt)

return 1
