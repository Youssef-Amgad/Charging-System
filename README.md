# Simple Online Charging System (OCS)

A Java SE simulation of a telecom charging server using TCP (signalling) and UDP (media).

---

## Architecture

```
┌────────────────────────────────────────────────────────┐
│                   Charging Server                      │
│                                                        │
│  TCPHandler (per-UE thread)   UDPHandler (single thread)│
│       │                              │                 │
│  SessionManager ◄────────────────────┘                 │
│       │                                                │
│  ChargingTask (ScheduledExecutorService per call)      │
│       │                                                │
│  CustomerDAO → PostgreSQL (NeonDB)                     │
└────────────────────────────────────────────────────────┘

┌─────────────────────────────┐
│         UE Client           │
│  UEClient (main thread)     │
│  VoiceSender (UDP thread)   │
└─────────────────────────────┘
```

---

## Project Structure

```
ocs/
├── sql/
│   └── setup.sql                    ← Run this on NeonDB first
│
├── charging-server/
│   ├── pom.xml
│   └── src/main/java/com/ocs/
│       ├── db/
│       │   ├── DatabaseConnection.java
│       │   └── CustomerDAO.java
│       ├── model/
│       │   ├── Customer.java
│       │   └── CallSession.java
│       └── server/
│           ├── ChargingServer.java   ← Main class
│           ├── charging/
│           │   ├── ChargingTask.java
│           │   └── SessionManager.java
│           ├── tcp/
│           │   └── TCPHandler.java
│           └── udp/
│               └── UDPHandler.java
│
└── ue-client/
    ├── pom.xml
    └── src/main/java/com/ocs/client/
        ├── UEClient.java             ← Main class
        └── VoiceSender.java
```

---

## Setup

### 1. NeonDB Configuration

Run `sql/setup.sql` in your NeonDB SQL console:

```sql
CREATE TABLE IF NOT EXISTS customers (
    msisdn  VARCHAR(20)    PRIMARY KEY,
    balance DECIMAL(12,2)  NOT NULL
);

INSERT INTO customers VALUES ('01000202', 55.00) ON CONFLICT DO NOTHING;
```

### 2. Configure DB credentials

Set these environment variables before running the server:

```bash
export DB_HOST="ep-xxxx-xxxx.us-east-2.aws.neon.tech"
export DB_NAME="neondb"
export DB_USER="neondb_owner"
export DB_PASS="your_password"
```

Or pass as JVM system properties:
```bash
java -DDB_HOST=... -DDB_NAME=... -DDB_USER=... -DDB_PASS=... -jar charging-server.jar
```

---

## Build

Requires: **Java 17+** and **Maven 3.8+**

### Build Charging Server

```bash
cd charging-server
mvn clean package -q
# Output: target/charging-server.jar
```

### Build UE Client

```bash
cd ue-client
mvn clean package -q
# Output: target/ue.jar
```

---

## Run

### Terminal 1 – Start the Charging Server

```bash
java -jar charging-server/target/charging-server.jar
```

Optional flags:
```bash
# Change ports
java -Dtcp.port=5000 -Dudp.port=5001 -jar charging-server.jar

# Use faster charging for testing (every 10 seconds)
java -Dcharge.interval.seconds=10 -jar charging-server.jar
```

### Terminal 2 – Start a UE

```bash
java -jar ue-client/target/ue.jar 01000202
```

Press **ENTER** to end the call.

### Multiple concurrent calls

```bash
# Terminal 2
java -jar ue.jar 01000202

# Terminal 3
java -jar ue.jar 01001111
```

---

## Expected Output

### Server

```
[SERVER] TCP listening on port 5000
[SERVER] UDP listening on port 5001
[TCP] New connection from /127.0.0.1:51234
[TCP] Received: START_CALL:01000202
[SESSION] Session added for MSISDN=01000202
[TCP] ANSWERED sent for MSISDN=01000202
[CHARGING] Scheduler started for MSISDN=01000202 (every 60s)
[UDP] Received Voice Segment from MSISDN=01000202 → VOICE_SEGMENT_1
[UDP] Received Voice Segment from MSISDN=01000202 → VOICE_SEGMENT_2
[CHARGING] MSISDN=01000202  Old Balance=55.00  New Balance=54.00
[TCP] Received: END_CALL:01000202
[SESSION] Session terminated for MSISDN=01000202
```

### UE Client

```
[TCP] Connected to 127.0.0.1:5000
[TCP] Sent: START_CALL:01000202
[TCP] Received: ANSWERED
[UE] Call is active! Press ENTER to end the call.
[UDP] Sent: 01000202:VOICE_SEGMENT_1
[UDP] Sent: 01000202:VOICE_SEGMENT_2
                                         ← user presses ENTER
[TCP] Sent: END_CALL:01000202
[UE] Goodbye.
```

---

## Threading Model

| Thread | Scope | Purpose |
|--------|-------|---------|
| `tcp-handler-N` | Per UE | Handles TCP signalling lifecycle |
| `voice-sender`  | Per UE (client) | Sends UDP voice packets |
| `udp-listener`  | Global (server) | Receives all UDP voice packets |
| `charging-MSISDN` | Per active call | `ScheduledExecutorService` deduction |
| `server-listener` | Per UE (client) | Watches for server-push messages |

---

## Low Balance Flow

```
Server ChargingTask:
  balance <= 0  →  cancel scheduler
              →  send "CALL_TERMINATED_LOW_BALANCE" via TCP
              →  SessionManager.removeSession()

UE Client server-listener thread:
  receives "CALL_TERMINATED_LOW_BALANCE"
  →  VoiceSender.stop()
  →  System.exit(0)
```
