# 🛒 Store Management System

A robust **Java-based Store Management System** developed as part of a Software Engineering curriculum. This application manages diverse product lines, integrates with a persistent **PostgreSQL** database, and implements advanced design patterns like the **Memento Pattern**.

---

## 🚀 Key Features
* **Multi-Channel Inventory:** Logic for handling `InStore`, `Website`, and `Wholesale` products.
* **Database Utility:** Custom-built `DbFunction` class for automated table creation and prepared statement execution.
* **Order Processing:** Interface-driven system (`Orderable.java`) to ensure consistent behavior across different order types.
* **Persistent Storage:** Full integration with PostgreSQL for high-reliability data management.

---

## 🛠️ Technical Stack
* **Language:** Java (JDK 17+)
* **Database:** PostgreSQL
* **IDE:** Eclipse / STM32CubeIDE
* **Driver:** JDBC (PostgreSQL Connector)

---

## 📁 Project Structure
* **`products`**: Contains the core logic for the abstract `Product` class and its specialized variants.
* **`orders`**: Handles the logic for `InStoreOrder`, `WebsiteOrder`, and `WholesalerOrder`.
* **`main`**: Home to `DbFunction.java`, managing the backend connection and SQL queries.
* **`enums`**: Stores constants for `eProductType` and `eShippingOption`.

---

## ⚙️ Setup & Installation

### 1. Database Configuration
1. Install **PostgreSQL** and **pgAdmin 4**.
2. Create a database named `store_db`.
3. Use the following credentials in your `connect_do_db` call:
   - **DB Name:** `store_db`
   - **User:** `postgres`
   - **Password:** *[Your assigned password]*

### 2. Eclipse Build Path
To resolve the `org.postgresql.Driver` connection, you must add the JDBC JAR:
1. Right-click the project -> **Build Path** -> **Configure Build Path**.
2. Go to **Libraries** -> **Classpath**.
3. Click **Add External JARs** and select the `postgresql-42.x.x.jar`.

### 3. Running the App
1. Initialize the `DbFunction` class.
2. Establish a connection using `connect_do_db`.
3. Execute `createTable` to set up your environment automatically.

---

## 📝 Code Snippet: DB Connection
```java
DbFunction db = new DbFunction();
Connection conn = db.connect_do_db("store_db", "postgres", "your_password");

if (conn != null) {
    System.out.println("System ready for operations.");
}
