package ru.rexchange.db.bridge_gen.model;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ru.rexchange.db.bridge_gen.container.TableInfoContainer;
import ru.rexchange.db.bridge_gen.container.TableInfoContainer.FieldInfo;
import ru.rexchange.db.bridge_gen.container.TableInfoContainer.FieldInfo.DomainInfo;

public abstract class AbstractDatabaseInteractor {
  private static Connection connection = null;
  protected static final Logger LOGGER = Logger.getLogger(AbstractDatabaseInteractor.class);
	/*private static final String PROP_USER = "db.user";
	private static final String PROP_PASS = "db.pass";
	private static final String PROP_DRIVER = "db.jdbc.driver";
	private static final String PROP_URL = "db.url";*/


  public Connection getConnection() throws ClassNotFoundException, SQLException {
    if (connection == null) {
      connection = createConnection();
    }
    return connection;

  }

  public abstract Connection createConnection() throws ClassNotFoundException, SQLException;

  // TODO: close connection
	public abstract boolean tableExists(String name) throws SQLException, ClassNotFoundException;

	public abstract boolean domainExists(String name) throws SQLException, ClassNotFoundException;

	public abstract boolean sequenceExists(String name) throws SQLException, ClassNotFoundException;

	public abstract Map<String, String> getTableFields(String tableName)
			throws SQLException, ClassNotFoundException;

	public abstract void createTable(TableInfoContainer data)
			throws SQLException, ClassNotFoundException;

	public abstract void createDomain(DomainInfo domain)
			throws SQLException, ClassNotFoundException;

	public abstract void createSequence(String name) throws SQLException, ClassNotFoundException;

	public abstract void initBaseDomains() throws SQLException, ClassNotFoundException;

	public abstract String getDomainName(String type, Integer size);

	public String getDomainName(DomainInfo domain) {
		return getDomainName(domain.getType(), domain.getSize());
	}

	public String getSequenceName(String fieldName, String tableName) {
		return String.format("SEQ_%s_%s", tableName.toUpperCase(), fieldName.toUpperCase());
	}

	public abstract String getDBSpecificType(DomainInfo domain);

	public void checkDomain(DomainInfo domain) throws ClassNotFoundException, SQLException {
		if (!domainExists(getDomainName(domain))) {
			createDomain(domain);
		}
	}

	public void checkSequence(String fieldName, String tableName)
			throws ClassNotFoundException, SQLException {
		String seqName = getSequenceName(fieldName, tableName);
		if (!sequenceExists(seqName)) {
			createSequence(seqName);
		}
	}

	public abstract void insertField(String tableName, String name, String domainName)
			throws SQLException, ClassNotFoundException;

	public abstract void removeField(String tableName, String field)
			throws SQLException, ClassNotFoundException;

	public abstract void updateField(String tableName, String name, String domainName)
			throws SQLException, ClassNotFoundException;

	protected String getNewTableQuery(TableInfoContainer data) {
		StringBuilder sb = new StringBuilder("CREATE TABLE");
		sb.append(' ').append(data.getName());
		sb.append(" (");
		List<String> pkFields = new ArrayList<>();
		for (FieldInfo fi : data.getFields()) {
			sb.append(fi.getName()).append(' ').append(getDomainName(fi.getDomain()));
			if (fi.isPrimary()) {
				pkFields.add(fi.getName());
			}
			sb.append(", ");
		}
		if (!pkFields.isEmpty()) {
			sb.append("primary key(");
			for (String pkField : pkFields) {
				sb.append(String.format("%s, ", pkField));
			}
			sb.setLength(sb.length() - 2);
			sb.append(")");
		} else {
			sb.setLength(sb.length() - 2);
		}
		sb.append(')');

		return sb.toString();
	}
}
