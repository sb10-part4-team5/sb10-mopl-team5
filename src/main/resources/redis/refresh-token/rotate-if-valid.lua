-- 기존 리프레시 토큰이 유효한 경우에만 기존 토큰을 폐기하고
-- 새 리프레시 토큰 하나만 저장한다.
--
-- KEYS[1]: 사용자별 리프레시 토큰 키
--
-- ARGV[1]: 현재 시간(epoch milliseconds)
-- ARGV[2]: 기존 리프레시 토큰 해시
-- ARGV[3]: 새 리프레시 토큰 해시
-- ARGV[4]: 새 토큰 만료 시간(epoch milliseconds)
--
-- 반환값:
-- 1: 로테이션 성공
-- 0: 기존 토큰이 없거나 만료되었거나 새 만료 시간이 유효하지 않음

local key = KEYS[1]

local now = tonumber(ARGV[1])
local oldTokenHash = ARGV[2]
local newTokenHash = ARGV[3]
local newExpiresAt = tonumber(ARGV[4])

if newExpiresAt <= now then
    return 0
end

if oldTokenHash == newTokenHash then
    return 0
end

-- 만료된 토큰을 먼저 정리한다.
redis.call('ZREMRANGEBYSCORE', key, '-inf', now)

-- 기존 토큰이 현재 유효한지 확인한다.
local oldExpiresAt = redis.call('ZSCORE', key, oldTokenHash)

if not oldExpiresAt then
    return 0
end

-- 사용자에게 저장된 기존 토큰을 모두 폐기한다.
redis.call('DEL', key)

-- 새 토큰 하나만 저장한다.
redis.call('ZADD', key, newExpiresAt, newTokenHash)

-- 저장된 유일한 토큰의 만료 시각에 키 만료 시각을 맞춘다.
redis.call('PEXPIREAT', key, newExpiresAt)

return 1
