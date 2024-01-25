local half = math.floor(#KEYS / 2)
local versions = redis.call('MGET', unpack(KEYS, half + 1, #KEYS))
local affected = 0
for i = 1, #versions do
  redis.call('DEL', KEYS[i])
  local version = tonumber(versions[i])
  if version == 9223372036854775807 then
    -- prevent overflow
    redis.call('SET', KEYS[half + i], 1)
  else
    redis.call('INCR', KEYS[half + i])
  end
  affected = affected + 1
end
return affected