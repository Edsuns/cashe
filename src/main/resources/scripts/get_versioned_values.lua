local half = math.floor(#KEYS / 2)
local values = redis.call('MGET', unpack(KEYS, 1, half))
local versions = redis.call('MGET', unpack(KEYS, half + 1, #KEYS))
local versionedValues = {}
for i = 1, #values do
  table.insert(versionedValues, { values[i], versions[i] })
end
return versionedValues