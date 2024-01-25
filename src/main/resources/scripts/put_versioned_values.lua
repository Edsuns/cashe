local half = math.floor(#KEYS / 2)
local versions = redis.call('MGET', unpack(KEYS, half + 1, #KEYS))
local failedKeys = {}
for i = 1, #versions do
  local version = tonumber(versions[i])
  if tonumber(ARGV[half + i]) == version or version == nil then
    redis.call('SET', KEYS[i], ARGV[i])
    if version == 9223372036854775807 then
      -- prevent overflow
      redis.call('SET', KEYS[half + i], 1)
    else
      redis.call('INCR', KEYS[half + i])
    end
  else
    table.insert(failedKeys, '"' .. KEYS[i] .. '"')
  end
end
return failedKeys