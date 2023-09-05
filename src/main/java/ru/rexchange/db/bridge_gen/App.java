package ru.rexchange.db.bridge_gen;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import ru.rexchange.db.bridge_gen.model.AbstractDatabaseInteractor;
import ru.rexchange.db.bridge_gen.model.FirebirdDatabaseInteractor;
import ru.rexchange.db.tools.FirebirdConnectionProvider;
import ru.rexchange.tools.FileUtils;

/**
 * Hello world!
 *
 */
public class App {
  private static final Logger LOGGER = Logger.getLogger(App.class);

  public static void main(String[] args) {
    PropertyConfigurator.configure("log4j.properties");
    // BasicConfigurator.configure();
    LOGGER.info("Starting Database Bridge Generator...");
    String jsonDbConfigFile = "tables.json";
    String dbPath = "localhost:default_site_db";
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-in") && i < args.length - 1) {
        jsonDbConfigFile = args[++i];
      } else if (args[i].equals("-db") && i < args.length - 1) {
        dbPath = args[++i];
      } else if (args[i].equals("-pas") && i < args.length - 1) {
        System.setProperty(FirebirdConnectionProvider.DB_PASS_SYS_SETTING, args[++i]);
      } else {
        LOGGER.warn(String.format("Unknown argument %s", args[i]));
      }
    }
    final String finalDbPath = dbPath;
    AbstractDatabaseInteractor db = new FirebirdDatabaseInteractor() {
      @Override
      public Connection createConnection() throws ClassNotFoundException, SQLException {
        return FirebirdConnectionProvider.createConnection(finalDbPath);
      }

    };
    BridgeGenerator bg = new BridgeGenerator(db);
    LOGGER.info(String.format("File %s is chosen for processing", jsonDbConfigFile));
    try (InputStream is = new FileInputStream(jsonDbConfigFile)) {
      bg.processData(FileUtils.readToString(is, "UTF-8"));
    } catch (IOException e) {
      LOGGER.error(e);
      return;
    }

    /*try {
    	TestObject2 obj1 = new TestObject2(17L);
    	obj1.setSendDate(new Date(100500L * 100500));
    	obj1.save(db.getConnection());
    
    	TestObject1 obj2 = new TestObject1(12L);
    	obj2.load(db.getConnection());
    	obj2.getDescription();
    } catch (Exception e) {
    	LOGGER.error(e);
    }*/
  }
}
