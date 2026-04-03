# 💧 Smart Tank Monitoring System

> A distributed IoT system for rainwater tank monitoring and automated water channel control — built across ESP32, Arduino, a Java/Vert.x backend, and a web dashboard.

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

## Hardware Overview

### ESP32 - TMS (Tank Monitoring Subsystem)
| Component | Pin | Description |
|-----------|-----|-------------|
| Sonar HC-SR04 | Trig: 5, Echo: 18 | Water level measurement via ultrasonic pulses |
| Green LED | 17 | Network healthy, data flowing |
| Red LED | 19 | Network error or MQTT failure |

### Arduino UNO - WCS (Water Channel Subsystem)
| Component | Pin | Description |
|-----------|-----|-------------|
| Servomotor (valve) | 10 | Valve control: 0° (0%) to 90° (100%) |
| Potentiometer | A0 | Manual valve control in MANUAL mode |
| Tactile Button | 8 | Toggle AUTOMATIC/MANUAL mode |
| LCD I2C (16x2) | Address: 0x27 | Display valve % and mode |

---

## Subsystems

### 🛰 TMS — Tank Monitoring Subsystem (`esp/`)

Runs on an **ESP32**. Continuously samples the water level via a sonar and publishes readings to the CUS over **MQTT**.

- Green LED on = network healthy, data flowing
- Red LED on = network error or MQTT failure
- Built on a task-based architecture using a scheduler

**Tasks:**
- `ConnectionTask` — manages Wi-Fi and MQTT connection, handles reconnection (period: 1000ms)
- `SensorTask` — periodically reads the sonar distance (period: 200ms)
- `PublishTask` — publishes water level readings to the CUS via MQTT (period: 2000ms)

**FSM States:**
- `CHECKING` — nominal state, verifies Wi-Fi and MQTT connectivity
- `RECONNECTING_WIFI` — attempts Wi-Fi recovery
- `RECONNECTING_MQTT` — attempts MQTT broker reconnection

**Libraries:** PubSubClient ^2.8, TimerOne ^1.2

---

### ⚙️ WCS — Water Channel Subsystem (`arduino/`)

Runs on an **Arduino UNO**. Controls the valve (servo motor) that drains water from the tank into the channel network.

- Valve opening: `0%` (closed, 0°) to `100%` (fully open, 90°)
- LCD shows current valve opening % and mode (`AUTOMATIC` / `MANUAL` / `UNCONNECTED`)
- Button toggles between `AUTOMATIC` and `MANUAL` mode
- In `MANUAL` mode, the potentiometer directly controls the valve opening
- Communicates with CUS over **serial** at 115200 baud

**Tasks:**
- `FSMController` — core state machine managing valve and mode transitions (period: 75ms)
- `PotReader` — samples potentiometer value for manual control (period: 200ms)
- `SerialTask` — handles serial communication with CUS (period: 50ms)

**FSM States:**
- `AUTOMATIC` — valve controlled by CUS via serial commands
- `MANUAL` — valve controlled locally via potentiometer
- `UNCONNECTED` — CUS unreachable (serial timeout)

**Serial Protocol:** `MODE,VALVE:XX` (e.g., `AUTOMATIC,VALVE:50`)

**Libraries:** TimerOne, LiquidCrystal_I2C, EnableInterrupt, ServoTimer2

---

### 🖥 CUS — Control Unit Subsystem (`java/`)

Runs on a **PC** as a Java/Vert.x backend. The central coordinator of the whole system.

**Framework:** Vert.x 4.2.6 (event-driven, non-blocking I/O)

**Responsibilities:**
- Subscribes to water level data from TMS over **MQTT**
- Applies automatic control logic:
  - Level > `L1` (20%) for more than `T1` → open valve to **50%** until level drops below `L1`
  - Level > `L2` (30%) → immediately open valve to **100%** until level drops below `L2`
