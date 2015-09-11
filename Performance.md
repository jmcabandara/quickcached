# Introduction #

Lets look at how QuickCached performance under different client and server settings.


# Details #

Server Configuration
  * Java VM
    1. java version "1.6.0\_26"
    1. Java(TM) SE Runtime Environment (build 1.6.0\_26-b03)
    1. Java HotSpot(TM) 64-Bit Server VM (build 20.1-b02, mixed mode)
  * OS
    1. Ubuntu 11.04 (64-bit)
    1. RAM: 4 GB
    1. CPU: Intel Core 2 Duo (each core - U7300  @ 1.30GHz)

Note: Load test was initiated with the following command "ant loadtest". Each Transaction has 3 operation - set, get and delete commands.
Client is using binary mode of communication. Value payload is 1024 bytes in size.

JVM was warmed by running 200 transaction by running the following command - "ant warmup".

<br />
**Whirlycott Impl with Server running in blocking mode. Client in Binary Mode**<br />


| **Threads** | **No. of Txn** | **Timeout** | **Client** | **Avg Time (ms)** |
|:------------|:---------------|:------------|:-----------|:------------------|
| 10          | 100            | 0           | XMemcached | 1.4               |
| 10          | 100            | 0           | XMemcached | 1.72              |
| 10          | 100            | 0           | XMemcached | 5.15              |
| 1           | 100            | 0           | XMemcached | 3.75              |
| 1           | 100            | 0           | XMemcached | 3.9               |
| 1           | 100            | 0           | XMemcached | 3.75              |
| 20          | 100            | 0           | XMemcached | 1.4               |
| 20          | 100            | 0           | XMemcached | 1.55              |
| 20          | 100            | 0           | XMemcached | 1.4               |

```
Avg Time per Txn: 2.66 ms
Avg Time per Operation: 0.88 ms
```

| **Threads** | **No. of Txn** | **Timeout** | **Client** | **Avg Time (ms)** |
|:------------|:---------------|:------------|:-----------|:------------------|
| 10          | 100            | 0           | SpyMemcached | 1.4               |
| 10          | 100            | 0           | SpyMemcached | 1.57              |
| 10          | 100            | 0           | SpyMemcached | 1.39              |
| 1           | 100            | 0           | SpyMemcached | 3.91              |
| 1           | 100            | 0           | SpyMemcached | 3.91              |
| 1           | 100            | 0           | SpyMemcached | 3.58              |
| 20          | 100            | 0           | SpyMemcached | 2.8               |
| 20          | 100            | 0           | SpyMemcached | 1.39              |
| 20          | 100            | 0           | SpyMemcached | 1.55              |

```
Avg Time per Txn: 2.38 ms
Avg Time per Operation: 0.79 ms
```

<br />
**Whirlycott Impl with Server running in blocking mode. Client in Text Mode**<br />

| **Threads** | **No. of Txn** | **Timeout** | **Client** | **Avg Time (ms)** |
|:------------|:---------------|:------------|:-----------|:------------------|
| 10          | 100            | 0           | XMemcached | 1.1               |
| 10          | 100            | 0           | XMemcached | 1.09              |
| 10          | 100            | 0           | XMemcached | 0.93              |
| 1           | 100            | 0           | XMemcached | 2.5               |
| 1           | 100            | 0           | XMemcached | 2.5               |
| 1           | 100            | 0           | XMemcached | 2.66              |
| 20          | 100            | 0           | XMemcached | 0.93              |
| 20          | 100            | 0           | XMemcached | 0.93              |
| 20          | 100            | 0           | XMemcached | 0.93              |

```
Avg Time per Txn: 1.50 ms
Avg Time per Operation: 0.50 ms
```


| **Threads** | **No. of Txn** | **Timeout** | **Client** | **Avg Time (ms)** |
|:------------|:---------------|:------------|:-----------|:------------------|
| 10          | 100            | 0           | SpyMemcached | 1.57              |
| 10          | 100            | 0           | SpyMemcached | 1.25              |
| 10          | 100            | 0           | SpyMemcached | 1.25              |
| 1           | 100            | 0           | SpyMemcached | 2.97              |
| 1           | 100            | 0           | SpyMemcached | 3.13              |
| 1           | 100            | 0           | SpyMemcached | 2.97              |
| 20          | 100            | 0           | SpyMemcached | 1.25              |
| 20          | 100            | 0           | SpyMemcached | 1.25              |
| 20          | 100            | 0           | SpyMemcached | 1.25              |

```
Avg Time per Txn: 1.87 ms
Avg Time per Operation: 0.62 ms
```

<br />
More Details : Detailed benchmark report may be viewed in
[XLS file](https://spreadsheets.google.com/spreadsheet/ccc?key=0AmvkI5XacgMpdEJQQW9vSUszVEhKeDZPbW45MzF0X1E&hl=en_US) attached here.