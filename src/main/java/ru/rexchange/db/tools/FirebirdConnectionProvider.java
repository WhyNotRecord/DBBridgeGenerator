package ru.rexchange.db.tools;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class FirebirdConnectionProvider {
  protected static final Logger LOGGER = LogManager.getLogger(FirebirdConnectionProvider.class);
  public static final String DB_PASS_SYS_SETTING = "ru.rexchange.db.pass";
  private static final String DEF_DRIVER = "org.firebirdsql.jdbc.FBDriver";
  private static final String DEF_USER = "SYSDBA";
  private static final String DEF_PASS = "masterkey";

  public static Connection createConnection(String dbPath) throws ClassNotFoundException, SQLException {
    // Register JDBC driver
    Class.forName(DEF_DRIVER);

    // Set the params
    Properties props = new Properties();
    props.setProperty("user", DEF_USER);
    props.setProperty("password", System.getProperty(DB_PASS_SYS_SETTING, DEF_PASS));
    props.setProperty("charSet", "UTF-8");
    // Open a connection
    LOGGER.trace(String.format("Connecting to database %s...", dbPath));
    return DriverManager.getConnection(String.format("jdbc:firebirdsql:%s", dbPath), props);
  }

  public static Connection createConnection() throws ClassNotFoundException, SQLException {
    return createConnection("localhost:test_db");
  }

}
