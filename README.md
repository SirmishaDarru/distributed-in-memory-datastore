# Distributed In-Memory Data Store

![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Maven](https://img.shields.io/badge/Build-Maven-C71A32.svg)
![Protocol](https://img.shields.io/badge/Protocol-RESP-blue.svg)
![TCP](https://img.shields.io/badge/Transport-TCP%2FIP-informational.svg)
![Concurrency](https://img.shields.io/badge/Concurrency-Thread%20Pool-success.svg)
![Status](https://img.shields.io/badge/Phase-1%20Core%20Server-yellow.svg)

A high-performance, multithreaded, Redis-compatible in-memory data store built from scratch in Java.

This project is a systems-engineering deep dive: a custom TCP server, a from-scratch REdis Serialization Protocol (RESP) parser, concurrent data structures, disk persistence (RDB/AOF), and multi-node replication. It is not a wrapper around Redis — it is an independent store that speaks the same wire protocol.

Because it implements RESP, **this server can be queried with the standard `redis-cli`.**

---

## Architecture

Requests follow a single pipeline. Each stage is isolated so later phases can replace internals without changing the wire contract.

```text
  Client (redis-cli / netcat)
              |
              v
         TCP Socket
              |
              v
         RESP Parser
              |
              v
        Command Router
              |
              v
       Data Structures
              |
              v
       Concurrency Layer
              |
              v
   Persistence / Replication
```

Expanded view of the same pipeline, including the stores and durability paths planned for later phases:

```text
                  Client (redis-cli / netcat)
                              |
                        TCP Socket
                              |
                       RESP Parser
                              |
                      Command Router
                              |
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
     KV Store               Lists                Streams
 (ConcurrentHashMap)  (Concurrent Linked)     (Radix Trees)
        │
  Concurrency & Transaction Layer (MULTI/EXEC, WATCH)
        │
  ┌─────┴─────────────────────┐
  │                           │
Persistence              Replication
(RDB / AOF)            (Primary → Replica)
```

Phase 1 implements the TCP socket layer: a `ServerSocket` accept loop, a bounded `ExecutorService` thread pool, and a per-connection `ClientHandler` that echoes a dummy RESP simple-string reply (`+OK\r\n`) until the parser and router land in later phases.

Architectural rationale lives in [`docs/architecture.md`](docs/architecture.md).

---

## Phases of Development

| Phase | Focus | Status |
| :---: | --- | --- |
| **1** | **Core Server** — multithreaded TCP listener on port `6379`, connection lifecycle, dummy RESP replies | In progress |
| **2** | **Advanced Data Structures** — strings, lists, hashes, sets, sorted sets, and (later) streams behind a command router | Planned |
| **3** | **Concurrency / Transactions** — shared-state protection, `MULTI`/`EXEC`, `WATCH`, isolation under concurrent clients | Planned |
| **4** | **Persistence** — RDB snapshots and AOF logging for crash recovery | Planned |
| **5** | **Replication** — primary → replica sync, replica catch-up, and failover behavior | Planned |

---

## Why RESP (and `redis-cli`)

RESP is a small, binary-safe, request/response protocol. Implementing it means:

- **Interoperability** — `redis-cli`, `redis-benchmark`, and most Redis client libraries can talk to this process without a custom SDK.
- **Testability** — protocol correctness can be verified with tools the ecosystem already trusts.
- **Discipline** — the parser is a first-class component, not an afterthought bolted onto a text line reader.

Until the parser ships, Phase 1 still writes `+OK\r\n` so a TCP client (including `redis-cli`) receives a well-formed simple string.

---

## Prerequisites

- **JDK 21** or newer
- **Apache Maven 3.9+**
- Optional: [Redis CLI](https://redis.io/docs/latest/develop/tools/cli/) for protocol-level testing

---

## Build and run

```bash
mvn -q compile
mvn -q exec:java -Dexec.mainClass=com.datastore.Main
```

Or package and run the JAR (the manifest `Main-Class` is `com.datastore.Main`):

```bash
mvn -q package
java -jar target/distributed-in-memory-datastore-0.1.0-SNAPSHOT.jar
```

The server binds **port 6379** (the Redis default) and logs accepted connections. Stop with `Ctrl+C`; a JVM shutdown hook calls `TcpServer.stop()` so the accept loop, thread pool, and server socket shut down cleanly.

### Talk to the server

```bash
# Any TCP client
printf 'PING\r\n' | nc 127.0.0.1 6379

# redis-cli (works today for a dummy +OK; full command semantics arrive with the RESP parser)
redis-cli -p 6379 PING
```

---

## Project layout

```text
distributed-in-memory-datastore/
├── pom.xml
├── README.md
├── docs/
│   └── architecture.md
└── src/main/java/com/datastore/
    ├── Main.java
    └── server/
        ├── TcpServer.java
        └── ClientHandler.java
```

| Type | Path | Role |
| --- | --- | --- |
| Entry point | `com.datastore.Main` | Binds port 6379, registers a shutdown hook, starts the server |
| Accept loop | `com.datastore.server.TcpServer` | `ServerSocket` + `ExecutorService` (fixed pool of 10) |
| Connection | `com.datastore.server.ClientHandler` | Reads bytes, logs payload, writes `+OK\r\n` |

---

## Design constraints (Phase 1)

- **Thread pool, not unbounded thread-per-connection.** `Executors.newFixedThreadPool(10)` caps OS threads and keeps the accept path from collapsing under a connection flood. See architecture Q1.
- **RESP-shaped I/O from day one.** Dummy replies use CRLF-terminated simple strings so later parser work does not change the socket layer.
- **Graceful shutdown.** Closing the `ServerSocket` unblocks `accept()`; the pool is shut down afterward.

---

## License

This repository is a learning / portfolio systems project. Licensing will be declared explicitly if the project is published.
