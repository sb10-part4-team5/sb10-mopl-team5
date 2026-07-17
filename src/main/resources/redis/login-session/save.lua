redis.call(
    'SET',
    KEYS[1],
    ARGV[1],
    'PXAT',
    ARGV[2]
)

redis.call(
    'ZADD',
    KEYS[2],
    ARGV[2],
    ARGV[1]
)

local sessionExpiresAt = tonumber(ARGV[2])
local indexExpiresAt = redis.call('PEXPIRETIME', KEYS[2])

if indexExpiresAt == -1 or indexExpiresAt < sessionExpiresAt then
    redis.call(
        'PEXPIREAT',
        KEYS[2],
        sessionExpiresAt
    )
end

return 1
