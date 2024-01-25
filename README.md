# cashe

## technology design

### single-flight (one database query per service instance)

multiple threads querying the same data at the same time, the threads will be serialized due to database locks,
which eventually results in multiple times of response time and increasing burden on database access.
to solve this problem, the first thread query the data, the other threads will wait for it and reuse
its returned value.

keeping the return value in memory with a timeout can provide its utilization, which is not taken because it
introduces additional memory consumption and don't keep return value alive is already sufficient for
our caching scenarios. caching return values in memory is the job of a local memory cache.
