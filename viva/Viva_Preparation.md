# ThreadSync — Viva Preparation Notes

**20 Most Likely Questions & Answers**

---

## Q1. What is a thread?

**Answer:** A thread is the smallest unit of CPU scheduling within a process. In Java, a thread is represented by the `java.lang.Thread` class. Multiple threads within the same JVM process share the heap memory (objects, static fields) but each has its own stack, program counter, and register state.

---

## Q2. What is multithreading?

**Answer:** Multithreading is the ability of a CPU or a program to execute multiple threads concurrently. In Java, multithreading allows a program to perform multiple tasks simultaneously — for example, downloading a file in one thread while updating the UI in another. The JVM schedules threads on available CPU cores.

---

## Q3. What is a race condition?

**Answer:** A race condition occurs when two or more threads access shared mutable data concurrently, and the final result depends on the non-deterministic order of their execution. The classic example is the read-modify-write pattern: if Thread A and Thread B both read `count = 5` before either writes back, both write `6`, so one increment is lost. The counter should be 7 but reads 6.

---

## Q4. What is a critical section?

**Answer:** A critical section is a region of code that accesses shared mutable state (shared variables, data structures, files). Only one thread should execute the critical section at a time to prevent race conditions. In Java, critical sections are protected using the `synchronized` keyword.

---

## Q5. What does the `synchronized` keyword do?

**Answer:** `synchronized` acquires the monitor lock of an object before executing the annotated method or block, and releases it when execution ends (even if an exception is thrown). Only one thread can hold a specific object's monitor at a time. Other threads that try to acquire the same lock are put into the BLOCKED state until the lock is released.

---

## Q6. What is a monitor lock?

**Answer:** Every Java object has an associated monitor lock (also called an intrinsic lock or mutex). When a thread calls a `synchronized` method, it acquires that object's monitor. No other thread can acquire the same monitor until the first thread exits the synchronized block. The monitor ensures mutual exclusion on the critical section.

---

## Q7. What is the difference between `start()` and `run()`?

**Answer:** 
- `start()` creates a new OS-level thread and then calls `run()` on it. The new thread executes `run()` concurrently with the calling thread.
- `run()` called directly is just a regular method call — it executes on the **current** thread, no new thread is created.

**Always call `start()` to start a new thread.**

---

## Q8. What is `wait()` and `notifyAll()`?

**Answer:**
- `wait()` — called inside a `synchronized` block, it **releases** the monitor lock and puts the thread into WAITING state until another thread calls `notify()` or `notifyAll()` on the same object.
- `notifyAll()` — wakes **all** threads waiting on the object's monitor. They then compete to re-acquire the lock.
- `notify()` — wakes **one** arbitrarily chosen waiting thread.

**Rule:** Always call `wait()` inside a `while` loop to handle spurious wakeups.

---

## Q9. What are Java thread priority levels?

**Answer:** Java thread priorities range from 1 to 10:
- `Thread.MIN_PRIORITY = 1` — lowest priority (background tasks)
- `Thread.NORM_PRIORITY = 5` — default priority for all new threads
- `Thread.MAX_PRIORITY = 10` — highest priority (time-critical tasks)

Set using `thread.setPriority(n)`. Query using `thread.getPriority()`.

---

## Q10. Does a higher-priority thread always execute first?

**Answer:** **No.** Thread priority is a **scheduling hint** to the JVM and underlying OS. A higher-priority thread is more likely to be scheduled sooner, but this is NOT guaranteed. On Linux, Java thread priorities map to nice values, while on Windows they map to Win32 priority levels. On multi-core systems especially, lower-priority threads may run in parallel with higher-priority ones. This is why this project says "priority is a HINT, not a guarantee."

---

## Q11. What is the difference between synchronization and thread priority?

**Answer:**
- **Synchronization** controls **access** to shared resources — it ensures only one thread runs a critical section at a time. It prevents data corruption.
- **Thread priority** controls **scheduling order** — it influences which thread the CPU picks to run next, but does not protect shared data.

They solve different problems. You need synchronization for correctness; priority is an optional performance tuning hint.

---

## Q12. How does this project demonstrate synchronization?

**Answer:** The project has a `SharedCounter` class with two increment methods:
1. `incrementSynchronized()` — uses `synchronized` to ensure atomic read-modify-write. With 4 threads × 50 tasks, the final count is always exactly **200**.
2. `incrementUnsynchronized()` — no synchronization, intentionally reads the counter, sleeps briefly, then writes tmp+1. With multiple threads, this frequently produces a value **less than 200** due to lost updates.

Both results are displayed live in the UI so the user can see the difference immediately.

---

