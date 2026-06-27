# Chat Server — Concurrent & Distributed Systems Project
Lebanese University — Faculty of Engineering — Semester VIII — 2025–2026
Instructor: Dr. Mohamad Aoude

## Group Members
- Michel NJEIM
- Lynne HAJJAR
- Andrew HAJJ

## Topic
Topic A — Real-Time Web Backend
Application: Live Chat Server with distributed worker logging service.

## Architecture Overview
[Browser Clients] ←→ WebSocket ←→ [Chat Server :8081] ←→ HTTP ←→ [Worker Service :8082]

## How to Run

### Requirements
- Java 23
- Gradle (wrapper included)

### Step 1 — Start the Worker Service
```bash
cd worker-service/app
../gradlew run
```
Wait for: `Tomcat started on port 8082`

### Step 2 — Start the Chat Server
```bash
cd chat-server-prog-conc/app
../gradlew run
```
Wait for: `Tomcat started on port 8081`

### Step 3 — Test via WebSocket
1. Go to https://www.piesocket.com/websocket-tester
2. Connect to: `ws://localhost:8081/chat/room1`
3. Send messages — they broadcast to all clients in the room

### Step 4 — Check Metrics
http://localhost:8081/metrics

## Running the Load Test
```bash
cd chat-server-prog-conc/app
../gradlew run -PrunClassName=com.chatserver.test.LoadTestClient
```

### Load Test Results
| Metric | Result |
|---|---|
| Concurrent clients | 50 |
| Messages sent | 1,000 |
| Messages received | 10,689 |
| Errors | 0 |
| Duration | 6,009 ms |
| Throughput | 166 msg/sec |

## Failure Injection

### Scenario 1 — Worker Service Down
1. Start both services
2. Send a message — confirm `WORKER response status=200`
3. Stop the worker service
4. Send another message
5. Chat server logs: `WORKER unreachable, continuing without it`
6. Chat continues working normally (graceful degradation)

### Scenario 2 — Graceful Shutdown
1. Stop the chat server
2. Logs show:
SHUTDOWN initiated - stopping sender thread pool
SHUTDOWN completed cleanly
## Concurrency Scorecard
| Question | Mechanism | Evidence |
|---|---|---|
| Thread-safe? | ConcurrentHashMap, CopyOnWriteArrayList, synchronized sends | No race conditions in load test |
| Visibility guaranteed? | volatile via AtomicLong, ConcurrentHashMap | Metrics always consistent |
| Deadlock-free? | No nested locks, one lock per session | 0 errors in load test |
| Liveness guaranteed? | Bounded queue with offer(), slow clients dropped | No blocking under load |
| Bounded resources? | MAX_QUEUE_SIZE=100, FixedThreadPool(4) | Queue depth never unbounded |
| Failure recovery path? | Worker timeout + exceptionally() fallback | WORKER unreachable log |

## Key Design Decisions
- **FixedThreadPool(4)** for sending — prevents unbounded thread creation
- **LinkedBlockingQueue(100)** per session — backpressure for slow clients
- **CopyOnWriteArrayList** for room members — safe iteration during broadcasts
- **CompletableFuture** for async worker calls — chat never blocks on HTTP
- **3-second timeout** on worker HTTP calls — fast failure detection
