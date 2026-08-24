# BhoomiBot AI & Autonomy: Strategic Roadmap

**Last Updated:** 2026-08-10
**Audit Status:** Verified Stable Infrastructure

---

## 1. The Real Status Board (Verified Ground Truth)

| Layer | Component | Status | Reality Check |
| :--- | :--- | :--- | :--- |
| **L0** | **Agent** | **Hybrid** | Cloud Llama-3 Active + Regex Fallback. |
| **L1** | **Knowledge** | **Stub** | In-memory only. No persistence. |
| **L2** | **Planner** | **Basic** | Fixed unrolling for Transport/Navigate. |
| **L3** | **Skills** | **Infra** | `NAVIGATE` skill returns dummy `(0,0)` path. |
| **L4** | **Perception** | **Shell** | TFLite loaded but `analyze()` is empty. |
| **L5** | **Localization**| **Functional**| GPS active. No IMU/Encoder fusion. |
| **L6** | **World Model** | **Stub** | Mirroring only. No spatial memory. |
| **L7** | **Control** | **Stub** | Returns constant 0.5m/s. No steering math. |
| **L8** | **Hardware** | **Active** | VCU ASCII Protocol 100% operational. |

---

## 2. Immediate Action Plan (Next 30 Days)

### Phase A: Vision & Perception (The "Eyes")
*   **A.1: Bitmap-to-Tensor Pipeline:** Implement the image preprocessing in `TfLiteWeedModule`.
*   **A.2: Weed Inference:** Run the first live detection and output `Observation` objects to the World Model.
*   **A.3: Semantic UI:** Visualize bounding boxes on the `OperatorLiveScreen`.

### Phase B: Closed-Loop Control (The "Feet")
*   **B.1: Pure Pursuit Controller:** Implement the math in `ControlLayerImpl` to calculate steering angles from GPS waypoints.
*   **B.2: Speed Scaling:** Adjust linear velocity based on distance to the next waypoint.
*   **B.3: VCU Feedback:** Feed motor RPM back into the control loop for precision.

### Phase C: Persistent Intelligence (The "Memory")
*   **C.1: SQLite/Room Migration:** Move `KnowledgeLayerImpl` from memory to a local database.
*   **C.2: Location Persistence:** Allow the robot to remember "Named Waypoints" across app restarts.
*   **C.3: Skill Storage:** Save user-taught manual paths as reusable L3 Skills.

---

## 3. Engineering Constraints
1. **Platform Independence:** AI Core code must remain 100% pure Kotlin (No `android.*` imports).
2. **Safety First:** Vision-based obstacle detection must have a direct path to the `HardwareLayer` for emergency braking.
3. **Low Latency:** Inference on L4 must maintain >10 FPS on target hardware (I2208/I2306).

---
*Senior Auditor Note: Infrastructure migration is complete. The system is now ready for Algorithmic implementation.*
