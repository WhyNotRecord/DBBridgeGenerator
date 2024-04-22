package ru.rexchange.db.bridge_gen.model;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

import com.sun.deploy.util.StringUtils;
import ru.rexchange.db.bridge_gen.container.TableInfoContainer;
import ru.rexchange.db.bridge_gen.container.TableInfoContainer.FieldInfo.DomainInfo;
import ru.rexchange.db.tools.DBUtils;

public abstract class FirebirdDatabaseInteractor extends AbstractDatabaseInteractor {

  private static final String Q_TABLE_EXISTANCE = "select count(rdb$relation_name) from rdb$relations rel where rdb$relation_name = '%s'";
  private static final String Q_FIELDS_LIST = "select rdb$field_name, rdb$field_source from rdb$relation_fields where rdb$relation_name = '%s'";
  private static final String Q_DOMAIN_EXISTANCE = "select count(rdb$field_name) from rdb$fields where rdb$field_name = '%s'";
  private static final String Q_CREATE_DOMAIN = "create domain %s as %s";
  private static final String Q_SEQUENCE_EXISTANCE = "select count(rdb$generator_name) from rdb$generators where rdb$generator_name = '%s'";
  private static final String Q_CREATE_SEQUENCE = "create sequence %s";
  private static final String Q_INSERT_FIELD = "alter table %s add %s %s";
  private static final String Q_DROP_FIELD = "alter table %s drop %s";
  private static final String Q_CONSTRAINT_SELECT =
      "SELECT RIS.RDB$INDEX_NAME FROM RDB$INDEX_SEGMENTS RIS\n" +
          "JOIN RDB$INDICES RI ON RI.RDB$INDEX_NAME = RIS.RDB$INDEX_NAME " +
          "AND RI.RDB$RELATION_NAME = ?\n" +
          "JOIN RDB$RELATION_CONSTRAINTS RRC ON RRC.RDB$CONSTRAINT_NAME = RIS.RDB$INDEX_NAME " +
          "AND RRC.RDB$CONSTRAINT_TYPE = 'PRIMARY KEY'\n" +
          "WHERE RIS.RDB$FIELD_NAME= ?";
  private static final String Q_CONSTRAINT_FIELDS_SELECT =
      "SELECT RIS2.RDB$FIELD_NAME, RIS.RDB$INDEX_NAME FROM RDB$INDEX_SEGMENTS RIS\n" +
          "JOIN RDB$INDICES RI ON RI.RDB$INDEX_NAME = RIS.RDB$INDEX_NAME " +
          "AND RI.RDB$RELATION_NAME = '%s'\n" +
          "JOIN RDB$RELATION_CONSTRAINTS RRC ON RRC.RDB$CONSTRAINT_NAME = RIS.RDB$INDEX_NAME " +
          "AND RRC.RDB$CONSTRAINT_TYPE = 'PRIMARY KEY'\n" +
          "RIGHT JOIN RDB$INDEX_SEGMENTS RIS2 ON RIS2.RDB$INDEX_NAME = RIS.RDB$INDEX_NAME\n" +
          "WHERE RIS.RDB$FIELD_NAME= '%s'";
  private static final String Q_DROP_CONSTRAINT = "alter table %s drop constraint %s";
  private static final String Q_CREATE_PRIMARY_KEY = "alter table %s add constraint %s primary key (%s)";
  private static final String Q_UPDATE_FIELD = "alter table %s alter column %s type %s";

  @Override
  public boolean tableExists(String name) throws SQLException, ClassNotFoundException {
    return DBUtils.getIntValue(getConnection(), String.format(Q_TABLE_EXISTANCE, name)) > 0;
  }

  @Override
  public Map<String, String> getTableFields(String tableName)
      throws SQLException, ClassNotFoundException {
    return DBUtils.getStringPairs(getConnection(), String.format(Q_FIELDS_LIST, tableName));
  }

  @Override
  public void createTable(TableInfoContainer data) throws SQLException, ClassNotFoundException {
    DBUtils.executeQuery(getConnection(), getNewTableQuery(data));
  }

  @Override
  public String getDomainName(String type, Integer size) {
    switch (type) {
    case TableInfoContainer.DataType.STRING:
      return String.format("D_VARCHAR_%s", size);
    case TableInfoContainer.DataType.INTEGER:
      return "D_INTEGER";// integer
    case TableInfoContainer.DataType.LONG:
      return "D_BIGINT";// bigint
    case TableInfoContainer.DataType.ID:
      return "D_ID";// bigint
    case TableInfoContainer.DataType.DOUBLE:
      return "D_DOUBLE";// double precision
    case TableInfoContainer.DataType.FLOAT:
      return "D_FLOAT";// float
    case TableInfoContainer.DataType.DATE:
      return "D_DATE";// date
    case TableInfoContainer.DataType.DATETIME:
      return "D_DATETIME";// timestamp
    }
    return null;
  }