- Sends valve commands to WCS over **serial**
- Exposes an **HTTP REST API** for the dashboard
- Tracks system state: `AUTOMATIC`, `MANUAL`, `UNCONNECTED`
- If no data received from TMS for more than `T2` → enters `UNCONNECTED` state

**Communication Protocols:**
- **MQTT** — Eclipse Paho client, callback-based
- **Serial** — jssc library with BlockingQueue for thread decoupling
- **HTTP** — Vert.x Web + WebClient

**REST API Endpoints:**
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/data` | Last N water level measurements (circular buffer, max 10) |
| GET | `/api/status` | Current system state (AUTOMATIC/MANUAL/UNCONNECTED) |
| POST | `/api/status` | Toggle between AUTOMATIC and MANUAL |
| POST | `/api/valve` | Set valve opening percentage in MANUAL mode |

**Source:**
```
java/ControlUnitSubsystem/http-backend/src/main/java/esiot/backend/
├── CUS.java          # Main backend logic, MQTT + serial + HTTP coordination
├── CUSService.java   # Façade for orchestration
├── DataPoint.java    # Water level measurement model
├── DataService.java  # HTTP API service layer (Verticle)
├── MQTTProtocol.java # MQTT client wrapper
├── SerialProtocol.java # Serial communication handler
└── HTTPProtocol.java # HTTP client wrapper
```

**Build with Maven:**
```bash
cd java/ControlUnitSubsystem/http-backend
mvn package
mvn exec:java
```

---

### 🌐 DBS — Dashboard Subsystem (`http/`)

A lightweight **web frontend** (vanilla JS + HTML/CSS + Chart.js). Connects to the CUS over HTTP.

**Displays:**
- Real-time line chart of the last N water level measurements (Chart.js)
- Current valve opening percentage (progress bar)
- System state: `AUTOMATIC`, `MANUAL`, `UNCONNECTED`, or `NOT AVAILABLE`

**Controls:**
- Button to toggle between `AUTOMATIC` and `MANUAL` mode
- Slider to set valve opening when in `MANUAL` mode

**Polling:** Every 1 second to `/api/data` and `/api/status`

Run by opening `index.html` in a browser, or serve with any static file server:
```bash
cd http/DashboardSubsystem
npx serve .
```

---

## Project Architecture

Each embedded subsystem (TMS and WCS) shares the same **task-based kernel** pattern with a cooperative scheduler:

```
src/
├── config.h              # Tunable parameters (thresholds, timings, pins)
├── main.cpp              # Entry point and scheduler setup
├── devices/              # Hardware abstraction (sonar, LEDs, servo, button, pot, LCD)
├── kernel/               # Cooperative scheduler, task base, messaging, logger
├── model/                # Shared context and platform abstraction
└── tasks/                # FSM tasks (one concern per task)
```

### Common Patterns

1. **Cooperative Task Scheduler** — TimerOne-based with 50ms base period
2. **Enum-based FSM** — lightweight state machines with `justEntered` pattern
3. **Context/Shared State** — repository for inter-task communication
4. **Façade Pattern** — `TankMonitoringPlatform` and `WaterChannelPlatform`
5. **Interface Segregation** — abstract device interfaces (Light, ProximitySensor, Button)

---

## Configuration Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| L1 | 20% | Threshold for 50% valve opening |
| L2 | 30% | Threshold for 100% valve opening |
| T1 | configurable | Time above L1 before valve opens |
| T2 | configurable | Timeout for UNCONNECTED state |
| Scheduler base period | 50ms | Base tick for all tasks |

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

## Challenges & Solutions

- **Timer conflicts on Arduino:** TimerOne (scheduler) conflicted with standard Servo library → solved using ServoTimer2 (Timer2)
- **Thread synchronization:** jssc serial callbacks run in separate thread → used BlockingQueue for Vert.x thread decoupling
- **Distributed UNCONNECTED state:** Coordinated timeout handling across MQTT, serial, and HTTP interfaces

---

## Authors

- **Arthur Istvan Muller**
- **Giuseppe Cattolico**