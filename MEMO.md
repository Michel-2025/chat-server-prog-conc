\# Architecture Decision Memo

\*\*Project:\*\* Live Chat Server — Topic A  

\*\*Group Members:\*\* Michel NJEIM, Lynn HAJJAR, Andrew Hajj  

\*\*Course:\*\* Concurrency, Parallelism \& Distributed Systems — Semester VIII  

\*\*Instructor:\*\* Dr. Mohamad Aoude  



\---



\## Decision: Bounded Per-Session Queue with Fixed Thread Pool



\### What We Decided

Each connected WebSocket client gets its own bounded queue of 100 messages.

A fixed thread pool of 4 threads handles all outgoing sends across all sessions.

The chat server calls the worker service asynchronously using CompletableFuture

with a 3-second timeout and an exceptionally() fallback.



\---



\### Q1. What guarantee does your chosen design provide?

\- No single slow client can block other clients from receiving messages.

\- The thread pool size is fixed at 4, so the JVM never creates unbounded threads.

\- Each session queue is bounded at 100 messages, so memory usage is predictable.

\- The worker HTTP call never blocks the chat thread — it runs on a separate

&#x20; async pool and fails silently if the worker is down.



\---



\### Q2. What failure modes does it prevent?

\- \*\*Thread explosion:\*\* FixedThreadPool(4) caps concurrent threads regardless

&#x20; of how many clients connect.

\- \*\*Memory exhaustion:\*\* LinkedBlockingQueue(100) per session means a slow

&#x20; client can accumulate at most 100 messages before drops begin.

\- \*\*Chat blocking on worker failure:\*\* CompletableFuture + exceptionally()

&#x20; ensures the chat server continues delivering messages even if the worker

&#x20; service is completely unreachable.

\- \*\*Race conditions on broadcast:\*\* CopyOnWriteArrayList allows safe iteration

&#x20; over room members while new clients connect or disconnect concurrently.



\---



\### Q3. What failure modes does it introduce?

\- \*\*Message loss for slow clients:\*\* When a session queue reaches 100 messages,

&#x20; new messages are dropped and logged as warnings. The client receives no

&#x20; notification of the drop.

\- \*\*Head-of-line blocking within a session:\*\* Messages in a session queue are

&#x20; processed in FIFO order. A large burst fills the queue and later messages

&#x20; are dropped even if the client recovers quickly.

\- \*\*Worker data loss:\*\* If the worker service is down, message logs are lost

&#x20; silently. There is no retry queue or persistent event log for worker calls.

\- \*\*4-thread bottleneck under extreme load:\*\* With thousands of clients,

&#x20; 4 sender threads may become a bottleneck. Increasing the pool size trades

&#x20; resource usage for throughput.



\---



\### Q4. How does it behave under overload? Measured numbers.



Load test with 50 concurrent clients, 20 messages each:



| Metric | Result |

|---|---|

| Clients | 50 |

| Messages sent | 1,000 |

| Messages received | 10,689 |

| Errors | 0 |

| Duration | 6,009 ms |

| Throughput | 166 msg/sec |



No messages were dropped during this test because 50 clients at 50ms intervals

did not saturate the queue. To trigger drops, a client would need to send

faster than the 4 sender threads can drain its queue of 100 messages.



\---



\### Q5. How would a new engineer debug it?



1\. \*\*Check metrics endpoint\*\* at `http://localhost:8081/metrics` — if

&#x20;  `messages\_dropped` is rising, a slow client is saturating its queue.



2\. \*\*Check logs for WARN lines\*\* — every dropped message logs:

&#x20;  `DROPPED message for slow client session=...`



3\. \*\*Check logs for WORKER unreachable\*\* — if the worker service is down,

&#x20;  every message logs: `WORKER unreachable, continuing without it`



4\. \*\*Check logs for SEND FAILED\*\* — indicates a WebSocket session closed

&#x20;  mid-send, usually from a client disconnect during load testing.



5\. \*\*Reproduce with LoadTestClient\*\* — run `LoadTestClient.java` to simulate

&#x20;  50 concurrent clients and observe throughput and drop behavior live.

