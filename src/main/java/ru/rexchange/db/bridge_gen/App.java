package ru.rexchange.db.bridge_gen;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import ru.rexchange.db.bridge_gen.model.AbstractDatabaseInteractor;
import ru.rexchange.db.bridge_gen.model.FirebirdDatabaseInteractor;
import ru.rexchange.db.tools.FirebirdConnectionProvider;
import ru.rexchange.tools.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Hello world!
 *
 */
public class App {
  protected static final Logger LOGGER = LogManager.getLogger(App.class);

  public static void configureLogging() {
    // import org.apache.logging.log4j.core.LoggerContext;
    LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
    File file = new File("log4j2.xml");

    // this will force a reconfiguration
    context.setConfigLocation(file.toURI());
  }

  //todo DAO pattern https://javatutor.net/articles/j2ee-pattern-data-access-object
  public static void main(String[] args) {
    configureLogging();

    LOGGER.info("Starting Database Bridge Generator...");
    String jsonDbConfigFile = "tables.json";
    String dbPath = "localhost:default_site_db";
    boolean dropFields = false;
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-in") && i < args.length - 1) {
        jsonDbConfigFile = args[++i];
      } else if (args[i].equals("-db") && i < args.length - 1) {
        dbPath = args[++i];
      } else if (args[i].equals("-drop") && i < args.length - 1) {
        dropFields = true;
      } else if (args[i].equals("-pas") && i < args.length - 1) {
        System.setProperty(FirebirdConnectionProvider.DB_PASS_SYS_SETTING, args[++i]);
      } else {
        LOGGER.warn(String.format("Unknown argument %s", args[i]));
        LOGGER.warn("Acceptable arguments: -in <db_metadata.json> -db <db_alias> -pas <db_password>");
      }
    }
    final String finalDbPath = dbPath;
    AbstractDatabaseInteractor db = new FirebirdDatabaseInteractor() {
      @Override
      public Connection createConnection() throws ClassNotFoundException, SQLException {
        return FirebirdConnectionProvider.createConnection(finalDbPath);
      }
    };
    BridgeGenerator bg = new BridgeGenerator(db, dropFields);
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
