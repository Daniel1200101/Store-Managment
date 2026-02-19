# 🛒 Store Management System

A robust **Java-based Store Management System** developed as part of a Software Engineering curriculum. This application manages diverse product lines, integrates with a persistent **PostgreSQL** database, and implements advanced design patterns like the **Memento Pattern**.

---

## 🖥️ User Interface & Menu
Below is a preview of the main application interface used to navigate between inventory management and order processing.

<img src="https://github.com/user-attachments/assets/0c37bda6-c01b-4233-a830-8e4ac94475a1" width="343" height="209" alt="Application Menu">

---

## 🚀 Key Features
* **Multi-Channel Inventory:** Logic for handling `InStore`, `Website`, and `Wholesale` products.
* **Database Utility:** Custom-built `DbFunction` class for automated table creation and prepared statement execution.
* **Order Processing:** Interface-driven system (`Orderable.java`) to ensure consistent behavior across different order types.
* **Security:** Implements `PreparedStatement` to prevent SQL injection during database queries.

---

## 🛠️ Technical Stack
* **Language:** Java (JDK 17+)
* **Database:** PostgreSQL
* **IDE:** Eclipse / STM32CubeIDE
* **Driver:** JDBC (PostgreSQL Connector)

---

## 📊 Database Architecture
The system uses a relational schema managed via pgAdmin 4. Below is the current table structure and sample data.

<img src="https://github.com/user-attachments/assets/faad7329-6976-43ff-af42-ba6d01e2a4e5" width="343" height="209" alt="Product table">
<img src="https://github.com/user-attachments/assets/48687db8-a36c-446d-ac91-c72d629aa6fc" width="343" height="209" alt="customer table">
<img src="https://github.com/user-attachments/assets/af65cd8a-58c3-4d3c-94d7-3508cf0a2c29" width="343" height="209" alt="invoice table">



---

## 📁 Project Structure
* **`products`**: Core logic for the abstract `Product` class and its specialized variants.
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

---

## 👤 Author
**Software Engineering Student** *Final Project - Store Management Module*
