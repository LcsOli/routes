🚛 Routes Sync: Logistical Integration Platform
Routes Sync is a high-performance, automated middleware designed to bridge the gap between WinThor ERP (Oracle 19c) and the Concept Logistical Platform (SOAP API). It orchestrates the complex synchronization of shipments, drivers, delivery routes, and points of interest (POIs) with a strict focus on transaction resilience, autonomous error recovery, and real-time monitoring.

🚀 Overview
The project was engineered to replace legacy manual routing processes with a modern, resilient, event-driven architecture. It manages the entire lifecycle of logistical integration: consuming queues from the ERP, sanitizing data, guaranteeing structural prerequisites in the external API, dispatching massive XML payloads, and maintaining an isolated audit trail for business rule rejections—all without blocking the operational pipeline.

🛠 Tech Stack
Backend: Java 21 (Leveraging modern features, Records, and Text Blocks).

Framework: Spring Boot 3.4.2.

Database: Oracle Database 19c (WinThor ERP) via OJDBC 11 & Spring Data JPA (Native Queries).

Integration: Web Services SOAP (Concept API) via customized clients.

Security: Native Spring Boot Environment Variable management (spring.config.import).

Frontend (Dashboard): HTML5, Tailwind CSS, and Vanilla JavaScript (Asynchronous Monitor-then-Poll architecture).

🏗 Architecture & Design Patterns
The project follows Clean Architecture and SOLID principles to ensure long-term maintainability and strict separation of concerns:

Domain Layer: Contains PedidoDTO and PedidoMapper. The mapper handles strict business logic for XML character escaping, POI fallback coordinates, and US Locale decimal formatting.

Application Layer: * RoteirizacaoUseCase: Orchestrates the execution flow, entity synchronization, and payload assembly.

LogRoteirizacaoService: Handles isolated audit logging for failed integrations.

Infrastructure Layer:

Repository: Highly optimized Native SQL queries bypassing standard ORM mappings for performance on legacy schemas.

SOAP Client: Customized HTTP clients handling raw XML envelope manipulation.

Scheduler: Automated background cron jobs (@Scheduled) consuming pending integration queues.

🔄 The Routing Lifecycle (Workflow)
The core of the system is automated and event-driven, following these sequential steps:

Queue Polling (RoteirizacaoScheduler): Wakes up at programmed intervals to fetch a list of pending shipment IDs (carregamentos) from WinThor based on control flags (e.g., IMPORTADOAPI = 'N').

Transaction Isolation: The RoteirizacaoUseCase iterates through the pending queue. Each shipment is processed inside its own @Transactional boundary to ensure that a failure in one load does not roll back the entire batch.

Data Extraction & Mapping: Executes a massive Native SQL SELECT joining ERP tables (PCCARREG, PCPEDC, PCCLIENT). Results are instantly mapped to structured PedidoDTO objects containing financial, dimensional (weight/volume), and geographical data.

Auto-Healing & Validation:

License Plate Sanitization: Ensures strict XXX-XXXX format via Regex.

Pre-sync: Uses Java Streams API to filter distinct Zones and Stores associated with the shipment, firing preemptive SOAP requests to guarantee these entities (and the Driver) exist in the Concept platform before the payload arrives.

Payload Generation: Iterates through the DTOs to build a bulk XML request. It strategically maps the Client Name to the <descricao> tag for optimal UI display in the Concept platform, while mapping the actual address to the <endereco> tag for accurate geocoding.

Geographic Engine Trigger: Dispatches the importarPedidos SOAP request, immediately followed by roteirizarPedidos. The Concept engine processes the geometry and returns a Routing/Trip ID.

ERP Commit: Executes a Native UPDATE on WinThor, flipping the shipment status flags to indicate a successful route generation (IMPORTADOAPI = 'S').

⚙️ Key Technical Features
1. Autonomous Resilience & Audit Logging (Bypassing Oracle Limits)
If the Concept routing engine rejects a shipment (e.g., Invalid License Plate or Unregistered Vehicle), the system triggers a protective contingency flow:

REQUIRES_NEW Propagation: The exception is caught and passed to the LogRoteirizacaoService, which opens an entirely new, independent database transaction. This guarantees the error is logged even when the main routing transaction is rolled back.

Trigger/Sequence Bypass: To circumvent restrictive permissions and missing legacy objects (SEQUENCES) in the WinThor Oracle database, the Java repository generates the Primary Key dynamically using Oracle's native math functions: TRUNC(DBMS_RANDOM.VALUE(1, 999999999)).

2. Native Security Strategy
Sensitive credentials (DB passwords, API tokens) are decoupled from the source code. The project utilizes a native Spring Boot approach to import .env files directly into the application context, ensuring zero-footprint security in version control.

3. Anti-Timeout Batch Strategy
To ensure stability against the Concept SOAP server's response limits, the system implements a controlled shipment batching strategy. This ensures that processing remains within safe HTTP time windows, preventing read timeouts during massive load imports.

4. High-Density Operational Dashboard
The monitoring interface features an asynchronous "Monitor-then-Poll" strategy. It loads shipment metadata from Oracle instantly and updates the delivery progress of each vehicle in the background, allowing for efficient fleet management without server overhead.

💾 Audit Database Structure
If you need to manually validate or deploy the audit table in the Oracle database, use the following DDL:

CREATE TABLE COMPREFACIL.CF_LOG_ROTEIRIZACAO (
    ID NUMBER(19,0) NOT NULL,
    NUMCAR NUMBER(19,0),
    ERRO VARCHAR2(4000 BYTE),
    HORA_ENVIO TIMESTAMP(6),
    CONSTRAINT SYS_C00115546 PRIMARY KEY (ID)
);

🔧 Installation & Setup
1. Environment Configuration
Create a .env file in the root directory (same level as pom.xml):

DB_URL=jdbc:oracle:thin:@your_host:1521/WINT
DB_USER=your_user
DB_PASS=your_password

CONCEPT_CNPJ=00.000.000/0000-00
CONCEPT_SENHA_CLIENTE=your_client_token
CONCEPT_SENHA_CENTRAL=your_central_token
CONCEPT_AUTOMATION_INTERVAL=60000

2. Build and Run
Clean any legacy artifacts and start the Spring Boot application:
mvn clean install
mvn spring-boot:run

(The embedded Tomcat server will start on port 8081 and the automated Scheduler will begin polling immediately).

📊 Operational Status Guide (Dashboard)
The frontend dashboard uses color-coded rows based on real-time routing and delivery progress:

🟢 Finished (Green): 100% of deliveries successfully completed.

🟡 In Progress (Amber): Deliveries started but with pending drops remaining.

🔴 Not Started (Rose): Successfully integrated/routed but no delivery activity recorded yet.