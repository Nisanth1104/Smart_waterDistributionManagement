# Smart Water Distribution Management System
**Java and Data Structures Academic Mini Project (Phases 1 & 2)**

---

##  1. Project Overview & Architecture

The **Smart Water Distribution Management System** models municipal water flow, demand aggregation, reservoir level monitoring, pipeline connectivity, citizen complaint handling, and automated priority-based water allocation.

* **Phase 1 (Frontend):** Pure HTML5, basic CSS, and vanilla JavaScript (no React, Angular, Bootstrap, or Tailwind CSS). Responsive, institutional light blue/dark blue/white theme with full `localStorage` persistence.
* **Phase 2 (Java Backend):** Pure Java (JDK 21) backend using standard Java data structures (**Adjacency List Graph, BFS Graph Traversal, PriorityQueue, ArrayDeque, HashMap, and ArrayList**) with a zero-dependency HTTP server that serves both REST APIs and the static frontend UI.

```
                    ┌────────────────────────────────────────────────────────┐
                    │               Web Browser (Frontend UI)                │
                    │  HTML5 + Basic CSS + Vanilla JS (fetch API Client)     │
                    └───────────────────────────┬────────────────────────────┘
                                                │ HTTP Requests (Port 8080)
                                                ▼
┌────────────────────────────────────────────────────────────────────────────────────────────┐
│                                Java Backend (JDK 21)                                       │
│                                                                                            │
│   ┌────────────────────────────────────────────────────────────────────────────────────┐   │
│   │ SimpleHttpServer (java.net.ServerSocket)                                           │   │
│   │   ├── REST API Endpoints (/api/...) ──> Built-in lightweight JSON API handlers     │   │
│   │   └── Static UI Assets (/*)         ──> Built-in static file streamer (frontend)   │   │
│   └──────────────────────────────────────────┬─────────────────────────────────────────┘   │
│                                              │                                             │
│   ┌──────────────────────────────────────────▼─────────────────────────────────────────┐   │
│   │ WaterNetwork (Core Distribution Coordinator)                                       │   │
│   │                                                                                    │   │
│   │   ├── Adjacency List Graph (Map<String, List<Pipeline>>)                           │   │
│   │   │   └── BFS Traversal: findReachableAreasBFS()                                   │   │
│   │   │                                                                                │   │
│   │   ├── PriorityQueue<WaterRequest> (PriorityAllocationPolicy)                       │   │
│   │   │   └── 5-Tier Sorting: Critical > High > Medium > Low > Very Low                │   │
│   │   │                                                                                │   │
│   │   ├── ArrayDeque<MaintenanceRequest> (FIFO Repair Queue)                           │   │
│   │   │   └── FIFO Dispatch: processNextMaintenanceFIFO()                              │   │
│   │   │                                                                                │   │
│   │   ├── HashMap<String, Reservoir> & HashMap<String, DemandArea>                     │   │
│   │   │   └── O(1) indexed lookups and updates                                         │   │
│   │   │                                                                                │   │
│   │   └── ArrayList<WaterRequest> & ArrayList<Complaint> (History logs)                │   │
│   └────────────────────────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Project Directory Structure

```text
Smart_Watermanage/
├── index.html                       # Root launcher redirecting to the portal
├── README.md                        # Project report, DSA explanation & evaluation guide
│
├── backend/
│   ├── bin/                         # Compiled Java .class bytecodes
│   └── src/
│       └── com/
│           └── smartwater/
│               ├── Main.java        # Main launcher class & Server entry point
│               ├── model/
│               │   ├── PriorityLevel.java       # Enum: CRITICAL(5), HIGH(4), MEDIUM(3), LOW(2), VERY_LOW(1)
│               │   ├── PipelineStatus.java      # Enum: WORKING, FAILED, UNDER_MAINTENANCE
│               │   ├── Reservoir.java           # Encapsulates storage, bounds (0 <= avail <= cap)
│               │   ├── Pipeline.java            # Models pipeline conduits and health status
│               │   ├── DemandArea.java          # Abstract base class with validation (min <= max)
│               │   ├── CriticalInstitution.java # Subclass: Hospital, ICU, enforces CRITICAL priority
│               │   ├── ResidentialArea.java     # Subclass: Residential societies, household counts
│               │   ├── CommercialArea.java      # Subclass: Commercial hubs, IT parks
│               │   ├── WaterRequest.java        # Implements Comparable<WaterRequest> for PriorityQueue
│               │   ├── Complaint.java           # Grievance reporting model
│               │   ├── MaintenanceRequest.java  # FIFO task model for ArrayDeque queue
│               │   ├── AllocationRecord.java    # Per-area allocation result
│               │   └── AllocationSummary.java   # Consolidated grid metrics and alerts
│               ├── policy/
│               │   ├── WaterAllocationPolicy.java    # Interface: strategy pattern abstraction
│               │   └── PriorityAllocationPolicy.java # PriorityQueue-based allocation engine
│               ├── network/
│               │   └── WaterNetwork.java        # Adjacency list, BFS reachability, collections
│               ├── server/
│               │   ├── SimpleJson.java          # Zero-dependency JSON serializer and parser
│               │   └── SimpleHttpServer.java    # Pure Java ServerSocket HTTP server & static streamer
│               └── test/
│                   └── BackendTestSuite.java    # Automated test runner for 10 core scenarios
│
└── SmartWaterDistribution/          # Complete Frontend Website
    ├── index.html                   # Login Selection Portal
    ├── user-login.html              # User login form (calls POST /api/login)
    ├── admin-login.html             # Admin login form (calls POST /api/login)
    ├── user-dashboard.html          # Resident dashboard (demands, complaints, status)
    ├── admin-dashboard.html         # Admin dashboard (7 modules, network graph, allocation)
    ├── css/
    │   └── styles.css               # Clean styling (light blue, white, dark blue)
    └── js/
        ├── data.js                  # BackendApi client (fetch) & LocalStorage fallback
        ├── script.js                # Shared helpers, formatting, badge utilities
        ├── user-dashboard.js        # User forms and status refresh
        └── admin-dashboard.js       # Admin modules, graph visualizer, allocation caller
