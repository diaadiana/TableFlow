TableFlow: Professional Digital Restaurant Management Architecture
 
The TableFlow system represents a comprehensive architectural solution for modernizing restaurant floor operations. Built on a zero-dependency Java 8 foundation, this platform provides a high-performance, non-blocking environment for real-time guest management, table state synchronization, and long-term data persistence. By eliminating the need for external libraries and heavy frameworks, TableFlow ensures 100 percent portability and rapid execution across any environment with a bare Java Runtime Environment (JRE).

Project Vision and Strategic Scope
 
TableFlow was developed to bridge the gap between traditional manual reservation logs and complex, monolithic management software. The strategic objective is to maximize venue efficiency by automating the table assignment process. In most mid-sized venues, managers often lose capacity by seating small groups at large tables during peak hours. TableFlow solves this through a custom-built, heuristic-driven seating engine that validates guest counts against exact table capacities. This ensures that larger tables are preserved for larger groups, optimizing the establishments revenue potential while providing a seamless, visual interface for the operator.

The transition from the initial Bella Vista concept to the current TableFlow architecture reflects a move towards a more robust, professional-grade solution. This includes the implementation of a persistent JSON data store, a built-in HTTP server, and a reactive SVG-based floor plan that provides instantaneous feedback on venue occupancy.

Detailed System Functionality
 
The system is divided into several specialized functional layers that internalize complex management tasks:

1. Interactive Floor Plan Engine
The front-facing component utilizes Scalable Vector Graphics (SVG) to render a precise architectural map of the venue. Unlike static maps, each table in the SVG is an intelligent DOM object mapped directly to a Java model on the server. The interface is completely reactive; it synchronizes with the Java backend on every interaction, ensuring that the floor plan always reflects the exact state of the restaurant. Identity colors (Green, Lilac, Yellow, Orange, Blue, Rose, and Teal) are permanently assigned to tables to create a visual continuity between the map and the management lists.

2. Smart Seating Heuristics and Fallback Logic
The core intelligence of TableFlow resides in its Seating Engine. When a manager selects a table for a new reservation, the system does not simply accept the choice. It executes a three-tier validation process:
First, it evaluates if the choice is Optimized. An optimized choice means no more than one seat is left empty.
Second, if the choice is inefficient, the system automatically redirects to the best available table in the same zone (Indoor or Outdoor) that meets the optimized criteria.
Third, to ensure guest satisfaction, if no optimized table exists, the system will fallback to any table that fits the group, avoiding unnecessary error messages and ensuring the reservation is secured.

3. Persistent Data Archiving
Data integrity is handled by a specialized persistence layer. Every modification—whether creating a new booking or cancelling an old one—triggers an immediate atomic write to the rezervari.json file. This ensures that the restaurant state is preserved across server restarts and provides a transparent audit trail for all business activities.

Object-Oriented Design Blueprint (OOP) with Implementation Examples
 
TableFlow is a strictly modeled Java application that serves as a benchmark for core Object-Oriented Programming (OOP) principles. Below are detailed explanations of these concepts with direct examples from the project structure:

A. Advanced Encapsulation
Encapsulation is the practice of bundling data and methods within a single unit while restricting access to internal state. In TableFlow, this is achieved through private scope modifiers and public access interfaces.
Example: Inside the Rezervare (Reservation) class, properties like numeClient (name) and nrPersoane (guest count) are protected.
- Field: private String numeClient;
- Constructor: Sets the name once during creation.
- Getter: public String getNumeClient() { return numeClient; }
This ensures that a client name cannot be accidentally overwritten or corrupted once the reservation object is initiated in memory.

B. Structural Abstraction
Abstraction involves reducing complex real-world categories into simplified software models.
Example: The system uses Java Enums to abstract venue properties. The Amplasare (Placement) enum (INDOOR, OUTDOOR) and the SpecificulRezervarii (Reservation Specifics) enum are used to categorize data without relying on fragile string comparisons. This provides compile-time safety and ensures that the business logic only deals with valid, predefined categories.

C. Entity Composition (Has-A Relationship)
Composition allows for the building of complex objects from simpler ones.
Example: The AranjareMese (Table) class models a physical table. It contains a List of Rezervare objects. This is a classic Composition pattern: A Table has a list of Reservations.
- Structure: class AranjareMese { private List Rezervari; }
This allows each table to manage its own schedule and capacity checks internally, rather than relying on a global, monolithic list.

D. Behavioral Polymorphism and Method Overriding
Polymorphism allows objects to be treated as instances of their parent class while executing specific child-level logic.
Example: Both the Table and Reservation classes override the native toString() method from the Java Object class. When the system needs to log a reservation, it does not need to know the specific fields; it simply calls the method, and the object provides its own formatted description.
- Example Implementation: public String toString() { return Table + id + at + location; }

E. Separation of Concerns (SRP)
Each class in the system has a single, well-defined responsibility:
- Restaurant.java manages the seating rules and filtering.
- Database.java manages the file-based archiving logic.
- ApiServer.java manages the networking protocol and data delivery.
This ensures that modifying the save file format (JSON) does not require any changes to the seating-selection algorithm, making the codebase maintainable and scalable.

Internal HTTP Server and Networking Stack
 
The network layer is built upon the built-in com.sun.net.httpserver package, providing a light alternative to heavy enterprise frameworks.

1. The Built-in Server Engine (The Framework)
The ApiServer class acts as the gateway between the visual frontend and the Java engine. It serves static assets (HTML, CSS, JS) and handles several specialized API contexts. The server uses an unthreaded serial executor for maximum efficiency in single-user dashboard environments, preventing the overhead of complex thread management.

2. Custom JSON Parsing and Serialization
Without external libraries like Jackson or Gson, TableFlow implements its own JSON logic.
- Serialization: The system manually builds JSON strings using StringBuilder for high performance and low memory footprint.
- Parsing: The field() method in ApiServer uses intelligent string indexing to extract values from incoming POST bodies, ensuring the system remains completely self-contained.

3. CORS Security and Interaction
To allow for seamless development across different environments, the server implements full Cross-Origin Resource Sharing (CORS) support. It handles OPTIONS preflight requests and injects the necessary security headers to allow secure communication between the browser and the Java backend.

API Specification and Protocol Interactions
 
The frontend communicates with the server via several specialized RESTful endpoints:

- GET /api/tables: Retrieves the full inventory and status (FREE, PARTIAL, OCCUPIED) for all tables at a specific timestamp.
- POST /api/reservation: Handled by the Smart Seating algorithm, this endpoint decides the final assignment based on group size and zone.
- DELETE /api/reservation: Allows for the immediate cancellation of bookings and frees table capacity.
- GET /api/reservations: Returns the entire global list of bookings for auditing and client management.

Deployment and Execution Guide
 
TableFlow is optimized for rapid deployment using standard Java tools:

I. Compilation Process
Open a terminal in the root directory and execute:
   javac -d out/production/ExamenPOO src/model_rezervari/*.java

II. System Launch
Start the server and backend with:
   java -cp out/production/ExamenPOO model_rezervari.Main

The dashboard will be active at http://localhost:8080 and will attempt to open the default system browser automatically upon successful initialization.

TableFlow: A Zero-Dependency Java Engineering Masterpiece.
