-- 기존 리프레시 토큰이 유효한 경우에만 기존 토큰을 제거하고
-- 새 리프레시 토큰을 저장한다.
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

-- 잘못된 호출로 동일한 토큰을 다시 저장하는 것을 방지한다.
if oldTokenHash == newTokenHash then
    return 0
end

-- 만료된 토큰을 먼저 정리한다.
redis.call('ZREMRANGEBYSCORE', key, '-inf', now)

-- 만료 토큰은 위에서 제거되었으므로, 존재 여부만 확인하면 된다.
local oldExpiresAt = redis.call('ZSCORE', key, oldTokenHash)

if not oldExpiresAt then
    return 0
end

-- 사용된 기존 토큰만 제거한다.
-- 같은 사용자의 다른 리프레시 토큰은 유지된다.
redis.call('ZREM', key, oldTokenHash)

-- 새 토큰을 저장한다.
redis.call('ZADD', key, newExpiresAt, newTokenHash)

-- 사용자에게 남아 있는 토큰 중 가장 늦은 만료 시각을 구한다.
local latest = redis.call(
    'ZRANGE',
    key,
    -1,
    -1,
    'WITHSCORES'
)

-- 사용자별 키의 만료 시각을 가장 늦은 토큰에 맞춘다.
if #latest >= 2 then
    redis.call('PEXPIREAT', key, tonumber(latest[2]))
end

return 1
