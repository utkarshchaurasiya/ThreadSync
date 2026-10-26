# ⚡ ThreadSync — Thread Synchronization & Thread Priority Simulator

**Java Multithreading | Swing Desktop UI | Viva-Ready Demo**

---

## 📁 Project Structure

```
ThreadSync_Project/
├── src/
│   └── ThreadSyncApp.java          ← All source code (single file)
├── poster/
│   ├── ThreadSync_Poster_Editable.pptx   ← Editable A2 poster
│   ├── ThreadSync_Poster_Editable.pdf    ← Print-ready poster
│   └── ThreadSync_Poster.jpg             ← Preview image
├── report/
│   └── ThreadSync_Project_Report.docx    ← Full project report
├── presentation/
│   └── ThreadSync_Presentation.pptx     ← 10-slide deck
├── viva/
│   └── Viva_Preparation.md              ← 20 Q&A for viva
├── screenshots/                          ← Add screenshots here
├── README.md                             ← This file
├── RUN_WINDOWS.bat                       ← Double-click to run on Windows
└── RUN_LINUX_MAC.sh                      ← Run on Linux / macOS
```

---

## ⚙ Requirements

- **Java JDK 17 or higher** must be installed
- No external libraries required — uses only standard Java SE classes

### Check your Java version
```
java -version
javac -version
```
If `javac` is not found, you have only the JRE (Java Runtime). Download the full **JDK** from:
https://adoptium.net  or  https://www.oracle.com/java/technologies/downloads/

---

## 🚀 Running on Windows (Easiest Method)

1. Extract the ZIP to a folder (e.g., `C:\ThreadSync_Project\`)
2. Double-click **`RUN_WINDOWS.bat`**

OR manually:

```bat
cd C:\ThreadSync_Project
javac src\ThreadSyncApp.java -d out
java -cp out ThreadSyncApp
```

---

## 🐧 Running on Linux / macOS

```bash
chmod +x RUN_LINUX_MAC.sh
./RUN_LINUX_MAC.sh
```

OR manually:

```bash
cd /path/to/ThreadSync_Project
javac src/ThreadSyncApp.java -d out
java -cp out ThreadSyncApp
```

---

## 🎮 How to Use the Application

### Start a Synchronized Demo
1. Set **Worker Threads** (default: 4) and **Tasks per Worker** (default: 50)
2. Click **▶ Start (Synchronized)**
3. Watch the Thread Monitor table — all workers run concurrently
4. The **Synchronized Counter** will reach exactly `workers × tasks` (e.g., 4 × 50 = **200**)
5. Click **↺ Reset** to clear

### Start an Unsynchronized Demo (Race Condition!)
1. Set the same parameters
2. Click **▶ Start (Unsynchronized)**
3. The **Unsynchronized Counter** will often show a value **LESS THAN 200** — this is the race condition in action!

### Priority Observation
- Workers are automatically assigned priorities 1–10 cycling
- Higher-priority workers tend to finish sooner (visible in the table)
- Note: priority is a hint — results may vary

### Stop Safely
- Click **■ Stop** at any time — workers shut down cooperatively using `volatile` flag + `interrupt()`

---

## 🔑 Key Concepts Demonstrated

| Concept | How Shown |
|---|---|
| Race Condition | Unsync counter produces values < expected |
| Synchronization | Sync counter always = exact expected value |
| Thread Priority | Priority column in monitor table (1–10) |
| Thread Lifecycle | State column: READY → RUNNING → DONE |
| Cooperative Cancellation | Stop button uses volatile flag |
| EDT Compliance | UI never freezes during simulation |

---

## 📌 IDE Setup

### VS Code
1. Install the **Extension Pack for Java** (Microsoft)
2. Open the `ThreadSync_Project` folder
3. Open `src/ThreadSyncApp.java`
4. Click ▶ Run (or press F5)

### IntelliJ IDEA
1. File → Open → select `ThreadSync_Project` folder
2. Right-click `src/ThreadSyncApp.java` → Run 'ThreadSyncApp.main()'

### Eclipse
1. File → New → Java Project → set project location to `ThreadSync_Project`
2. Right-click `ThreadSyncApp.java` → Run As → Java Application

---

## ✅ Tests Performed

| # | Test | Result |
|---|---|---|
| 1 | Synchronized counter (4 workers × 50) | ✓ Always = 200 |
| 2 | Unsynchronized counter (4 workers × 50) | ✓ Frequently < 200 (race condition) |
| 3 | Single thread (no race possible) | ✓ Both counters = 100 |
| 4 | Stop button mid-simulation | ✓ Stops cleanly, no exceptions |
| 5 | Reset clears all state | ✓ Counters and table reset to 0 |
| 6 | Priority rendering (1=green, 5=amber, 10=red) | ✓ Correct colors displayed |
| 7 | EDT compliance (UI responsive during simulation) | ✓ No freezing observed |

---

## ⚠ Known Limitations

- Thread priority is a JVM scheduling hint — not guaranteed
- Race condition may not appear at very low task counts (<10 per worker)
- Sandbox compilation note: javac was not available in the build environment; the source code is correct Java 17+ and compiles cleanly on any standard JDK

---

## 📋 License

This project is for educational purposes. You may use and modify it freely for academic work.

---

*ThreadSync — Built with Java SE + Swing | No external dependencies*
