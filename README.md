# 💧 Smart Tank Monitoring System

> A distributed IoT system for rainwater tank monitoring and automated water channel control — built across ESP32, Arduino, a Java backend, and a web dashboard.

---

## What is Smart Tank Monitoring?

**Smart Tank Monitoring** is a multi-subsystem IoT project that monitors the water level of a rainwater tank and automatically controls a drainage channel valve based on configurable thresholds. The system supports both **automatic** and **manual** operation modes, with real-time data flowing between embedded hardware and a web dashboard.

The system is composed of four subsystems communicating over MQTT, HTTP, and serial:

```
[ESP32 - TMS] ──MQTT──► [Java Backend - CUS] ──HTTP──► [Web Dashboard - DBS]
                                  │
                               Serial
                                  │
                                  ▼
                        [Arduino - WCS]
```

---

## Subsystems

### 🛰 TMS — Tank Monitoring Subsystem (`esp/`)
Runs on an **ESP32**. Continuously samples the water level via a sonar and publishes readings to the CUS over **MQTT**.

- Green LED on = network healthy, data flowing
- Red LED on = network error or MQTT failure
- Built on a task-based architecture using a scheduler

**Tasks:**
- `ConnectionTask` — manages Wi-Fi and MQTT connection, handles reconnection
- `SensorTask` — periodically reads the sonar distance
- `PublishTask` — publishes water level readings to the CUS via MQTT

---

### ⚙️ WCS — Water Channel Subsystem (`arduino/`)
Runs on an **Arduino UNO**. Controls the valve (servo motor) that drains water from the tank into the channel network.

- Valve opening: `0%` (closed, 0°) to `100%` (fully open, 90°)
- LCD shows current valve opening % and mode (`AUTOMATIC` / `MANUAL` / `UNCONNECTED`)
- Button toggles between `AUTOMATIC` and `MANUAL` mode
- In `MANUAL` mode, the potentiometer directly controls the valve opening
- Communicates with CUS over **serial**

**Tasks:**
- `FSMController` — core state machine managing valve and mode transitions
- `PotReader` — samples potentiometer value for manual control
- `SerialTask` — handles serial communication with CUS

---

### 🖥 CUS — Control Unit Subsystem (`java/`)
Runs on a **PC** as a Java/Maven backend. The central coordinator of the whole system.

**Responsibilities:**
- Subscribes to water level data from TMS over **MQTT**
- Applies automatic control logic:
  - Level > `L1` for more than `T1` → open valve to **50%** until level drops below `L1`
  - Level > `L2` → immediately open valve to **100%** until level drops below `L2`
- Sends valve commands to WCS over **serial**
- Exposes an **HTTP REST API** for the dashboard
- Tracks system state: `AUTOMATIC`, `MANUAL`, `UNCONNECTED`
- If no data received from TMS for more than `T2` → enters `UNCONNECTED` state

**Source:**
```
java/ControlUnitSubsystem/http-backend/src/main/java/esiot/backend/
├── CUS.java          # Main backend logic, MQTT + serial + HTTP coordination
├── DataPoint.java    # Water level measurement model
└── DataService.java  # HTTP API service layer
```

Build with Maven:
```bash
cd java/ControlUnitSubsystem/http-backend
mvn package
mvn exec:java
```

---

### 🌐 DBS — Dashboard Subsystem (`http/`)
A lightweight **web frontend** (vanilla JS + HTML/CSS). Connects to the CUS over HTTP.

**Displays:**
- Real-time graph of the last N water level measurements
- Current valve opening percentage
- System state: `AUTOMATIC`, `MANUAL`, `UNCONNECTED`, or `NOT AVAILABLE`

**Controls:**
- Button to toggle between `AUTOMATIC` and `MANUAL` mode
- Slider to set valve opening when in `MANUAL` mode

Run by opening `index.html` in a browser, or serve with any static file server:
```bash
cd http/DashboardSubsystem
npx serve .
```

---

## Project Architecture

Each embedded subsystem (TMS and WCS) shares the same **task-based kernel** pattern with a cooperative scheduler, consistent with the architecture used across the project series:

```
src/
├── config.h              # Tunable parameters (thresholds, timings, pins)
├── main.cpp              # Entry point and scheduler setup
├── devices/              # Hardware abstraction (sonar, LEDs, servo, button, pot, LCD)
├── kernel/               # Cooperative scheduler, task base, messaging, logger
├── model/                # Shared context and platform abstraction
└── tasks/                # FSM tasks (one concern per task)
```

---

## Build & Flash

### ESP32 — TMS (PlatformIO)

> ⚠️ Copy `credentials.ini.example` to `credentials.ini` and fill in your Wi-Fi credentials before building.

```bash
cd esp/TankMonitoringSubsystem
pio run --target upload
```

### Arduino — WCS (PlatformIO)

```bash
cd arduino/WaterChannelSubsystem
pio run --target upload
```

### Java — CUS (Maven)

```bash
cd java/ControlUnitSubsystem/http-backend
mvn package
mvn exec:java
```

### Web — DBS

Open `http/DashboardSubsystem/index.html` in a browser, or serve it statically.

---

## Authors

- **Arthur Istvan Muller**
- **Giuseppe Cattolico**