```

---

## 3. How to Compile and Run

### Option A: Running in an IDE (VS Code, IntelliJ IDEA, Eclipse, NetBeans)
1. Open the project folder `Smart_Watermanage` in your IDE.
2. Ensure Java 17+ or JDK 21 is configured as the project SDK.
3. Open [`Main.java`](file:///c:/Users/Welcome/Desktop/Smart_Watermanage/backend/src/com/smartwater/Main.java) and click **Run** (or press the green Run button).
4. Open your browser and navigate to: **`http://localhost:8080/`**.
5. To run the automated test suite, open [`BackendTestSuite.java`](file:///c:/Users/Welcome/Desktop/Smart_Watermanage/backend/src/com/smartwater/test/BackendTestSuite.java) and click **Run**.

### Option B: Using Standard Command Line (Terminal / PowerShell / CMD)
```powershell
# 1. Compile Java sources into backend/bin
javac -d backend/bin (Get-ChildItem -Recurse backend/src/*.java)

# 2. Run Automated Test Suite (10 Scenarios)
java -cp backend/bin -ea com.smartwater.test.BackendTestSuite

# 3. Start the Web Server
java -cp backend/bin com.smartwater.Main
```
*After starting the server, open **`http://localhost:8080/`** in your browser.*

### Option C: Offline / Direct Browser Mode
If running without the Java backend, simply double-click [`SmartWaterDistribution/index.html`](file:///c:/Users/Welcome/Desktop/Smart_Watermanage/SmartWaterDistribution/index.html). The frontend will detect that the backend is offline and seamlessly run using `localStorage`, displaying a `🔵 LocalStorage Cache` badge.

---

##  4. Demonstration Credentials

| Role | Username | Password | Target Portal |
| :--- | :--- | :--- | :--- |
| **User Login** | `user` | `user123` | Resident demand submission & complaint tracking |
| **Admin Login** | `admin` | `admin123` | Municipal grid operations, BFS graph, FIFO queue, allocation |

*(Both login forms include an **"⚡ Auto-fill Demo Credentials"** button for quick faculty evaluation.)*

---

##  5. Core Java Data Structures & Why They Are Used

### 1. Adjacency List Graph (`Map<String, List<Pipeline>>`)
* **Why it's used:** Real-world distribution networks consist of nodes (reservoirs, junctions, demand areas) and directed/undirected edges (pipelines). An adjacency list provides optimal $O(V + E)$ space complexity and enables rapid traversal of outgoing pipes from any reservoir node.
* **Location in code:** [`WaterNetwork.java`](file:///c:/Users/Welcome/Desktop/Smart_Watermanage/backend/src/com/smartwater/network/WaterNetwork.java) (`rebuildAdjacencyList()`).

### 2. Breadth-First Search (BFS) Traversal
* **Why it's used:** To determine physical network reachability. Starting from all active reservoirs (`status == Active`), BFS explores neighbor nodes only across operational pipelines (`status == Working`). If a pipeline is `Failed`, BFS cannot cross the edge, correctly classifying downstream demand areas as **Disconnected**.
* **Location in code:** [`WaterNetwork.java`](file:///c:/Users/Welcome/Desktop/Smart_Watermanage/backend/src/com/smartwater/network/WaterNetwork.java) (`findReachableAreasBFS()`).

### 3. PriorityQueue (`java.util.PriorityQueue<WaterRequest>`)
* **Why it's used:** Water is an essential, life-critical utility. Healthcare institutions (Hospitals) and schools cannot be starved in favor of standard domestic or recreational uses.
* **Ordering rule:**
  1. Primary: Priority weight descending: `Critical (5)` > `High (4)` > `Medium (3)` > `Low (2)` > `Very Low (1)`.
  2. Secondary: Deterministic FIFO request arrival ordering (`arrivalOrder ASC`) for requests with equal priority.
* **Location in code:** [`WaterRequest.java`](file:///c:/Users/Welcome/Desktop/Smart_Watermanage/backend/src/com/smartwater/model/WaterRequest.java) (`compareTo`) and [`PriorityAllocationPolicy.java`](file:///c:/Users/Welcome/Desktop/Smart_Watermanage/backend/src/com/smartwater/policy/PriorityAllocationPolicy.java).

### 4. ArrayDeque (`java.util.ArrayDeque<MaintenanceRequest>`)
* **Why it's used:** Servicing municipal infrastructure requires a fair, starvation-free **FIFO (First-In, First-Out)** dispatch model. Java's `ArrayDeque` is significantly faster and more memory-efficient than `LinkedList` or legacy `Vector`-based `Stack` for queue operations (`offerLast` / `pollFirst`).
* **Location in code:** [`WaterNetwork.java`](file:///c:/Users/Welcome/Desktop/Smart_Watermanage/backend/src/com/smartwater/network/WaterNetwork.java) (`processNextMaintenanceFIFO()`).

### 5. HashMap (`java.util.HashMap<String, T>`)
* **Why it's used:** Provides $O(1)$ constant-time lookup, retrieval, and updates for reservoirs, demand areas, pipelines, and allocation status by unique alphanumeric identifiers (`AREA-01`, `RES-A`, `PL-04`).
* **Location in code:** [`WaterNetwork.java`](file:///c:/Users/Welcome/Desktop/Smart_Watermanage/backend/src/com/smartwater/network/WaterNetwork.java) (`reservoirs`, `demandAreas`, `pipelines`).

### 6. ArrayList (`java.util.ArrayList<T>`)
* **Why it's used:** Dynamic contiguous-memory list that preserves historical chronological sequences for user demand requests, submitted complaints, and allocation audits.
* **Location in code:** [`WaterNetwork.java`](file:///c:/Users/Welcome/Desktop/Smart_Watermanage/backend/src/com/smartwater/network/WaterNetwork.java) (`requestHistory`, `complaints`).

---

## 📡 6. REST API Endpoint Documentation

| Method | Endpoint | Request Body (JSON) | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/login` | `{"username":"admin","password":"...","role":"admin"}` | Validates user or admin credentials. |
| `GET` | `/api/overview` | — | Returns high-level grid metrics for dashboard cards. |
| `GET` | `/api/reservoirs` | — | Retrieves all reservoirs, capacities, and active levels. |
| `PUT` | `/api/reservoirs` | `{"id":"RES-A","available":35000,"dailyLimit":50000,"status":"Active"}` | Updates reservoir available volume and status with bounds checks. |
| `GET` | `/api/areas` | — | Retrieves all configured demand areas and priorities. |
| `POST` | `/api/areas` | `{"id":"AREA-06","name":"Tech Park","type":"Commercial",...}` | Creates a new demand area and auto-generates feeder pipeline. |
| `PUT` | `/api/areas` | `{"id":"AREA-01","name":"...","minRequirement":15000,...}` | Updates area parameters with $\text{min} \le \text{max}$ validation. |
| `DELETE`| `/api/areas?id=AREA-06` | — | Deletes demand area and cleans up connected pipelines. |
| `GET` | `/api/pipelines` | — | Retrieves all pipelines and health states. |
| `PUT` | `/api/pipelines/status` | `{"id":"PL-04","status":"Working"}` | Toggles pipeline status (`Working`, `Failed`, `Under Maintenance`). |
| `GET` | `/api/requests` | — | Retrieves all water demand requests. |
| `POST` | `/api/requests` | `{"areaId":"AREA-01","quantity":15000,"priority":"High",...}` | Registers a new water request. |
| `GET` | `/api/complaints`| — | Retrieves all citizen and institutional complaints. |
| `POST` | `/api/complaints`| `{"areaId":"AREA-04","type":"Pipeline Leakage",...}` | Submits a new grievance ticket. |
| `PUT` | `/api/complaints/status` | `{"id":"CMP-201","status":"Resolved"}` | Updates complaint resolution status. |
| `GET` | `/api/maintenance`| — | Retrieves the current FIFO maintenance queue. |
| `POST` | `/api/maintenance`| `{"pipelineId":"PL-04","problemDescription":"...",...}` | Enqueues a maintenance task at the tail of the FIFO queue. |
| `POST` | `/api/maintenance/process` | — | Dequeues the front task (Queued $\to$ In Progress $\to$ Completed & restored). |
| `PUT` | `/api/maintenance/repair` | `{"id":"MNT-301"}` | Marks specific task repaired and restores pipeline to `Working`. |
| `GET` | `/api/network/connectivity` | — | Executes BFS to return `reachable` and `disconnected` area IDs. |
| `GET` | `/api/allocation` | — | Returns allocation simulation preview without deducting water. |
| `POST` | `/api/allocation/run` | — | Runs PriorityQueue allocation and commits deductions to reservoirs. |
| `POST` | `/api/reset` | — | Resets memory state back to initial sample demonstration data. |

---

## 🧪 7. Test Scenarios & Results (`BackendTestSuite.java`)

All 10 scenarios are verified through [`BackendTestSuite.java`](file:///c:/Users/Welcome/Desktop/Smart_Watermanage/backend/src/com/smartwater/test/BackendTestSuite.java):

```text
=================================================================
🧪 RUNNING SMART WATER DISTRIBUTION JAVA BACKEND TEST SUITE 🧪
=================================================================
[Test 1] Normal Water Supply & Initialization: PASSED ✓
[Test 2] Low Reservoir Water Bounds & Validation: PASSED ✓
[Test 3] Critical Demand Priority over Lower-Priority (PriorityQueue): PASSED ✓
[Test 4] Failed Pipeline PL-04 causing Area 4 Disconnection (Graph BFS): PASSED ✓
[Test 5] Water Allocation Execution & Partial Allocation: PASSED ✓
[Test 6] Multi-Reservoir Switching (Area 2 Hospital Backup PL-06 from RES-B): PASSED ✓
[Test 7] Maintenance Queue FIFO Dispatcher (ArrayDeque): PASSED ✓
[Test 8] Restoring Connectivity & Reallocating Area 4: PASSED ✓
[Test 9] Demand Area Validation & Inheritance: PASSED ✓
[Test 10] Complaint Creation & Status Transition: PASSED ✓
=================================================================
RESULTS: 10 / 10 TESTS PASSED.
🎉 ALL 10 PHASE 2 BACKEND TEST SCENARIOS PASSED SUCCESSFULLY!
=================================================================
```

---

##  8. Step-by-Step Viva Demonstration Script

1. **Start the Backend:** Run `java -cp backend/bin com.smartwater.Main` (or run `Main.java` in your IDE). Show the console output to the faculty, explaining the initialization of the Adjacency List, BFS, PriorityQueue, and ArrayDeque.
2. **Access the Website:** Open `http://localhost:8080/`. Point out the **`🟢 Java Backend Active (Port 8080)`** badge in the header, demonstrating that `fetch()` is communicating with the Java server.
3. **Show Graph Reachability (BFS):**
   * Log into the Admin Dashboard (`admin` / `admin123`).
   * Go to **Water Network Flow Graph**.
   * Show that `PL-04` is red (`Failed`) and Area 4 (*Apex Model School*) displays a **⚠️ DISCONNECTED** badge.
   * Explain to the faculty: *"Our BFS traversal in `findReachableAreasBFS()` started from active reservoirs and discovered that no working path reaches Area 4."*
4. **Show Priority Allocation (PriorityQueue):**
   * Go to **Water Allocation**.
   * Click ** Run Water Allocation Algorithm**.
   * Note that Area 2 (*City Care Hospital - Critical*) and Area 1 (*High*) are fully allocated first.
   * Note that Area 4 receives **0 L** because it is disconnected.
5. **Demonstrate Multi-Reservoir Switching (Failover):**
   * Mention that Area 2 (*Hospital*) has primary pipeline `PL-02` from Reservoir A and backup pipeline `PL-06` from Reservoir B. If Reservoir A is depleted or `PL-02` fails, the Java policy automatically routes water from Reservoir B.
6. **Process the FIFO Maintenance Queue (`ArrayDeque`):**
   * Go to **Maintenance Queue (FIFO)**.
   * Point out `MNT-301` at position **#1 (FRONT)** of the FIFO queue.
   * Click ** Process Next in Queue (FIFO Pop)** $\to$ Moves to `In Progress`.
   * Click ** Process Next in Queue (FIFO Pop)** again $\to$ Marked `Completed`, and pipeline `PL-04` is restored to `Working` condition in the graph!
7. **Verify Reconnection & Reallocation:**
   * Return to **Water Network Flow Graph** $\to$ `PL-04` is now green and Area 4 is **Connected**.
   * Return to **Water Allocation** and click **🚀 Run Water Allocation Algorithm** $\to$ Area 4 now receives its full water allocation!
