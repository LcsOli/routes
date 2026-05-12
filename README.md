# 🚛 Routes Sync: Logistical Integration Platform

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.2-green?style=for-the-badge&logo=springboot)
![Oracle](https://img.shields.io/badge/Oracle-Database-red?style=for-the-badge&logo=oracle)
![Architecture](https://img.shields.io/badge/Architecture-Clean-blue?style=for-the-badge)

**Routes Sync** is a high-performance middleware designed to bridge the gap between **WinThor ERP (Oracle)** and the **Concept Logistical Platform (SOAP API)**. It automates the complex synchronization of shipments, drivers, and delivery routes with a focus on resilience, security, and real-time monitoring.


<h2>🚀 Overview </h2>

The project was designed to replace legacy scripts with a modern, resilient architecture. It manages the entire lifecycle of logistical integration, ensuring data integrity, handling complex SOAP communication, and providing real-time visibility into fleet status through a high-density "Operational Control Tower" dashboard.

<h3>🛠 Tech Stack</h3>

Backend: Java 21 (LTS), Spring Boot 3.4.2.

Security: Native Spring Boot Environment Variable management (via spring.config.import).

Database: Oracle Database (WinThor ERP) via OJDBC 11.

Integration: SOAP/XML via Spring RestTemplate (using Java 17 Text Blocks for envelope clarity).

Frontend: HTML5, Tailwind CSS, and Vanilla JavaScript (Asynchronous polling architecture).

<h3>🏗 Architecture & Design Patterns</h3>

The project follows Clean Architecture and SOLID principles to ensure long-term maintainability:

Domain Layer: Contains PedidoDTO and PedidoMapper. The mapper handles strict business logic for XML character escaping and US Locale decimal formatting.

Application Layer: Orchestrated by RoteirizacaoUseCase, managing the execution flow, entity synchronization, and business rules.

Infrastructure Layer:

Repository: Native SQL queries optimized for Oracle performance.

SOAP Client: Customized HTTP clients with specific timeouts for heavy logistical payloads and SOAPAction header management.

Scheduler: Automatic background processing of pending integration queues.

<h3>⚙️ Key Technical Features</h3>

1. Native Security Strategy

Sensitive credentials (DB passwords, API tokens) are decoupled from the source code. The project utilizes a native Spring Boot approach to import .env files directly into the application context, ensuring zero-footprint security in version control.

2. Anti-Timeout Batch Strategy

To ensure stability against the Concept SOAP server's response limits, the system implements a 2-by-2 shipment batching strategy. This ensures that processing remains within safe time windows, preventing HTTP read timeouts.

3. "Auto-Healing" Entity Synchronization

Before attempting to route a shipment, the system automatically verifies and registers missing Zones, Stores, or Drivers in the Concept API. This proactive check drastically reduces integration failure rates.

4. High-Density Dashboard

The monitoring interface features an asynchronous "Monitor-then-Poll" strategy. It loads shipment metadata from Oracle instantly and updates the delivery progress of each vehicle in the background, allowing for efficient fleet management without server overhead.

<h3>🔧 Installation & Setup</h3>

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

mvn clean install
java -jar target/routes-0.0.1-SNAPSHOT.jar


<h3>📊 Operational Status Guide</h3>

The dashboard uses color-coded rows based on real-time progress:

🟢 Finished (Green): 100% of deliveries successfully completed.

🟡 In Progress (Amber): Deliveries started but with pending drops remaining.

🔴 Not Started (Rose): Successfully integrated/routed but no delivery activity recorded yet.
