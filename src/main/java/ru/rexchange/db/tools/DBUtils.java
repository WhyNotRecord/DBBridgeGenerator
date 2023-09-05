package ru.rexchange.db.tools;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import ru.rexchange.exception.SystemException;
import ru.rexchange.tools.DateUtils;

public class DBUtils {
	protected static final Logger LOGGER = Logger.getLogger(DBUtils.class);

	public static Integer getIntValue(Connection connection, String sql)
			throws SQLException {
		Statement stmt = null;
		try {
			// Execute a query
			synchronized (connection) {
				System.out.println("Creating statement...");
				stmt = connection.createStatement();
				LOGGER.trace("Executing query:\n" + sql);
				ResultSet rs = stmt.executeQuery(sql);
				if (rs.next()) {
					return rs.getInt(1);
				}
			}
		} finally {
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException e) {
				LOGGER.warn("Error occured while closing prepared statement", e);
			}
		}
		return -1;
	}

	public static Long getLongValue(Connection connection, String sql) throws SQLException {
		Statement stmt = null;
		try {
			// Execute a query
			synchronized (connection) {
        //System.out.println("Creating statement...");
				stmt = connection.createStatement();
				LOGGER.trace("Executing query:\n" + sql);
				ResultSet rs = stmt.executeQuery(sql);
				if (rs.next()) {
					return rs.getLong(1);
				}
			}
		} finally {
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException e) {
				LOGGER.warn("Error occured while closing prepared statement", e);
			}
		}
		return null;
	}

	public static boolean hasRecords(Connection connection, String sql)
			throws SQLException {
		Statement stmt = null;
		try {
			// Execute a query
			synchronized (connection) {
        //System.out.println("Creating statement...");
				stmt = connection.createStatement();
				LOGGER.trace("Executing query:\n" + sql);
				ResultSet rs = stmt.executeQuery(sql);
				// Extract data from result set
				return rs.next();
			}
		} finally {
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException e) {
				LOGGER.warn("Error occured while closing prepared statement", e);
			}
		}
	}

	public static boolean executeQuery(Connection connection, String sql)
			throws SQLException {
		Statement stmt = null;
		try {
			// Execute a query
			synchronized (connection) {
        //System.out.println("Creating statement...");
				stmt = connection.createStatement();
				LOGGER.trace("Executing query:\n" + sql);
				return stmt.execute(sql);
			}
		} finally {
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException e) {
				LOGGER.warn("Error occured while closing prepared statement", e);
			}
		}
	}

	public static boolean executeQuery(Connection connection, String sql, Object... params)
			throws SQLException {
		PreparedStatement stmt = null;
		try {
			// Execute a query
			synchronized (connection) {
        //System.out.println("Creating statement...");
				stmt = connection.prepareStatement(sql);
				for (int i = 0; i < params.length; i++) {
					setParameter(stmt, params[i], i + 1);
				}
				LOGGER.trace("Executing query:\n" + sql);
				return stmt.execute();
			}
		} finally {
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException e) {
				LOGGER.warn("Error occured while closing prepared statement", e);
			}
		}
	}

	public static Map<String, String> getStringPairs(Connection connection, String sql)
      throws SQLException {
		Statement stmt = null;
		try {
			// Execute a query
			Map<String, String> result = new HashMap<>();
			synchronized (connection) {
        //System.out.println("Creating statement...");
				stmt = connection.createStatement();
				LOGGER.trace("Executing query:\n" + sql);
				ResultSet rs = stmt.executeQuery(sql);
				// Extract data from result set
				while (rs.next()) {
					result.put(rs.getString(1).trim(), rs.getString(2).trim());
				}
			}
			return result;
		} finally {
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException e) {
				LOGGER.warn("Error occured while closing prepared statement", e);
			}
		}
	}

	public static Map<String, Object> getQueryResult(Connection connection, String sql,
			Object... params)
      throws SQLException {
		PreparedStatement stmt = null;
		try {
			// Execute a query
			synchronized (connection) {
        //System.out.println("Creating statement...");
				stmt = connection.prepareStatement(sql);
				for (int i = 0; i < params.length; i++) {
					setParameter(stmt, params[i], i + 1);
				}
				LOGGER.trace("Executing query:\n" + sql);
				ResultSet rs = stmt.executeQuery();
				if (rs.next()) {
					Map<String, Object> result = new HashMap<>();
					ResultSetMetaData meta = rs.getMetaData();
					int colCount = meta.getColumnCount();
					for (int i = 0; i < colCount; i++) {
						String colName = meta.getColumnName(i + 1);
						result.put(colName, rs.getObject(colName));
					}
					return result;
				}
				return null;
			}
		} finally {
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException e) {
				LOGGER.warn("Error occured while closing prepared statement", e);
			}
		}
	}

  /*public static String quotateVariable(Object var) {
  	return quotateVariable(var, true);
  }
  
  public static String quotateVariable(Object var, boolean escape) {
  	if (var == null)
  		return "";
  	if (var instanceof String) {
  		// todo if (escape) исключить возможность инъекции
  		return String.format("'%s'", var);
  	}
  	if (var instanceof Timestamp) {
  		return String.format("cast('%s' as TIMESTAMP)",
  				DateUtils.formatDateTime((Date) var, "yyyy-MM-dd HH:mm:ss.SSS"));
  	}
  	if (var instanceof Date) {
  		return String.format("'%s'", DateUtils.formatDateTime((Date) var, "yyyy-MM-dd"));
  	}
  
  	return var.toString();
  }*/

	protected static void setParameter(PreparedStatement stmt, Object param, int number)
			throws SQLException {
		if (param == null) {
			stmt.setNull(number, Types.VARCHAR);
		} else if (param instanceof String) {
			stmt.setString(number, (String) param);
		} else if (param instanceof Long) {
			stmt.setLong(number, (Long) param);
		} else if (param instanceof Integer) {
			stmt.setInt(number, (Integer) param);
		} else if (param instanceof Double) {
			stmt.setDouble(number, (Double) param);
		} else if (param instanceof Float) {
			stmt.setFloat(number, (Float) param);
		} else if (param instanceof Timestamp) {
			stmt.setTimestamp(number, (Timestamp) param);
		} else if (param instanceof Date) {
			stmt.setDate(number, (Date) param);
		} else
			throw new SystemException("Unsupported variable type %s", param.getClass());
	}
}