  @Override
  public void insertField(String tableName, String name, String domainName)
      throws ClassNotFoundException, SQLException {
    DBUtils.executeQuery(getConnection(),
        String.format(Q_INSERT_FIELD, tableName, name, domainName));
  }

  @Override
  public void removeField(String tableName, String field)
      throws ClassNotFoundException, SQLException {
    DBUtils.executeQuery(getConnection(), String.format(Q_DROP_FIELD, tableName, field));
  }

  @Override
  public void updateField(String tableName, String field, String domainName)
      throws ClassNotFoundException, SQLException {
    DBUtils.executeQuery(getConnection(),
        String.format(Q_UPDATE_FIELD, tableName, field, domainName));
  }

  @Override
  public void initBaseDomains() throws SQLException, ClassNotFoundException {
    if (!domainExists("D_INTEGER")) {
      createDomain(new DomainInfo(TableInfoContainer.DataType.INTEGER));
    }
    if (!domainExists("D_BIGINT")) {
      createDomain(new DomainInfo(TableInfoContainer.DataType.LONG));
    }
    if (!domainExists("D_ID")) {
      createDomain(new DomainInfo(TableInfoContainer.DataType.ID));
    }
    if (!domainExists("D_DOUBLE")) {
      createDomain(new DomainInfo(TableInfoContainer.DataType.DOUBLE));
    }
    if (!domainExists("D_FLOAT")) {
      createDomain(new DomainInfo(TableInfoContainer.DataType.FLOAT));
    }
    if (!domainExists("D_DATE")) {
      createDomain(new DomainInfo(TableInfoContainer.DataType.DATE));
    }
    if (!domainExists("D_DATETIME")) {
      createDomain(new DomainInfo(TableInfoContainer.DataType.DATETIME));
    }

  }

  @Override
  public boolean domainExists(String name) throws SQLException, ClassNotFoundException {
    return DBUtils.getIntValue(getConnection(), String.format(Q_DOMAIN_EXISTANCE, name)) > 0;
  }

  @Override
  public void createDomain(DomainInfo domain) throws SQLException, ClassNotFoundException {
    DBUtils.executeQuery(getConnection(),
        String.format(Q_CREATE_DOMAIN, getDomainName(domain), getDBSpecificType(domain)));
  }

  @Override
  public boolean sequenceExists(String name) throws SQLException, ClassNotFoundException {
    return DBUtils.getIntValue(getConnection(), String.format(Q_SEQUENCE_EXISTANCE, name)) > 0;
  }

  @Override
  public void createSequence(String name) throws SQLException, ClassNotFoundException {
    DBUtils.executeQuery(getConnection(), String.format(Q_CREATE_SEQUENCE, name));
  }

  @Override
  public Map<String, String> fieldConstraints(String field, String tableName) throws SQLException, ClassNotFoundException {
    return DBUtils.getStringPairs(getConnection(), String.format(Q_CONSTRAINT_FIELDS_SELECT,  tableName, field));
    //return DBUtils.getStringList(getConnection(), Q_CONSTRAINT_SELECT,  tableName, field);
  }

  @Override
  public void dropConstraint(String constraint, String tableName) throws SQLException, ClassNotFoundException {
    DBUtils.executeQuery(getConnection(), String.format(Q_DROP_CONSTRAINT, tableName, constraint));
  }

  @Override
  public void createPrimaryKey(String tableName, String constraintName, Collection<String> fields)
      throws SQLException, ClassNotFoundException {
    DBUtils.executeQuery(getConnection(),
        String.format(Q_CREATE_PRIMARY_KEY, tableName, constraintName, StringUtils.join(fields, ", ")));

  }

  public String getSequenceName(String fieldName, String tableName) {
    String seqName = super.getSequenceName(fieldName, tableName);
    if (seqName.length() > 31)
      return seqName.substring(0, 31);
    return seqName;
  }

  @Override
  public String getDBSpecificType(DomainInfo domain) {
    switch (domain.getType()) {
    case TableInfoContainer.DataType.STRING:
      return String.format("varchar(%s)", domain.getSize());
    case TableInfoContainer.DataType.INTEGER:
      return "INTEGER";// integer
    case TableInfoContainer.DataType.LONG:
      return "BIGINT";// bigint
    case TableInfoContainer.DataType.ID:
      return "BIGINT";// bigint
    case TableInfoContainer.DataType.DOUBLE:
      return "DOUBLE PRECISION";// double precision
    case TableInfoContainer.DataType.FLOAT:
      return "FLOAT";// float
    case TableInfoContainer.DataType.DATE:
      return "DATE";// date
    case TableInfoContainer.DataType.DATETIME:
      return "TIMESTAMP";// timestamp
    }
    return null;
  }
}
