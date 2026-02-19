package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class Main {
	// Scanner-for the connection with the user
	public static Scanner scanner = new Scanner(System.in);
	final public static double DOLLAR = 4.0;

	public static void addProduct(DbFunction myStore, Connection conn) throws SQLException {
		boolean flagShip = false;
		int costPrice, sellingPrice, stock;
		double weight;
		String more = null;
		eProductType choosenType = returnProductType();

		System.out.println("Enter product name:");
		String productName = scanner.nextLine();
		System.out.println("Enter product ID:");
		String productId = scanner.nextLine();
		System.out.println("Enter product cost price:");
		costPrice = IsValidFunctions.validInt();
		System.out.println("Enter product selling price:");
		sellingPrice = IsValidFunctions.validInt();
		System.out.println("Enter product stock:");
		stock = IsValidFunctions.validInt();
		System.out.println("Enter product weight:");
		weight = IsValidFunctions.validDouble();

		// Start a transaction by disabling auto-commit
		conn.setAutoCommit(false);

		try {
			// Insert the product
			String addProduct = "INSERT INTO product (product_name, product_id, cost_price, selling_price, stock, weight, Product_Type) "
					+ "VALUES (?, ?, ?, ?, ?, ?, ?)";

			try (PreparedStatement pst = conn.prepareStatement(addProduct)) {
				pst.setString(1, productName);
				pst.setString(2, productId);
				pst.setDouble(3, costPrice);
				pst.setDouble(4, sellingPrice);
				pst.setInt(5, stock);
				pst.setDouble(6, weight);
				pst.setString(7, choosenType.toString());
				pst.executeUpdate();

				System.out.println("Product successfully added to the store.");
			} catch (Exception e) {
				System.out.println(e);
			}

			// If the product type is SOLD_THROUGH_WEBSITE, add shipping options
			if (eProductType.SOLD_THROUGH_WEBSITE == choosenType) {

				while (!flagShip) {
					int optionId = returnOptionId(myStore, conn);
					if (optionId == -1) {
						System.out.println("Option table is empty");
						conn.setAutoCommit(true);
						return; // Exit the method if the option table is empty
					}
					// Insert shipping options for the product
					String query = "INSERT INTO product_shipping_options (product_id, option_id) VALUES (?, ?)";
					try (PreparedStatement pst1 = conn.prepareStatement(query)) {
						pst1.setString(1, productId);
						pst1.setInt(2, optionId);
						pst1.executeUpdate();

						System.out.println("Do you want to add another shipping option? Y/y- yes or N/n- no");
						more = scanner.nextLine();
						if (more.equalsIgnoreCase("N")) {
							flagShip = true;
						}
					} catch (SQLException ex) {
						// Print SQL exceptions
						System.out.println("This option already exists in the product");

					}
				}

				System.out.println("Product and shipping options successfully added.");
			}

			// Commit the transaction if everything was successful
			conn.commit();

		} catch (SQLException ex) {
			// Rollback the transaction if any error occurs
			conn.rollback();
			System.out.println("Transaction rolled back due to error.");
			ex.printStackTrace();
		} finally {
			// Restore auto-commit mode
			conn.setAutoCommit(true);
		}
	}

	public static void removeProduct(DbFunction myStore, Connection conn) throws SQLException {
		String productId = null;
		boolean removed = false;
		boolean validProduct = false;

		if (!isTableEmpty(conn, "product")) {
			System.out.println("All the products in the store:");
			showAllProducts(conn);

			// Start transaction
			conn.setAutoCommit(false);

			try {
				while (!removed) {
					while (!validProduct) {
						System.out.println("\nPlease choose a product by its ID:");
						productId = scanner.nextLine();

						if (productExists(conn, productId)) {
							validProduct = true;
						} else {
							System.out.println("No product found with ID " + productId + ".");
						}
					}

					// Delete from product_shipping_options table
					String deleteShippingOptionsQuery = "DELETE FROM product_shipping_options WHERE Product_ID = ?";

					// Delete from Product table
					String deleteProductQuery = "DELETE FROM Product WHERE Product_ID = ?";

					// Execute the deletion within the transaction
					try (PreparedStatement deleteShippingStmt = conn.prepareStatement(deleteShippingOptionsQuery);
							PreparedStatement deleteProductStmt = conn.prepareStatement(deleteProductQuery)) {

						// Remove product from product_shipping_options table
						deleteShippingStmt.setString(1, productId);
						deleteShippingStmt.executeUpdate();

						// Remove product from Product table
						deleteProductStmt.setString(1, productId);
						int rowsAffected = deleteProductStmt.executeUpdate();

						if (rowsAffected > 0) {
							System.out.println("Product with ID " + productId + " was successfully deleted.");
							removed = true;
						} else {
							System.out.println("No product found with ID " + productId + ".");
						}
					}

					// Commit the transaction after successful execution
					conn.commit();
				}
			} catch (SQLException e) {
				// Rollback the transaction in case of an error
				conn.rollback();
				System.out.println("Transaction rolled back due to error.");
				e.printStackTrace();
			} finally {
				// Restore auto-commit mode
				conn.setAutoCommit(true);
			}
		} else {
			System.out.println("The store is empty.");
		}
	}

	public static void updateStock(DbFunction myStore, Connection conn) throws SQLException {
		String productId = null;
		int stockAmount;
		boolean validId = false;
		boolean update = false;
		if (!isTableEmpty(conn, "product")) {
			showAllProducts(conn);
			while (!update) {
				System.out.println("Enter a product ID which you want to update");
				productId = scanner.nextLine();
				validId = productExists(conn, productId);
				if (validId) {
					System.out.println("Enter new stock amount:");
					stockAmount = IsValidFunctions.isPositiveNumber();
					String query = "UPDATE Product SET stock = '" + stockAmount + "' WHERE Product_ID = '" + productId
							+ "'";

					try (Statement stmt = conn.createStatement()) {
						int rowsAffected = stmt.executeUpdate(query);
						if (rowsAffected > 0) {
							System.out.println("Product with ID " + productId + " was successfully updated.");
							update = true;
						} else
							System.out.println("No product found with ID " + productId + ".");

					} catch (Exception e) {
						System.out.println(e);
					}
				} else
					System.out.println("Invalid id");
			}
		} else
			System.out.println("There are no products in the store");
	}

	public static void addOrder(DbFunction myStore, Connection conn) throws SQLException {

		boolean getProduct = false;
		String choosenPId = null, customerName, customerMobile;
		int amount = 0;
		int productCount = 0;
		int customerId;
		int newAmount = 0;
		boolean validStock = false;
		boolean validProduct = false;
		String country = null;

		System.out.println("Please enter customer details:");
		System.out.println("ID:");
		customerId = IsValidFunctions.validInt();
		System.out.println("Name:");
		customerName = scanner.nextLine();
		System.out.println("Phone number:");
		customerMobile = scanner.nextLine();

		// Start a transaction by disabling auto-commit
		conn.setAutoCommit(false);

		try {
			// Insert the customer if they don't exist
			if (!customerExists(conn, customerId)) {
				String query = "INSERT INTO Customer (Customer_ID, customer_Name, customer_mobile) VALUES (?, ?, ?)";
				try (PreparedStatement pst = conn.prepareStatement(query)) {
					pst.setInt(1, customerId);
					pst.setString(2, customerName);
					pst.setString(3, customerMobile);
					pst.executeUpdate();
				}
			}

			eProductType productType = returnProductType();
			eOrderType orderType;

			// Determine the order type based on the product type
			if (productType == eProductType.SOLD_IN_STORE) {
				orderType = eOrderType.IN_STORE_ORDER;
			} else if (productType == eProductType.SOLD_TO_WHOLESALERS) {
				orderType = eOrderType.WHOLESALE_ORDER;
			} else {
				orderType = eOrderType.WEBSITE_ORDER;
			}

			System.out.println("List of products that are " + productType);

			productCount = printProductsByType(conn, productType);

			if (productCount != 0) {

				while (!getProduct) {
					while (!validProduct) {
						System.out.println("\nPlease choose a product by its ID:");
						choosenPId = scanner.nextLine();
						if (productExistsByType(conn, choosenPId, productType))
							validProduct = true;
						else
							System.out.println("No product found with ID " + choosenPId + ".");
					}

					int productStock = getProductStock(conn, choosenPId);
					if (productStock == 0) {
						System.out.println("Stock is empty. Restock this product " + choosenPId);
						conn.setAutoCommit(true);
						return;
					}

					showProduct(myStore, conn, choosenPId);
					while (!validStock) {
						System.out.println("Enter how many do you want:");
						amount = IsValidFunctions.isPositiveNumber();
						newAmount = productStock - amount;
						if (newAmount >= 0) {
							validStock = true;
							getProduct = true;
						} else {
							System.out.println("Amount exceeds available stock.");
						}
					}
				}

				// Add the order to the table
				String orderQuery = "INSERT INTO Orders (Product_ID, Customer_id, Order_type, product_amount) "
						+ "VALUES (?, ?, ?, ?)";

				try (PreparedStatement pst = conn.prepareStatement(orderQuery)) {
					pst.setString(1, choosenPId);
					pst.setInt(2, customerId);
					pst.setString(3, orderType.toString());
					pst.setInt(4, amount);
					pst.executeUpdate();
					// Calculate minimum cost (assuming this doesn't affect the database)
					if (orderType == eOrderType.WEBSITE_ORDER) {
						// Website orders require delivery details
						int optionId = returnProductOptionId(myStore, conn, choosenPId);
						boolean existFlag = false;
						while (!existFlag) {
							displayCountryTaxTable(myStore, conn);
							System.out.println("Enter country code:");
							country = scanner.nextLine();
							existFlag = countryExists(myStore, conn, country);
						}
						CalculateMinCost(myStore, conn, optionId, choosenPId, country);
					}
				}

				// Update the product stock
				String stockQuery = "UPDATE Product SET stock = ? WHERE Product_ID = ?";
				try (PreparedStatement pst = conn.prepareStatement(stockQuery)) {
					pst.setInt(1, newAmount);
					pst.setString(2, choosenPId);
					pst.executeUpdate();
				}

				// Get the last order ID
				int lastOrderID = getLastRowId(conn, "orders", "order_id");

				// Insert invoice records
				insertInvoice(conn, "CUSTOMER_INVOICE", lastOrderID, choosenPId);
				insertInvoice(conn, "ACCOUNTANT_INVOICE", lastOrderID, choosenPId);

				// If everything was successful, commit the transaction
				conn.commit();
				System.out.println("Order successfully added and transaction committed.");

			} else {
				System.out.println("No products available.");
			}

		} catch (SQLException ex) {
			// If any error occurs, roll back the transaction
			conn.rollback();
			System.out.println("Transaction rolled back due to error.");
			ex.printStackTrace();
		} finally {
			// Restore auto-commit mode
			conn.setAutoCommit(true);
		}
	}

	public static void undoOrder(Connection conn) throws SQLException {

		int lastOrderId = getLastRowId(conn, "orders", "order_id");
		String productId = null;
		int productAmount = 0;

		// Start a transaction
		conn.setAutoCommit(false); // Disable auto-commit

		try {
			if (lastOrderId > 0) {
				System.out.println("Last Order ID: " + lastOrderId);

				// Step 1: Delete the invoice attached to this order
				String deleteInvoiceSql = "DELETE FROM Invoice WHERE order_id = ?";
				try (PreparedStatement deleteInvoiceStmt = conn.prepareStatement(deleteInvoiceSql)) {
					deleteInvoiceStmt.setInt(1, lastOrderId);
					deleteInvoiceStmt.executeUpdate();
				}

				// Step 2: Extract product ID and amount from the last order
				String getOrderDetailsSql = "SELECT product_id, product_amount FROM Orders WHERE order_id = ?";
				try (PreparedStatement getOrderDetailsStmt = conn.prepareStatement(getOrderDetailsSql)) {
					getOrderDetailsStmt.setInt(1, lastOrderId);
					try (ResultSet rs = getOrderDetailsStmt.executeQuery()) {
						if (rs.next()) {
							productId = rs.getString("product_id");
							productAmount = rs.getInt("product_amount");
						}
					}
				}

				// Step 3: Remove the last order
				String deleteOrderSql = "DELETE FROM Orders WHERE order_id = ?";
				try (PreparedStatement deleteOrderStmt = conn.prepareStatement(deleteOrderSql)) {
					deleteOrderStmt.setInt(1, lastOrderId);
					deleteOrderStmt.executeUpdate();
				}

				// Step 4: Update the stock of the affected product
				System.out.println("Restoring stock by amount: " + productAmount);
				String updateStockSql = "UPDATE Product SET Stock = Stock + ? WHERE Product_ID = ?";
				try (PreparedStatement updateStockStmt = conn.prepareStatement(updateStockSql)) {
					updateStockStmt.setInt(1, productAmount);
					updateStockStmt.setString(2, productId);
					updateStockStmt.executeUpdate();
				}

				// If everything was successful, commit the transaction
				conn.commit();
				System.out.println("Last order has been undone and stock has been updated.");

			} else {
				System.out.println("There are no orders to undo.");
			}

		} catch (SQLException e) {
			// Rollback the transaction if any operation fails
			conn.rollback();
			System.out.println("Error occurred. Transaction rolled back.");
			e.printStackTrace();

		} finally {
			// Reset auto-commit to true after transaction is complete
			conn.setAutoCommit(true);
		}
	}

	public static void showSpecificProduct(DbFunction myStore, Connection conn) throws SQLException {
		System.out.println("Enter product ID");
		String productId = scanner.nextLine();
		if (productExists(conn, productId))
			showProduct(myStore, conn, productId);
		else
			System.out.println("Product ID dont exists");
	}

	public static void showProduct(DbFunction myStore, Connection conn, String productId) throws SQLException {
		String type = null;
		String query = "SELECT * FROM Product WHERE Product_ID = ?";
		ResultSet rs = myStore.getTable(conn, query, productId);
		if (rs.next()) {
			String name = rs.getString("Product_Name");
			type = rs.getString("Product_Type");
			float costPrice = rs.getFloat("Cost_Price");
			float sellingPrice = rs.getFloat("Selling_Price");
			int stock = rs.getInt("Stock");
			float weight = rs.getFloat("Weight");

			System.out.println("Product Details:");
			System.out.println("ID: " + productId);
			System.out.println("Name: " + name);
			System.out.println("Type: " + type);
			if (type.equals("SOLD_THROUGH_WEBSITE")) {
				System.out.println("Cost Price: $" + costPrice / DOLLAR);
				System.out.println("Selling Price: $" + sellingPrice / DOLLAR);
			} else {
				System.out.println("Cost Price: ₪" + costPrice / DOLLAR);
				System.out.println("Selling Price: ₪" + sellingPrice / DOLLAR);
			}
			System.out.println("Stock: " + stock);
			System.out.println("Weight: " + weight);
		}

	}

	public static void showAllProducts(Connection conn) {
		String query = "SELECT * FROM Product";

		try (PreparedStatement pst = conn.prepareStatement(query); ResultSet rs = pst.executeQuery()) {

			// Print the column headers
			System.out.printf("%-15s %-20s %-20s %-15s %-15s %-10s %-10s%n", "Product_ID", "Product_Name",
					"Product_Type", "Cost_Price", "Selling_Price", "Stock", "Weight");

			// Loop through the result set and print each row
			while (rs.next()) {
				String productId = rs.getString("Product_ID");
				String productName = rs.getString("Product_Name");
				String productType = rs.getString("Product_Type");
				float costPrice = rs.getFloat("Cost_Price");
				float sellingPrice = rs.getFloat("Selling_Price");
				int stock = rs.getInt("Stock");
				float weight = rs.getFloat("Weight");

				// Adjust formatting and currency based on product type
				if ("SOLD_THROUGH_WEBSITE".equals(productType)) {
					System.out.printf("%-15s %-20s %-20s $%-15.2f $%-15.2f %-10d kg %-10.2f%n", productId, productName,
							productType, costPrice / DOLLAR, sellingPrice / DOLLAR, stock, weight);
				} else {
					System.out.printf("%-15s %-20s %-20s ₪%-15.2f ₪%-15.2f %-10d kg %-10.2f%n", productId, productName,
							productType, costPrice, sellingPrice, stock, weight);
				}
			}
		} catch (SQLException e) {
			System.err.println("SQL Exception: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void printProductsOrder(Connection conn) throws SQLException {

		boolean validProduct = false;
		String productId = null;

		System.out.println("All the products in store by ID:");
		showAllProducts(conn); // Assumed to display products

		// Assume that the ID is valid by the Instructions
		while (!validProduct) {
			System.out.println("\nPlease choose a product by its ID:");
			productId = scanner.nextLine();
			if (productExists(conn, productId)) { // Assumed to check product existence
				validProduct = true;
			} else {
				System.out.println("No product found with ID " + productId + ".");
			}
		}

		String sql = "SELECT * FROM Orders WHERE Product_ID = ?";
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, productId);
			ResultSet rs = pstmt.executeQuery();

			if (rs.next()) {
				// Process the results
				do {
					int orderId = rs.getInt("order_id");
					int customerId = rs.getInt("customer_id");
					String orderType = rs.getString("order_type");
					int productAmount = rs.getInt("product_amount");

					System.out.println("Order ID: " + orderId);
					System.out.println("Customer ID: " + customerId);
					System.out.println("Order Type: " + orderType);
					System.out.println("Product Amount: " + productAmount);
					System.out.println("--------------------------------");
					printInvoiceByOrderId(conn, orderId);
				} while (rs.next()); // Continue until all rows are processed
			} else {
				System.out.println("No orders found for Product ID: " + productId);
			}
		} catch (SQLException e) {
			System.err.println("SQL Exception: " + e.getMessage());
		}
	}

	public static void printInvoiceByOrderId(Connection conn, int orderId) {

		String orderType = null;
		String typeSql = "SELECT order_type FROM Orders WHERE order_id = ?";
		try (PreparedStatement pstmt = conn.prepareStatement(typeSql)) {
			pstmt.setInt(1, orderId);
			ResultSet rs = pstmt.executeQuery();

			if (rs.next()) {
				orderType = rs.getString("order_type");
			} else {
				System.out.println("Order not found.");
				return; // Exit if the order is not found
			}
		} catch (SQLException e) {
			System.err.println("SQL Exception: " + e.getMessage());
			return; // Exit if an exception occurs
		}

		// Define the SQL query to get invoice details based on order_id
		String query = "SELECT invoice_id, invoice_type, total_tax, total_price, profit "
				+ "FROM Invoice WHERE order_id = ?";

		try (PreparedStatement pst = conn.prepareStatement(query)) {
			// Set the parameter for the query
			pst.setInt(1, orderId);

			try (ResultSet rs = pst.executeQuery()) {
				while (rs.next()) {
					// Retrieve invoice details
					int invoiceId = rs.getInt("invoice_id");
					String invoiceType = rs.getString("invoice_type");
					float totalTax = rs.getFloat("total_tax");
					float totalPrice = rs.getFloat("total_price");
					float profit = rs.getFloat("profit");

					// Print the invoice details
					System.out.println("Invoice ID: " + invoiceId);
					System.out.println("Invoice Type: " + invoiceType);
					if (orderType.equals("WEBSITE_ORDER"))
						System.out.println("Total Price: $" + totalPrice);
					else
						System.out.println("Total Price: ₪" + totalPrice);

					if ("ACCOUNTANT_INVOICE".equals(invoiceType)) {
						if (orderType.equals("WEBSITE_ORDER"))
							System.out.println("Profit: $" + profit);
						else
							System.out.println("Profit: ₪" + profit);

					} else {
						if (orderType.equals("WEBSITE_ORDER"))
							System.out.println("Total Tax: $" + totalTax);
						else
							System.out.println("Total Tax: ₪" + totalTax);

					}
					System.out.println("--------------------------------");
				}
			}
		} catch (SQLException e) {
			System.err.println("SQL Exception: " + e.getMessage());
		}
	}

	public static int returnOptionId(DbFunction myStore, Connection conn) {
		boolean flagShip = false;
		int userInput = 0;
		int rowCount = 0;
		while (!flagShip) {
			System.out.println("Available shipping options:");

			// Fetch and display available shipping options
			try {

				// Define the query
				String query = "SELECT * FROM shipping_options";

				// Create and execute PreparedStatement
				PreparedStatement pst = conn.prepareStatement(query);
				ResultSet rs = pst.executeQuery();

				// Process the ResultSet
				while (rs.next()) {
					int id = rs.getInt("option_id");
					String option = rs.getString("option_name");
					System.out.println("ID: " + id + ", Option: " + option);
					rowCount++;
				}
			} catch (SQLException ex) {
				// Handle SQL exceptions
				while (ex != null) {
					System.out.println("SQL exception: " + ex.getMessage());
					ex = ex.getNextException();
				}
			}
			if (rowCount != 0) {
				System.out.println("\nSelect a shipping type, enter a number:");
				userInput = IsValidFunctions.validInt();
				try {
					// Check if the user input matches a valid option_id
					String query1 = "SELECT * FROM shipping_options WHERE option_id = ?";
					ResultSet rs = myStore.getTable(conn, query1, userInput);
					if (rs.next()) // If result set is not empty
						flagShip = true;
					else
						System.out.println("Wrong option, Try again");

				} catch (SQLException e) {
					System.err.println("SQL Exception: " + e.getMessage());
				}
			} else {
				return -1;
			}
		}
		return userInput;
	}

	public static int returnProductOptionId(DbFunction myStore, Connection conn, String productId) {
		boolean flagShip = false;
		int userInput = 0;

		while (!flagShip) {
			System.out.println("Available shipping options for Product ID: " + productId);

			// Fetch and display available shipping options with their names
			String query = "SELECT p.product_id, s.option_id, s.option_name " + "FROM product_shipping_options p "
					+ "JOIN shipping_options s ON p.option_id = s.option_id " + "WHERE p.product_id = ?";

			try (PreparedStatement pst = conn.prepareStatement(query)) {
				pst.setString(1, productId);

				try (ResultSet rs = pst.executeQuery()) {
					while (rs.next()) {
						int option_id = rs.getInt("option_id");
						String option_name = rs.getString("option_name");
						System.out.println("Option ID: " + option_id + ", Option Name: " + option_name);

					}
				}
			} catch (SQLException ex) {
				while (ex != null) {
					System.out.println("SQL exception: " + ex.getMessage());
					ex = ex.getNextException();
				}
			}

			System.out.println("\nSelect a shipping type, enter a number:");
			userInput = IsValidFunctions.validInt();

			// Check if the user input matches a valid option_id for the specified product
			String checkQuery = "SELECT * FROM product_shipping_options WHERE product_id = ? AND option_id = ?";

			try (PreparedStatement pst = conn.prepareStatement(checkQuery)) {
				// Set the parameters for the query
				pst.setString(1, productId);
				pst.setInt(2, userInput);

				try (ResultSet rs = pst.executeQuery()) {
					if (rs.next())// If result set is not empty
						flagShip = true;
					else {
						System.out.println("Wrong option, Try again");
					}
				}
			} catch (SQLException e) {
				System.err.println("SQL Exception: " + e.getMessage());

			}

		}
		return userInput;
	}

	public static boolean countryExists(DbFunction myStore, Connection conn, String countryCode) {
		boolean exists = false;

		try {
			String query = "SELECT 1 FROM Country_tax WHERE Country_Code = ?";
			ResultSet rs = myStore.getTable(conn, query, countryCode);

			if (rs.next()) {
				exists = true;
			}
		} catch (SQLException e) {
			System.err.println("SQL Exception: " + e.getMessage());
		}

		return exists;
	}

	public static void displayCountryTaxTable(DbFunction myStore, Connection conn) {
		System.out.println("Country tax table:");

		String query = "SELECT * FROM Country_tax";

		try (PreparedStatement pst = conn.prepareStatement(query); ResultSet rs = pst.executeQuery()) {

			while (rs.next()) {
				String countryCode = rs.getString("Country_Code");
				String countryName = rs.getString("Country_Name");
				float taxRate = rs.getFloat("tax_rate");

				System.out.println(countryCode + ": " + countryName + " - Tax Rate: " + taxRate);
			}
		} catch (SQLException e) {
			System.err.println("SQL Exception: " + e.getMessage());
		}
	}

	public static boolean isTableEmpty(Connection conn, String tableName) {
		String query = "SELECT COUNT(*) AS rowcount FROM " + tableName;
		boolean isEmpty = false;

		try (PreparedStatement pst = conn.prepareStatement(query); ResultSet rs = pst.executeQuery()) {

			if (rs.next()) {
				int rowCount = rs.getInt("rowcount");
				isEmpty = (rowCount == 0);
			}
		} catch (SQLException e) {
			System.err.println("SQL Exception: " + e.getMessage());
			e.printStackTrace();
		}

		return isEmpty;
	}

	public static eProductType returnProductType() {
		boolean flag = false;
		int i, userInput;
		eProductType choosenType = null;
		while (!flag) {
			System.out.println("Available product types:");
			i = 1;
			for (eProductType type : eProductType.values()) {
				System.out.println(i + ")" + type);
				i++;
			}
			System.out.println("\nSelecte a product type, enter a number");
			userInput = IsValidFunctions.validInt();

			eProductType[] allTypes = eProductType.values();
			if (userInput >= 1 && userInput <= allTypes.length) {
				choosenType = allTypes[userInput - 1];
				flag = true;
			} else
				System.out.println("Invalid option");
		}
		return choosenType;
	}

	public static int getLastRowId(Connection conn, String tableName, String idColumnName) throws SQLException {
		String query = "SELECT " + idColumnName + " FROM " + tableName + " ORDER BY " + idColumnName + " DESC LIMIT 1";
		int lastId = -1;

		try (PreparedStatement pst = conn.prepareStatement(query); ResultSet rs = pst.executeQuery()) {

			if (rs.next()) {
				lastId = rs.getInt(idColumnName); // Retrieve the ID from the last row
			}
		} catch (SQLException e) {
			System.err.println("SQL Exception: " + e.getMessage());
			throw e; // Rethrow exception to signal error to the caller
		}

		return lastId;
	}

	public static void insertInvoice(Connection conn, String invoiceType, int orderId, String productId) {
		float costPrice = 0;
		float sellingPrice = 0;
		float totalProfit = 0;
		int amount = 0;
		String orderType = null;
		float totalTax = 0;
		float totalPrice = 0;
		final double TAX_RATE = 0.17;

		// Retrieve the amount from Orders table
		String amountSql = "SELECT product_amount,order_type FROM Orders WHERE order_id = ?";
		try (PreparedStatement pstmt = conn.prepareStatement(amountSql)) {
			pstmt.setInt(1, orderId);
			ResultSet rs = pstmt.executeQuery();

			if (rs.next()) {
				amount = rs.getInt("product_amount");
				orderType = rs.getString("order_type");
			} else {
				System.out.println("Order not found.");
				return; // Exit if the order is not found
			}
		} catch (SQLException e) {
			System.err.println("SQL Exception: " + e.getMessage());
			return; // Exit if an exception occurs
		}

		// Retrieve Cost_Price and Selling_Price from Product table
		String productSql = "SELECT Cost_Price, Selling_Price FROM Product WHERE Product_ID = ?";
		try (PreparedStatement pstmt = conn.prepareStatement(productSql)) {
			pstmt.setString(1, productId);
			ResultSet rs = pstmt.executeQuery();

			if (rs.next()) {
				if (orderType.equals("WEBSITE_ORDER")) {
					costPrice = (float) (rs.getFloat("Cost_Price") / DOLLAR);
					sellingPrice = (float) (rs.getFloat("Selling_Price") / DOLLAR);
				} else {
					costPrice = (float) (rs.getFloat("Cost_Price"));
					sellingPrice = (float) (rs.getFloat("Selling_Price"));
				}

			} else {
				System.out.println("Product not found.");
				return; // Exit if the product is not found
			}
		} catch (SQLException e) {
			System.err.println("SQL Exception: " + e.getMessage());
			return; // Exit if an exception occurs
		}

		// Calculate totalProfit, totalTax, and totalPrice
		totalProfit = (sellingPrice - costPrice) * amount;
		totalTax = (float) ((sellingPrice) * amount * TAX_RATE);
		totalPrice = (sellingPrice * amount) + totalTax;

		// Insert into Invoice table based on the invoice type
		String sqlInvoice = "INSERT INTO Invoice (invoice_type, order_id, total_tax, total_price, profit) VALUES (?, ?, ?, ?, ?)";
		try (PreparedStatement pstmt = conn.prepareStatement(sqlInvoice)) {
			pstmt.setString(1, invoiceType);
			pstmt.setInt(2, orderId);
			pstmt.setFloat(4, totalPrice);

			if (invoiceType.equals("CUSTOMER_INVOICE")) {
				pstmt.setFloat(3, totalTax); // Set total_tax
				pstmt.setNull(5, java.sql.Types.FLOAT); // Set profit as NULL
			} else if (invoiceType.equals("ACCOUNTANT_INVOICE")) {
				pstmt.setNull(3, java.sql.Types.FLOAT); // Set total_tax as NULL
				pstmt.setFloat(5, totalProfit); // Set profit
			} else {
				throw new IllegalArgumentException("Invalid invoice type.");
			}

			pstmt.executeUpdate();
			System.out.println(invoiceType + " generated successfully.");
		} catch (SQLException e) {
			System.err.println("SQL Exception: " + e.getMessage());
		} catch (IllegalArgumentException e) {
			System.err.println("Illegal Argument: " + e.getMessage());
		}
	}

	public static int printProductsByType(Connection conn, eProductType productType) {
		String query = "SELECT * FROM Product WHERE Product_Type = ?";
		int productCount = 0;

		try (PreparedStatement pst = conn.prepareStatement(query)) {
			// Set the parameter for the query
			pst.setString(1, productType.name());

			try (ResultSet rs = pst.executeQuery()) {
				// Print the column headers
				System.out.printf("%-15s %-20s %-20s %-15s %-15s %-10s %-10s%n", "Product_ID", "Product_Name",
						"Product_Type", "Cost_Price", "Selling_Price", "Stock", "Weight");

				// Loop through the result set and print each row
				while (rs.next()) {
					String productId = rs.getString("Product_ID");
					String productName = rs.getString("Product_Name");
					String productTypeString = rs.getString("Product_Type");
					float costPrice = rs.getFloat("Cost_Price");
					float sellingPrice = rs.getFloat("Selling_Price");
					int stock = rs.getInt("Stock");
					float weight = rs.getFloat("Weight");
					productCount++;
					if (productTypeString.equals("SOLD_THROUGH_WEBSITE"))
						System.out.printf("%-15s %-20s %-20s $%-15.2f $%-15.2f %-10d kg %-10.2f%n ", productId,
								productName, productTypeString, costPrice / DOLLAR, sellingPrice / DOLLAR, stock,
								weight);
					else
						System.out.printf("%-15s %-20s %-20s ₪%-15.2f ₪%-15.2f %-10d kg %-10.2f%n", productId,
								productName, productTypeString, costPrice, sellingPrice, stock, weight);
				}
			}
		} catch (SQLException e) {
			System.err.println("SQL Exception: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Exception: " + e.getMessage());
		}

		return productCount;
	}

	public static float CalculateMinCost(DbFunction myStore, Connection conn, int optionId, String productId,
			String destCountry) throws SQLException {
		float minPrice = Float.MAX_VALUE;
		float deliveryPrice = 0;
		float productPrice = 0;
		float weight = 0;
		String deliveryCompanyOption = null;
		String companyId = null;
		// Get weight from product
		String weightQuery = "SELECT Weight FROM Product WHERE Product_ID = ?";
		try (PreparedStatement pstmt = conn.prepareStatement(weightQuery)) {
			pstmt.setString(1, productId);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				weight = rs.getFloat("Weight");
			}
		}

		// Get selling price from product
		String priceQuery = "SELECT Selling_Price FROM Product WHERE Product_ID = ?";
		try (PreparedStatement pstmt = conn.prepareStatement(priceQuery)) {
			pstmt.setString(1, productId);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				productPrice = rs.getFloat("Selling_Price");
			}
		}

		// Get all shipping options
		String queryShipping = "SELECT * FROM delivery_company_shipping_options WHERE option_id = ?";
		try (PreparedStatement pstmt = conn.prepareStatement(queryShipping)) {
			pstmt.setInt(1, optionId);
			ResultSet rs = pstmt.executeQuery();

			while (rs.next()) {
				deliveryCompanyOption = rs.getString("delivery_company_option");
				companyId = rs.getString("company_id");
				int baseCost = rs.getInt("base_cost");
				Integer maxCost = (Integer) rs.getObject("max_cost"); // Handle null values
				Float percentageFee = (Float) rs.getObject("percentage_fee"); // Handle null values
				float costPerKg = rs.getFloat("cost_per_kg");

				if (optionId == 1) {
					if ("FX".equals(companyId)) {
						deliveryPrice = costPerKg * weight;
					} else if ("DH".equals(companyId)) {
						deliveryPrice = baseCost;
					}

					if (deliveryPrice < minPrice) {
						minPrice = deliveryPrice;
					}

				} else if (optionId == 2) {
					if ("FX".equals(companyId)) {
						deliveryPrice = costPerKg * weight;
					} else if ("DH".equals(companyId)) {
						deliveryPrice = (percentageFee * productPrice);
						if (maxCost != null && deliveryPrice > maxCost) {
							deliveryPrice = maxCost;
						}
					}
					if (deliveryPrice < minPrice) {
						minPrice = deliveryPrice;
					}
				}
			}
		}

		minPrice += minPrice * getTaxRate(conn, destCountry);
		//// Add delivery to table
		String deliveryQuery = "INSERT INTO delivery (order_id,destination_country, delivery_price, delivery_company_option_id) "
				+ "VALUES (?, ?, ?, ?)";

		int orderId = getLastRowId(conn, "Orders", "order_id");
		try (PreparedStatement pstmt = conn.prepareStatement(deliveryQuery)) {

			pstmt.setInt(1, orderId); // Properly enclosed order ID
			pstmt.setString(2, destCountry); // Properly enclosed country name
			pstmt.setFloat(3, deliveryPrice); // Delivery price
			pstmt.setString(4, deliveryCompanyOption); // Set the delivery company option ID
			pstmt.executeUpdate();
		}
		System.out.printf("The choosen delivery company is %s with total price of %f \n", companyId, minPrice);
		return minPrice;
	}

	public static float getTaxRate(Connection conn, String countryCode) {
		String query = "SELECT tax_rate FROM Country_tax WHERE Country_Code = ?";
		float taxRate = -1.0f; // Use -1.0 to indicate that the country was not found

		try (PreparedStatement pst = conn.prepareStatement(query)) {
			// Set the parameter for the query
			pst.setString(1, countryCode);

			try (ResultSet rs = pst.executeQuery()) {
				if (rs.next()) {
					// Retrieve the tax rate from the result set
					taxRate = rs.getFloat("tax_rate");
				} else {
					System.out.println("Country not found in the tax table.");
				}
			}
		} catch (SQLException e) {
			System.err.println("SQL Exception: " + e.getMessage());
		}

		return taxRate;
	}

	public static int getProductStock(Connection conn, String productId) throws SQLException {
		String query = "SELECT Stock FROM Product WHERE Product_ID = ?";
		int stock = -1; // Default value if product does not exist

		try (PreparedStatement pst = conn.prepareStatement(query)) {
			// Set the parameter for the query
			pst.setString(1, productId);

			try (ResultSet rs = pst.executeQuery()) {
				if (rs.next()) {
					stock = rs.getInt("Stock"); // Extract the stock value
				} else {
					System.out.println("No stock found for Product ID: " + productId); // Debug statement if no result
																						// is found
				}
			}
		} catch (SQLException e) {
			System.err.println("SQL Exception: " + e.getMessage());
			throw e; // Rethrow exception to signal error to the caller
		}

		return stock;
	}

	public static boolean customerExists(Connection conn, int customerId) throws SQLException {
		String query = "SELECT 1 FROM Customer WHERE Customer_ID = ?";
		boolean exists = false;

		try (PreparedStatement pst = conn.prepareStatement(query)) {
			// Set the parameter for the query
			pst.setInt(1, customerId);

			try (ResultSet rs = pst.executeQuery()) {
				exists = rs.next(); // If there is a result, the customer exists
			}
		} catch (SQLException e) {
			System.err.println("SQL Exception: " + e.getMessage());
			throw e; // Rethrow exception to signal error to the caller
		}

		return exists;
	}

	public static boolean productExists(Connection conn, String productId) throws SQLException {
		String query = "SELECT 1 FROM Product WHERE Product_ID = ?";
		boolean exists = false;

		try (PreparedStatement pst = conn.prepareStatement(query)) {
			// Set the parameter for the query
			pst.setString(1, productId);

			try (ResultSet rs = pst.executeQuery()) {
				exists = rs.next(); // If there is a result, the product exists
			}
		} catch (SQLException e) {
			System.err.println("SQL Exception: " + e.getMessage());
			throw e; // Rethrow exception to signal error to the caller
		}

		return exists;
	}

	public static boolean productExistsByType(Connection conn, String productId, eProductType type)
			throws SQLException {
		String query = "SELECT 1 FROM Product WHERE Product_ID = ? AND Product_Type = ?";
		boolean exists = false;

		try (PreparedStatement pst = conn.prepareStatement(query)) {
			// Set parameters for the query
			pst.setString(1, productId);
			pst.setString(2, type.name()); // Assuming eProductType has a name() method that returns a string
											// representation

			try (ResultSet rs = pst.executeQuery()) {
				exists = rs.next(); // If there is a result, the product exists with the specified type
			}
		} catch (SQLException e) {
			System.err.println("SQL Exception: " + e.getMessage());
			throw e; // Rethrow exception to signal error to the caller
		}

		return exists;
	}

	public static void performBackupTransaction(Connection conn) throws SQLException {
		Statement stmt = null;
		try {
			// Disable auto-commit to begin the transaction
			conn.setAutoCommit(false);

			// Create a statement to execute SQL queries
			stmt = conn.createStatement();

			// Commit the transaction after completing the operations
			conn.commit();
			System.out.println("Backup transaction committed successfully.");

		} catch (SQLException e) {
			// Handle errors during backup and roll back if needed
			if (conn != null) {
				try {
					conn.rollback(); // Rollback if an error occurs
					System.out.println("Backup transaction rolled back due to an error.");
				} catch (SQLException ex) {
					ex.printStackTrace();
				}
			}
			e.printStackTrace();
		} finally {
			// Restore auto-commit to its default state and close resources
			if (conn != null) {
				conn.setAutoCommit(true);
			}
			if (stmt != null)
				stmt.close();
		}
	}

	public static void backupDatabase(String dbName, String filePath) throws IOException {
		// Full path to pg_dump
		String pgDumpPath = "C:\\Program Files\\PostgreSQL\\16\\bin\\pg_dump";

		// Construct the command as a list of arguments
		ProcessBuilder processBuilder = new ProcessBuilder(pgDumpPath, "-U", "postgres", "-F", "c", "-b", "-v", "-f",
				filePath, dbName);

		// Set environment variable for PostgreSQL password
		processBuilder.environment().put("PGPASSWORD", "daniel123");
		processBuilder.redirectErrorStream(true); // Combine stdout and stderr

		Process process = processBuilder.start();

		// Read and print the output and error streams
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
			}
		}

		try {
			int exitCode = process.waitFor();
			if (exitCode == 0) {
				System.out.println("Backup completed successfully.");
			} else {
				System.err.println("Backup failed with exit code: " + exitCode);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			e.printStackTrace();
		}
	}

	public static void restoreDatabase(String dbName, String dumpFilePath, Connection conn)
			throws IOException, SQLException {
		// Truncate all tables in the database
		deleteAllTables(conn);

		// Full path to pg_restore
		String pgRestorePath = "C:\\Program Files\\PostgreSQL\\16\\bin\\pg_restore";

		// Construct the command as a list of arguments
		ProcessBuilder processBuilder = new ProcessBuilder(pgRestorePath, "-U", "postgres", "-d", dbName, "-v", // Verbose
																												// output
				dumpFilePath);

		// Set environment variable for PostgreSQL password
		processBuilder.environment().put("PGPASSWORD", "daniel123");
		processBuilder.redirectErrorStream(true); // Combine stdout and stderr

		Process process = processBuilder.start();

		// Read and print the output and error streams
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
			}
		}

		try {
			int exitCode = process.waitFor();
			if (exitCode == 0) {
				System.out.println("Restore completed successfully.");
			} else {
				System.err.println("Restore failed with exit code: " + exitCode);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			e.printStackTrace();
		}
	}

	private static void deleteAllTables(Connection conn) throws SQLException {

		try (Statement stmt = conn.createStatement()) {

			// Disable foreign key checks to avoid constraint violations during truncation
			stmt.execute("SET session_replication_role = 'replica';");

			// Truncate all tables in the public schema
			// stmt.execute("DO $$ DECLARE r RECORD; BEGIN FOR r IN (SELECT tablename FROM
			// pg_tables WHERE schemaname = 'public') LOOP EXECUTE 'TRUNCATE TABLE ' ||
			// quote_ident(r.tablename) || ' CASCADE'; END LOOP; END $$;");

			// Drop all tables in the public schema
			stmt.execute(
					"DO $$ DECLARE r RECORD; BEGIN FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public') LOOP EXECUTE 'DROP TABLE IF EXISTS ' || quote_ident(r.tablename) || ' CASCADE'; END LOOP; END $$;");

			// Re-enable foreign key checks
			stmt.execute("SET session_replication_role = 'origin';");

			System.out.println("All tables deleted successfully.");

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// Function to "restore" by rolling back (begin a transaction and roll it back)
	public static void performRestoreTransaction(Connection conn) throws SQLException {
		Statement stmt = null;
		try {
			// Disable auto-commit to begin the transaction
			conn.setAutoCommit(false);

			// Create a statement to execute SQL queries
			stmt = conn.createStatement();

			// Rollback the transaction instead of committing
			conn.rollback();
			System.out.println("Restore transaction rolled back successfully.");

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			// Restore auto-commit to its default state and close resources
			if (conn != null) {
				conn.setAutoCommit(true);
			}
			if (stmt != null)
				stmt.close();
		}
	}
	
	String dbName = "store_db";
	String user = "postgres";
	String pass = "your_password_here";
	
	public static void main(String[] args) throws SQLException, ClassNotFoundException, IOException {
		DbFunction db = new DbFunction();
		Connection conn = null;
		try {
			conn = db.connectToDB(dbName, user, pass);
			String option = "";
			while (!option.toUpperCase().equals("E")) {
				System.out.println("Hello");
				System.out.println("1-->Add product");
				System.out.println("2-->Remove product");
				System.out.println("3-->Update product stock");
				System.out.println("4-->Add order to product");
				System.out.println("5-->Undo orders");
				System.out.println("6-->Show specific product");
				System.out.println("7-->Show all products");
				System.out.println("8-->Print all orders of specific product");
				System.out.println("9-->System backUp");
				System.out.println("10-->System restore");
				System.out.println("E/e-->exit");
				option = scanner.nextLine();
				try {
					switch (option) {
					case "1":
						addProduct(db, conn);
						break;
					case "2":
						removeProduct(db, conn);
						break;
					case "3":
						updateStock(db, conn);
						break;
					case "4":
						addOrder(db, conn);
						break;
					case "5":
						undoOrder(conn);
						break;
					case "6":
						showSpecificProduct(db, conn);
						break;
					case "7":
						showAllProducts(conn);
						break;
					case "8":
						printProductsOrder(conn);
						break;
					case "9":

						backupDatabase("MyStore", ".\\BackupData\\mydb.sql");
						// performBackupTransaction(conn);

						break;
					case "10":
						restoreDatabase("MyStore", ".\\BackupData\\mydb.sql", conn);
						// performRestoreTransaction(conn);
						break;
					case "E":
					case "e":
						System.out.println("Bye Bye");
						break;
					default:
						System.out.println("Invalid option");
					}
				} catch (SQLException e) {
					if (!conn.getAutoCommit()) { // Check if autoCommit is disabled
						System.out.println("Error occurred: " + e.getMessage());
						conn.rollback(); // Rollback transaction on error
					} else {
						throw e; // Re-throw exception if autoCommit is enabled
					}
				}
				System.out.println("Enter to continue");
				scanner.nextLine();
			}
		} finally {
			if (conn != null) {
				conn.close(); // Ensure the connection is closed
			}
			scanner.close();
		}
	}

}
