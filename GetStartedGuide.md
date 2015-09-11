# Installation #
First download the latest build from https://github.com/QuickServerLab/QuickCached/archive/v2.0.0.zip

Also code  available at GitHub

https://github.com/QuickServerLab/QuickCached

## Requirements ##
QuickCached is a memcached server implementation in Java, so will work on any OS that supports Java VM. QuickCached needs Java VM v 1.5 or higher installed.

# Windows #
First extract the archive to a directory. Say "d:\quickcached". Now open a command prompt and type the following.

```
c:\>d:
d:\>cd d:\quickcached
D:\QuickCached>QuickCached.bat -p 11211 -l 0.0.0.0
Loading QuickServer v1.4.8 [PID:0000]... Done

-------------------------------
 Name : QuickCached
 Port : 11211
-------------------------------
```

# Linux/Solaris/HP-Unix/AIX #
First extract the archive to a directory. Say "/home/user/quickcached". Now open a command prompt and type the following.
```
$ cd /home/user/quickcached
$ sh QuickCached.sh -p 11211 -l 0.0.0.0
Loading QuickServer v1.4.8 [PID:0000]... Done

-------------------------------
 Name : QuickCached
 Port : 11211
-------------------------------
```

# Memory Sizing #
By default QuickCached will use maximum of 512MB of RAM, if you need it to use different amount, just edit the .bat file or .sh file and change the `-xMX` value.

One can also run multiple instances on the same system or different systems across the network to archive more available memory/cache. Running multiple instances is always recommended over running one big instance.

# Testing #
```
$ telnet localhost 11211
Trying ::1...
Connected to localhost.
Escape character is '^]'.
get key1
END
set key1 0 3600 4 (Note: TTL 3600 and 4 bytes)
ABCD
STORED
get key1 
VALUE key1 0 4
ABCD
END
```


# Client API #
Client API's / libraries - http://code.google.com/p/memcached/wiki/Clients

# Using in your Code #
Here is a nice article on how to use Quickcached/Memcached in Java http://sacharya.com/using-memcached-with-java/