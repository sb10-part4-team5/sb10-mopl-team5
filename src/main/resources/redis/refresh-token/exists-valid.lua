-- 주어진 리프레시 토큰이 존재하며 만료되지 않았는지 확인한다.
--
-- KEYS[1]: 사용자별 리프레시 토큰 키
--
-- ARGV[1]: 현재 시간(epoch milliseconds)
-- ARGV[2]: 확인할 리프레시 토큰 해시
--
-- 반환값:
-- 1: 유효한 토큰
-- 0: 존재하지 않거나 만료된 토큰

local key = KEYS[1]

local now = tonumber(ARGV[1])
local tokenHash = ARGV[2]

redis.call('ZREMRANGEBYSCORE', key, '-inf', now)

local expiresAt = redis.call('ZSCORE', key, tokenHash)

if not expiresAt then
    return 0
end

return 1
