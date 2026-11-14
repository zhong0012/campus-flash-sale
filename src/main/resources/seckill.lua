-- KEYS[1]: 库存key  KEYS[2]: 用户去重key
-- ARGV[1]: 用户ID
local stock = tonumber(redis.call('get', KEYS[1]))
if stock == nil or stock <= 0 then
    return 0 -- 库存不足
end

local isBought = redis.call('sismember', KEYS[2], ARGV[1])
if isBought == 1 then
    return -1 -- 重复抢购
end

redis.call('decr', KEYS[1])
redis.call('sadd', KEYS[2], ARGV[1])
return 1 -- 扣减成功