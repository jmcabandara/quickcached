# Introduction #
QuickCached is memcached server implementation in Java based on QuickServer. It is an open source, high-performance, distributed memory object caching system, generic in nature, but intended for use in speeding up dynamic web applications by alleviating database load.

QuickCached is an in-memory key-value store for small chunks of arbitrary data (strings, objects) from results of database calls, API calls, or page rendering.

QuickCached is simple yet powerful. Its simple design promotes quick deployment, ease of development, and solves many problems facing large data caches.


# Details #
Currently Support Following commands
  * Text and Binary Protocol
  * Commands: set, setq, get, getq, delete, deleteq, flush, no-op, version, stats, increment, decrement operations
  * CLI options to set port, IP binding, max connection
  * Plug-able cache implementation.

To be Implemented:
  * UDP support

Check out our [Get Started Guide](http://code.google.com/p/quickcached/wiki/GetStartedGuide) to get going.