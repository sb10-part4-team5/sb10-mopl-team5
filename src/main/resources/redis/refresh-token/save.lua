-- 사용자별 Sorted Set에 리프레시 토큰을 추가한다.
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

-- 만료된 토큰을 지연 정리한다.
redis.call('ZREMRANGEBYSCORE', key, '-inf', now)

-- member: 토큰 해시
-- score: 토큰 만료 시각
redis.call('ZADD', key, expiresAt, tokenHash)

-- 사용자에게 저장된 토큰 중 가장 늦은 만료 시각에
-- 사용자별 Redis 키의 만료 시각을 맞춘다.
local latest = redis.call(
    'ZRANGE',
    key,
    -1,
    -1,
    'WITHSCORES'
)

if #latest >= 2 then
    redis.call('PEXPIREAT', key, tonumber(latest[2]))
end

return 1
