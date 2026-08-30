# Architectural Decisions

This document records the design choices behind the distributed in-memory data store. Each numbered section is a standing question. Phase 1 answered the networking model. Phase 2 answered the wire protocol and command dispatch. Later phases fill in the rest without rewriting the outline.

---

## 1. Why thread-per-client or thread pool?

We are using a thread pool (`ExecutorService`) to prevent OS thread exhaustion and scale to thousands of connections.

A naive thread-per-client model (`new Thread(handler).start()` on every `accept()`) maps each TCP connection to a kernel thread. Under load that exhausts the OS thread table, increases context-switch cost, and makes denial-of-service trivial (open many idle sockets). A fixed pool (`Executors.newFixedThreadPool(10)` in Phase 1) bounds concurrency: the accept loop stays responsive, excess connections wait to be scheduled, and the process cannot spawn an unbounded number of native threads.

Phase 1 used a conservative pool size of 10 so behavior is predictable while the protocol parser and shared store were still absent. Phase 2 did not change that bound: command work for `PING`/`ECHO` is cheap and still blocking on socket I/O. The pool size (or a move to virtual threads behind the same `ExecutorService` interface) can be revisited once a shared store and blocking command processing are measured; the public contract remains “submit a `ClientHandler` to an `ExecutorService`.”

---

## Phase 2: RESP protocol and command dispatch

The socket layer must not grow a `switch` on command names. Framing, routing, and semantics are three types:

| Layer | Type | Responsibility |
| --- | --- | --- |
| Framing | `RespParser` / `RespSerializer` | Bytes ↔ Java strings |
| Routing | `CommandRouter` | Name → `Command` |
| Semantics | `Command` implementations | Args → RESP reply |

### Why a line-oriented RESP subset

Redis clients (including `redis-cli`) send commands as **RESP arrays of bulk strings**:

```text
*2\r\n$4\r\nECHO\r\n$5\r\nhello\r\n
```

`RespParser` reads one frame from a `BufferedReader`: the `*` count, then for each element a `$` length line and the following data line. Stream close, a missing `*`/`$` prefix, or a truncated frame returns an empty list. `ClientHandler` treats that as disconnect rather than spinning on a half-read request.

This is enough for interoperable `PING`/`ECHO`. Inline protocol (`PING\r\n` with no array) and nested RESP types are deferred; adding them later should not change `Command` or `TcpServer`.

`RespSerializer` is the inverse: simple strings (`+`), bulk strings (`$…\r\n…\r\n`), null bulk (`$-1\r\n`), and errors (`-`). Commands return already-serialized strings so the handler only writes and flushes.

### Why the Command pattern

Each verb is a `Command` with `String execute(List<String> args)`. `CommandRouter` holds a `Map<String, Command>` filled in its constructor (`PING`, `ECHO`). Lookup uppercases the first token (`Locale.ROOT`) so `ping` and `PING` are the same command. Arguments are `subList(1, size)` — the command name is not passed into `execute`.

Unknown names become `ERR unknown command '…'` instead of a thrown exception, matching Redis’s “the server stays up; the client gets an error” contract.

`PingCommand` and `EchoCommand` are stateless. `TcpServer` therefore constructs **one** `CommandRouter` and passes it into every `ClientHandler`. When Phase 3 introduces a shared store, that store will be injected the same way; commands that mutate memory will take the store in their constructor rather than putting maps on the handler.

### Request path

```text
accept() → ClientHandler.run()
        → RespParser.parse(reader)
        → CommandRouter.route(tokens)
        → Command.execute(args)
        → BufferedWriter.write(resp) + flush()
```

The handler never inspects token[0]. Adding `GET`/`SET` is a new `Command` class plus a `put` in the router.

---

## 2. How is shared state protected?

(To be populated in later phases)

---

## 3. Internal data structures used

(To be populated in later phases)

---

## 4. Persistence mechanisms

(To be populated in later phases)

---

## 5. Handling primary failures

(To be populated in later phases)

---

## 6. Replication synchronization

(To be populated in later phases)

---

## 7. Bottlenecks

(To be populated in later phases)

---

## 8. Benchmarking

(To be populated in later phases)
