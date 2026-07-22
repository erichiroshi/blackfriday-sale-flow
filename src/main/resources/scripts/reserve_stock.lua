-- KEYS[1] = stock counter key (e.g. "stock:PRODUCT-1")
--
-- Returns:
--   -2  key does not exist (product was never seeded / initialized)
--   -1  out of stock (decrement would have gone negative; compensated)
--   >=0 remaining stock after a successful reservation
--
-- Runs atomically: under 10k concurrent callers, DECR alone can race past
-- zero before any client reads the value. Doing the check-and-compensate
-- inside the same EVAL removes that race entirely.
local exists = redis.call('EXISTS', KEYS[1])
if exists == 0 then
  return -2
end

local remaining = redis.call('DECR', KEYS[1])
if remaining < 0 then
  redis.call('INCR', KEYS[1])
  return -1
end

return remaining
