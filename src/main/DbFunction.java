package main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DbFunction {
	public Connection connectToDB(String dbname, String user, String pass) throws ClassNotFoundException {
		Connection conn = null;
		try {
			Class.forName("org.postgresql.Driver");
			conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/" + dbname, user, pass);
			if (conn != null)
				System.out.println("Connection Established");
			else
				System.out.println("Connection Failed");

		} catch (SQLException ex) {
			while (ex != null) {
				System.out.println("SQL exception: " + ex.getMessage());
				ex = ex.getNextException();
			}
		}
		return conn;
	}

	public void createTable(Connection conn, String sqlTableStatement) throws SQLException {
		Statement statement;

		try {
			String query = sqlTableStatement;
			statement = conn.createStatement();
			statement.executeUpdate(query);
		} catch (SQLException ex) {
			while (ex != null) {
				System.out.println("SQL exception: " + ex.getMessage());
				ex = ex.getNextException();
			}
		}
	}

	public ResultSet getTable(Connection conn, String query, Object variable) throws SQLException {
	    PreparedStatement pst = conn.prepareStatement(query);

	    // Handle different types of variables
	    if (variable instanceof String) {
	        pst.setString(1, (String) variable);
	    } else if (variable instanceof Integer) {
	        pst.setInt(1, (Integer) variable);
	    } else if (variable instanceof Float) {
	        pst.setFloat(1, (Float) variable);
	    } else {
	        throw new SQLException("Unsupported variable type.");
	    }

	    return pst.executeQuery();
	}


	public boolean executeQuery(Connection conn, String sqlStatement, Object variable) throws SQLException {
	    PreparedStatement pst = null;
	    try {
	        // Create PreparedStatement
	        pst = conn.prepareStatement(sqlStatement);
	        
	        // Set parameter based on its type
	        if (variable instanceof String) {
	            pst.setString(1, (String) variable);
	        } else if (variable instanceof Integer) {
	            pst.setInt(1, (Integer) variable);
	        } else if (variable instanceof Float) {
	            pst.setFloat(1, (Float) variable);
	        } else {
	            throw new SQLException("Unsupported variable type.");
	        }

	        // Execute the update
	        pst.executeQuery();
	        return true;
	    } catch (SQLException ex) {
	        // Print SQL exceptions
	        while (ex != null) {
	            System.out.println("SQL exception: " + ex.getMessage());
	            ex = ex.getNextException();
	        }
	        return false;
	    } finally {
	        // Close PreparedStatement
	        if (pst != null) {
	            pst.close();
	        }
	    }
	}


}