## Q13. How does the project handle cancellation (Stop button)?

**Answer:** The project uses **cooperative cancellation**:
1. A `volatile boolean simulationRunning` flag is set to `false` when Stop is clicked.
2. Each `WorkerThread` checks `!simulationRunning` on every loop iteration and breaks if false.
3. `thread.interrupt()` is also called to wake threads sleeping in `Thread.sleep()`.
4. `executor.shutdownNow()` tells the ExecutorService to stop accepting new tasks and interrupt running ones.

This avoids the deprecated `Thread.stop()` method, which is unsafe because it can leave shared data in an inconsistent state by releasing all locks held by the thread.

---

## Q14. Why should Swing UI updates use the Event Dispatch Thread (EDT)?

**Answer:** Swing is **single-threaded**. All Swing component reads and writes must occur on the **Event Dispatch Thread (EDT)** to avoid race conditions in the UI toolkit itself. Updating a `JLabel` or `JProgressBar` from a worker thread causes visual glitches, thrown exceptions, or freezing. The correct way is: `SwingUtilities.invokeLater(() -> { label.setText(...); });` — this queues the update to run on the EDT.

---

## Q15. What is a volatile variable?

**Answer:** A `volatile` variable in Java guarantees **visibility** — when one thread writes to a volatile variable, all other threads immediately see the updated value (no CPU register caching). In this project, `simulationRunning` is `volatile` so that when the EDT sets it to `false`, all worker threads immediately observe the change without caching a stale `true` value.

---

## Q16. What is `ExecutorService` and why use it?

**Answer:** `ExecutorService` is a higher-level thread management API in `java.util.concurrent`. It manages a thread pool — threads are reused rather than created and destroyed for each task. In this project, `Executors.newFixedThreadPool(n)` creates a pool of n threads. Submitting a `Runnable` (WorkerThread) to the executor schedules it for execution without managing raw thread creation.

---

## Q17. What is `AtomicInteger` and when would you use it instead of `synchronized`?

**Answer:** `AtomicInteger` is a class in `java.util.concurrent.atomic` that supports lock-free atomic integer operations using CPU-level compare-and-swap (CAS) instructions. `getAndIncrement()` on an `AtomicInteger` is both thread-safe and faster than a `synchronized` method, because it avoids acquiring a monitor lock. Use `AtomicInteger` for simple counters; use `synchronized` for complex multi-statement critical sections.

---

## Q18. What is a deadlock?

**Answer:** A deadlock occurs when two or more threads are each waiting for a resource held by the other, creating a circular dependency from which neither can proceed. Classic example: Thread A holds lock X and waits for lock Y; Thread B holds lock Y and waits for lock X — both are stuck forever. This project does not demonstrate deadlock, but it could be extended to do so.

---

## Q19. Why is `Thread.stop()` deprecated?

**Answer:** `Thread.stop()` forcibly terminates a thread by throwing a `ThreadDeath` error, which causes the thread to immediately release **all monitor locks** it holds. This can leave shared data in an inconsistent, partially updated state — other threads see the corrupted data with no warning. Java deprecated it as unsafe. The correct approach is cooperative cancellation (volatile flag + interrupt), which is what this project uses.

---

## Q20. What are the limitations of your project?

**Answer:**
1. Thread priority scheduling is JVM/OS-dependent — cannot guarantee observed order across platforms.
2. Race conditions are non-deterministic — may not appear every run at very low task counts.
3. The work simulation uses `Thread.sleep()`, which is not representative of actual CPU-bound work.
4. Swing UI may show slight delay at very high concurrency (>50 threads) due to EDT queue.
5. No persistent storage — results are lost when the app closes.

---

## 📌 Quick Reference Cheat Sheet

| Concept | Key Class/Method | Purpose |
|---|---|---|
| Create thread | `extends Thread`, `implements Runnable` | Define concurrent task |
| Start thread | `thread.start()` | Launch on new OS thread |
| Synchronize | `synchronized void method()` | Mutual exclusion |
| Wait for notification | `wait()` | Release lock, pause thread |
| Wake up waiters | `notifyAll()` | Resume waiting threads |
| Set priority | `thread.setPriority(1-10)` | Scheduling hint |
| Safe stop | `volatile boolean flag` + `interrupt()` | Cooperative cancel |
| Update Swing safely | `SwingUtilities.invokeLater()` | Run on EDT |
| Thread-safe counter | `AtomicInteger.getAndIncrement()` | Lock-free counting |
| Thread pool | `Executors.newFixedThreadPool(n)` | Manage worker threads |

---

*Good luck with your viva! Remember: synchronized = correctness, priority = hint only.*
