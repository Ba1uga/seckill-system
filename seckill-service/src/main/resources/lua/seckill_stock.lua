local stock = redis.call('GET', KEYS[1])

if (not stock) then
    return -2
end

stock = tonumber(stock)

if (not stock) then
    return -2
end

if (stock <= 0) then
    return -1
end

return redis.call('DECR', KEYS[1])
