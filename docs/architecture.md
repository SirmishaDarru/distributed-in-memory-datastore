# Architectural Decisions

This document records the design choices behind the distributed in-memory data store. Each section is a standing question: Phase 1 answers the networking model; later phases fill in the rest without rewriting the outline.

---

## 1. Why thread-per-client or thread pool?

We are using a thread pool (`ExecutorService`) to prevent OS thread exhaustion and scale to thousands of connections.

A naive thread-per-client model (`new Thread(handler).start()` on every `accept()`) maps each TCP connection to a kernel thread. Under load that exhausts the OS thread table, increases context-switch cost, and makes denial-of-service trivial (open many idle sockets). A fixed pool (`Executors.newFixedThreadPool(10)` in Phase 1) bounds concurrency: the accept loop stays responsive, excess connections wait to be scheduled, and the process cannot spawn an unbounded number of native threads.

Phase 1 uses a conservative pool size of 10 so behavior is predictable while the protocol parser and shared store are still absent. The pool size (or a move to virtual threads behind the same `ExecutorService` interface) can be revisited once command processing and blocking I/O characteristics are measured; the public contract remains “submit a `ClientHandler` to an `ExecutorService`.”

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
