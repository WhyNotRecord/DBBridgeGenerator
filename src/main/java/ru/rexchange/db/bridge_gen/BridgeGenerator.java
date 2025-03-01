package ru.rexchange.db.bridge_gen;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.rexchange.db.bridge_gen.container.TableInfoContainer;
import ru.rexchange.db.bridge_gen.container.TableInfoContainer.FieldInfo;
import ru.rexchange.db.bridge_gen.model.AbstractDatabaseInteractor;
import ru.rexchange.tools.StringUtils;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

public class BridgeGenerator {
  protected static final Logger LOGGER = LogManager.getLogger(BridgeGenerator.class);
  private static final String CONST_GEN_PACKAGE = "ru.rexchange.gen";
  private AbstractDatabaseInteractor db = null;
  private final boolean dropFields;

  public BridgeGenerator(AbstractDatabaseInteractor db, boolean dropFields) {
    this.db = db;
    this.dropFields = dropFields;
  }

  public boolean processData(String data) {
    Gson gs = new Gson();
    TableInfoContainer[] tables = gs.fromJson(data, TableInfoContainer[].class);
    Map<String, String> classes = new HashMap<>();
    try {
      db.initBaseDomains();
      for (TableInfoContainer table : tables) {
        if (!table.isTransient() && !table.isParent())
          processDatabaseObjects(table);
        classes.putAll(processCodeObjects(table));
      }

      for (Entry<String, String> e : classes.entrySet()) {
        File dir = new File("src/main/java/ru/rexchange/gen/");
        if (!dir.exists()) {
          dir.mkdirs();
        }
        try (OutputStream os =
                 Files.newOutputStream(Paths.get(
                     String.format("src/main/java/ru/rexchange/gen/%s.java", e.getKey())))) {
          os.write(e.getValue().getBytes(StandardCharsets.UTF_8));
          LOGGER.debug(String.format("%s class exported", e.getKey()));
        }
      }
    } catch (Exception e) {
      LOGGER.error("Error", e);
      return false;
    }
    return true;
  }

  private void processDatabaseObjects(TableInfoContainer tableInfo)
      throws SQLException, ClassNotFoundException {
    if (!db.tableExists(tableInfo.getName())) {
      for (FieldInfo fi : tableInfo.getFields()) {
        db.checkDomain(fi.getDomain());
        if (TableInfoContainer.DataType.ID.equals(fi.getDomain().getType()) && fi.isGenerated())
          db.checkSequence(fi.getName(), tableInfo.getName());
      }
      db.createTable(tableInfo);
    } else {
      updateFieldsData(tableInfo.getFields(), tableInfo.getName());
    }
  }

  private void updateFieldsData(List<FieldInfo> fields, String tableName)
      throws SQLException, ClassNotFoundException {
    Map<String, String> dbFields = db.getTableFields(tableName);
    List<String> removedFields = new ArrayList<>(dbFields.keySet());
    List<FieldInfo> updatedFields = new ArrayList<>();
    List<FieldInfo> newFields = new ArrayList<>();
    for (FieldInfo fi : fields) {
      String fieldName = fi.getName();
      if (removedFields.contains(fieldName)) {
        removedFields.remove(fieldName);

        if (!dbFields.get(fieldName).equals(db.getDomainName(fi.getDomain()))) {
          updatedFields.add(fi);
        }
      }
      if (!dbFields.containsKey(fieldName)) {
        newFields.add(fi);
      }
    }

    processNewFields(tableName, newFields);
    if (dropFields)
      processOldFields(tableName, removedFields);
    updateFields(tableName, updatedFields);
  }

  private void processNewFields(String tableName, List<FieldInfo> newFields)// todo не добавляет primary key
      throws SQLException, ClassNotFoundException {
    for (FieldInfo field : newFields) {
      db.checkDomain(field.getDomain());
      if (TableInfoContainer.DataType.ID.equals(field.getDomain().getType()) && field.isGenerated())
        db.checkSequence(field.getName(), tableName);
      db.insertField(tableName, field.getName(), db.getDomainName(field.getDomain()));
    }
  }

  private void processOldFields(String tableName, List<String> removedFields)
      throws SQLException, ClassNotFoundException {
    for (String field : removedFields) {
      Map<String, String> constraints = db.fieldConstraints(field, tableName);
      if (constraints == null || constraints.isEmpty()) {
        db.removeField(tableName, field);
        continue;
      }

      String constraintName = constraints.values().iterator().next();
      db.dropConstraint(constraintName, tableName);
      try {
        db.removeField(tableName, field);
      } finally {
        Set<String> fields = constraints.keySet();
        fields.remove(field);
        db.createPrimaryKey(tableName, constraintName, fields);
      }
    }
  }

  private void updateFields(String tableName, List<FieldInfo> updatedFields)
      throws SQLException, ClassNotFoundException {
    for (FieldInfo field : updatedFields) {
      db.checkDomain(field.getDomain());
      if (TableInfoContainer.DataType.ID.equals(field.getDomain().getType()) && field.isGenerated())
        db.checkSequence(field.getName(), tableName);
      Map<String, String> constraints = db.fieldConstraints(field.getName(), tableName);
      if (constraints == null || constraints.isEmpty()) {
        db.updateField(tableName, field.getName(), db.getDomainName(field.getDomain()));
        return;
      }

      String constraintName = constraints.values().iterator().next();
      db.dropConstraint(constraintName, tableName);
      try {
        db.updateField(tableName, field.getName(), db.getDomainName(field.getDomain()));
      } finally {
        db.createPrimaryKey(tableName, constraintName, constraints.keySet());
      }
    }
  }

  private Map<String, String> processCodeObjects(TableInfoContainer tableInfo) {
    Map<String, String> result = new HashMap<>();
    String className = StringUtils.toUpperCamelCase(tableInfo.getName());
    String parentClassName = StringUtils.toUpperCamelCase(tableInfo.getParent());
    StringBuilder sb = new StringBuilder(String.format("package %s;", CONST_GEN_PACKAGE));
    addImportBlock(sb);
    startClass(sb, className, parentClassName);

    addConsts(sb, tableInfo);
    if (!tableInfo.isTransient()) {
      addQueryDummies(sb, tableInfo);
      addServiceFields(sb);
    }
    processFields(sb, tableInfo.getFields(), tableInfo.getParent() != null);
    addConstructor(sb, tableInfo);
    addSecondConstructor(sb, tableInfo);
    addCreateAndLoad(sb, tableInfo);
    addGetTableNameFunction(sb);
    if (!tableInfo.isTransient()) {
      //TODO поля родительского объекта не сохраняются и не загружаются
      addLoadFunction(sb, tableInfo);
      addFillFunction(sb, tableInfo);
      addSaveFunction(sb, tableInfo);
      addInsertFunction(sb, tableInfo);
      addUpdateFunction(sb, tableInfo);
    }

    endClass(sb, className);
    result.put(className, sb.toString());
    return result;
  }

  private void addImportBlock(StringBuilder sb) {
    sb.append(String.format("%n"));
    sb.append(String.format("%nimport java.sql.Timestamp;"));
    sb.append(String.format("%nimport java.sql.Date;"));
    sb.append(String.format("%nimport java.sql.Connection;"));
    sb.append(String.format("%nimport java.sql.ResultSet;"));
    sb.append(String.format("%nimport java.sql.SQLException;"));
    sb.append(String.format("%nimport java.util.Map;"));
    sb.append(String.format("%nimport java.util.Objects;"));
    sb.append(String.format("%nimport javax.persistence.Entity;"));
    sb.append(String.format("%nimport ru.rexchange.db.tools.DBUtils;"));
    sb.append(String.format("%nimport ru.rexchange.exception.SystemException;"));
    sb.append(String.format("%nimport ru.rexchange.exception.UserException;"));
  }

  private void startClass(StringBuilder sb, String className, String parentClassName) {
    sb.append(String.format("%n%n"));
    sb.append(String.format("%n@Entity"));
    sb.append(String.format("%npublic class %s", className));
    if (parentClassName != null && !parentClassName.isEmpty()) {
      sb.append(" extends ").append(parentClassName);
    }
    sb.append(" {");
  }

  private void endClass(StringBuilder sb, String className) {
    sb.append(String.format("%n}"));
  }

  private void addConsts(StringBuilder sb, TableInfoContainer tableInfo) {
    sb.append(
        String.format("%n  public static final String TABLE_NAME = \"%s\";", tableInfo.getName()));
    for (FieldInfo f : tableInfo.getFields()) {
      sb.append(
          String.format("%n  public static final String FIELD_%1$s = \"%1$s\";", f.getName()));
    }
  }

  private void addServiceFields(StringBuilder sb) {
    sb.append(String.format("%n  private boolean isNew = true;"));
    sb.append(String.format("%n  public boolean isNew() { return isNew; }"));
  }

  private void addQueryDummies(StringBuilder sb, TableInfoContainer tableInfo) {
    sb.append(String.format("%n  private static final String QUERY_LOAD_OBJECT = "));
    sb.append(String.format("\"%s\";", generateSelectQuery(tableInfo)));
    sb.append(String.format("%n  private static final String QUERY_INSERT_OBJECT = "));
    sb.append(String.format("\"%s\";", generateInsertQuery(tableInfo)));
    sb.append(String.format("%n  private static final String QUERY_UPDATE_OBJECT = "));
    sb.append(String.format("\"%s\";", generateUpdateQuery(tableInfo)));

    for (FieldInfo field : tableInfo.getFields()) {
      if (TableInfoContainer.DataType.ID.equals(field.getDomain().getType()) && field.isGenerated()) {
        sb.append(String.format("%n  private static final String QUERY_%s_SEQ_VALUE = ",
            field.getName().toUpperCase()));
        sb.append(
            String.format("\"%s\";", generateSequenceQuery(field.getName(), tableInfo.getName())));
      }
    }
  }

  private String generateSelectQuery(TableInfoContainer tableInfo) {
    StringBuilder sb = new StringBuilder();
    sb.append("SELECT ");
    for (FieldInfo fi : tableInfo.getFields()) {
      sb.append(String.format("%s, ", fi.getName()));
    }
    sb.setLength(sb.length() - 2);
    sb.append(String.format(" FROM %s WHERE ", tableInfo.isParent() ? "%s" : tableInfo.getName()));
    for (FieldInfo fi : tableInfo.getPrimaryFields()) {
      sb.append(String.format("%s = ? AND ", fi.getName()));
    }
    sb.setLength(sb.length() - 5);
    return sb.toString();
  }

  private String generateInsertQuery(TableInfoContainer tableInfo) {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("INSERT INTO %s(", tableInfo.isParent() ? "%s" : tableInfo.getName()));
    for (FieldInfo fi : tableInfo.getFields()) {
      sb.append(String.format("%s, ", fi.getName()));
    }
    sb.setLength(sb.length() - 2);
    sb.append(") VALUES (");
    for (int i = 0; i < tableInfo.getFields().size(); i++) {
      sb.append("?, ");
    }
    sb.setLength(sb.length() - 2);
    sb.append(")");
    return sb.toString();
  }

  private String generateUpdateQuery(TableInfoContainer tableInfo) {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("UPDATE %s SET ", tableInfo.isParent() ? "%s" : tableInfo.getName()));
    for (FieldInfo fi : tableInfo.getFields()) {
      if (!fi.isPrimary())
        sb.append(String.format("%s = ?, ", fi.getName()));
    }
    sb.setLength(sb.length() - 2);
    sb.append(String.format(" WHERE "));
    for (FieldInfo fi : tableInfo.getPrimaryFields()) {
      sb.append(String.format("%s = ? AND ", fi.getName()));
    }
    sb.setLength(sb.length() - 5);
    return sb.toString();
  }

  private String generateSequenceQuery(String fieldName, String tableName) {
    return String.format("SELECT gen_id(%s, 1) FROM rdb$database",
        db.getSequenceName(fieldName, tableName));
  }

  private void processFields(StringBuilder sb, List<FieldInfo> fields, boolean hasParent) {
    for (FieldInfo fi : fields) {
      if (fi.isPrimary() && hasParent)
        continue;
      processField(sb, fi);
    }
  }

  private void processField(StringBuilder sb, FieldInfo field) {
    String fieldName = StringUtils.toLowerCamelCase(field.getName());
    String FieldName = StringUtils.toUpperCamelCase(field.getName());
    sb.append(String.format("%n%n  protected %s %s;", field.getDomain().getJavaType(), fieldName));
    if (field.isPrimary())
      sb.append("//primary key");
    sb.append(String.format("%n%n  public %s get%s() {%n    return this.%s;%n  }",
        field.getDomain().getJavaType(), FieldName, fieldName));
    sb.append(String.format("%n%n  public void set%s(%s value) {%n    this.%s = value;%n  }",
        FieldName, field.getDomain().getJavaType(), fieldName));
    // sb.append(String.format("%n"));
  }

  private void addConstructor(StringBuilder sb, TableInfoContainer tableInfo) {
    String className = StringUtils.toUpperCamelCase(tableInfo.getName());
    sb.append(String.format("%n%n  public %s(", className));
    sb.append(") {");
    sb.append(String.format("%n  }"));
  }

  private void addSecondConstructor(StringBuilder sb, TableInfoContainer tableInfo) {
    String className = StringUtils.toUpperCamelCase(tableInfo.getName());
    sb.append(String.format("%n%n  public %s(", className));
    List<String> pkFields = new ArrayList<>();
    for (FieldInfo fi : tableInfo.getPrimaryFields()) {
      String fieldName = StringUtils.toLowerCamelCase(fi.getName());
      sb.append(String.format("%s %s, ", fi.getDomain().getJavaType(), fieldName));
      pkFields.add(fieldName);
    }
    sb.setLength(sb.length() - 2);
    sb.append(") {");
    for (String pkField : pkFields) {
      sb.append(String.format("%n    this.%1$s = %1$s;", pkField));
    }
    sb.append(String.format("%n  }"));
  }

  private void addCreateAndLoad(StringBuilder sb, TableInfoContainer tableInfo) {
    String className = StringUtils.toUpperCamelCase(tableInfo.getName());
    sb.append(String.format("%n%n  public static %s createAndLoad(Connection conn, ", className));
    List<String> pkFields = new ArrayList<>();
    for (FieldInfo fi : tableInfo.getPrimaryFields()) {
    	String fieldName = StringUtils.toLowerCamelCase(fi.getName());
    	sb.append(String.format("%s %s, ", fi.getDomain().getJavaType(), fieldName));
    	pkFields.add(fieldName);
    }
    sb.setLength(sb.length() - 2);
    sb.append(") throws SQLException, UserException, SystemException {");
    sb.append(String.format("%n    %1$s instance = new %1$s();", className));
    for (String pkField : pkFields) {
    	sb.append(String.format("%n    instance.%1$s = %1$s;", pkField));
    }
    sb.append("\n    instance.load(conn);");
    sb.append("\n    return instance;");
    sb.append(String.format("%n  }"));
  }

  private void addGetTableNameFunction(StringBuilder sb) {
    sb.append(String.format("%n%n  public String getTableName() {%n    return TABLE_NAME;%n  }"));
  }

  private void addLoadFunction(StringBuilder sb, TableInfoContainer tableInfo) {
    String className = StringUtils.toUpperCamelCase(tableInfo.getName());
    sb.append(String.format(
        "%n%n  public void load(Connection conn) throws SQLException, UserException, SystemException {"));
    int pkCount = 0;
    for (FieldInfo fi : tableInfo.getPrimaryFields()) {
      String fieldName = StringUtils.toLowerCamelCase(fi.getName());
      sb.append(String.format("%n    if (%1$s == null)%n"
          + "      throw new SystemException(\"Primary key (%1$s) is null\");", fieldName));
      pkCount++;
    }
    if (tableInfo.isParent()) {
      sb.append(String.format(
          "%n    Map<String, Object> result = DBUtils.getQueryResult(conn, String.format(QUERY_LOAD_OBJECT, getTableName())"));
    } else {
      sb.append(String.format(
          "%n    Map<String, Object> result = DBUtils.getQueryResult(conn, QUERY_LOAD_OBJECT"));
    }
    for (FieldInfo fi : tableInfo.getPrimaryFields()) {
      String fieldName = StringUtils.toLowerCamelCase(fi.getName());
      sb.append(String.format(", %s", fieldName));
    }
    sb.append(");");
    sb.append(String.format("%n    if (result != null) {"));
    sb.append(String.format("%n      %s.fillFromResultSet(result, this);", className));
    sb.append(String.format("%n      isNew = false;"));
    sb.append(String.format("%n    } else {"));
    sb.append(String.format("%n      throw new UserException(\"Cannot find object ("));

    for (int i = 0; i < pkCount; i++) {
      sb.append("%s, ");
    }
    sb.setLength(sb.length() - 2);

    sb.append(")\"");
    for (FieldInfo fi : tableInfo.getPrimaryFields()) {
      String fieldName = StringUtils.toLowerCamelCase(fi.getName());
      sb.append(String.format(", this.%s", fieldName));
    }
    sb.append(");");
    sb.append(String.format("%n    }"));
    if (tableInfo.getParent() != null) {
      sb.append(String.format("%n    super.load(conn);"));
    }
    sb.append(String.format("%n  }"));
  }

  private void addFillFunction(StringBuilder sb, TableInfoContainer tableInfo) {
    String className = StringUtils.toUpperCamelCase(tableInfo.getName());
    sb.append(String.format(
        "%n%n  public static void fillFromResultSet(Map<String, Object> result, %s obj) {", className));
    for (FieldInfo fi : tableInfo.getFields()) {
      String fieldName = StringUtils.toLowerCamelCase(fi.getName());
      String javaType = fi.getDomain().getJavaType();
      if ("Float".equals(javaType)) {
        sb.append(String.format("%n    obj.%1$s = result.get(FIELD_%2$s) == null ? " +
            "null : ((Double) result.get(FIELD_%2$s)).floatValue();", fieldName, fi.getName()));
      } else if ("Boolean".equals(javaType)) {
        sb.append(String.format("%n    obj.%1$s = result.get(FIELD_%2$s) == null ? " +
            "null : Objects.equals(result.get(FIELD_%2$s), 1);", fieldName, fi.getName()));
      } else {
        sb.append(String.format("%n    obj.%s = (%s) result.get(FIELD_%s);", fieldName, javaType, fi.getName()));
      }
    }
    sb.append(String.format("%n  }"));
  }

  private void addSaveFunction(StringBuilder sb, TableInfoContainer tableInfo) {
    sb.append(String.format(
        "%n%n  public boolean save(Connection conn) throws SQLException {"));
    sb.append(String.format("%n    if (isNew) {"));
    if (tableInfo.getParent() != null) {
      sb.append(String.format("%n      return insert(conn) && super.update(conn);"));
    } else {
      sb.append(String.format("%n      return insert(conn);"));
    }
    sb.append(String.format("%n    } else {"));
    if (tableInfo.getParent() != null) {
      sb.append(String.format("%n      return update(conn) && super.update(conn);"));
    } else {
      sb.append(String.format("%n      return update(conn);"));
    }
    sb.append(String.format("%n    }"));
    sb.append(String.format("%n  }"));
  }

  private void addInsertFunction(StringBuilder sb, TableInfoContainer tableInfo) {
    if (!tableInfo.isParent()) {
      sb.append(String.format(
          "%n%n  public boolean insert(Connection conn) throws SQLException {"));
      for (FieldInfo fi : tableInfo.getFields()) {
        if (TableInfoContainer.DataType.ID.equals(fi.getDomain().getType()) && fi.isGenerated()) {
          sb.append(String.format("%n    this.%s = DBUtils.getLongValue(conn, QUERY_%s_SEQ_VALUE);",
              StringUtils.toLowerCamelCase(fi.getName()), fi.getName().toUpperCase()));
        }
      }
      if (tableInfo.isParent()) {
        sb.append(String.format(
            "%n    boolean result = DBUtils.executeQuery(conn, String.format(QUERY_INSERT_OBJECT, getTableName())"));
      } else {
        sb.append(String.format("%n    boolean result = DBUtils.executeQuery(conn, QUERY_INSERT_OBJECT"));
      }
      for (FieldInfo fi : tableInfo.getFields()) {
        String fieldName = StringUtils.toLowerCamelCase(fi.getName());
        sb.append(String.format(", %s", fieldName));
      }
      sb.append(");");
      sb.append(String.format("%n    isNew = false;"));
      sb.append(String.format("%n    return result;"));
      sb.append(String.format("%n  }"));
    } else {
      sb.append(String.format(
          "%n%n  public boolean insert(Connection conn) throws SQLException {"));
      sb.append(String.format("%n    throw new UnsupportedOperationException(\"Parent object cannot be inserted\");"));
      sb.append(String.format("%n  }"));
    }
  }

  private void addUpdateFunction(StringBuilder sb, TableInfoContainer tableInfo) {
    sb.append(String.format("%n%n  public boolean update(Connection conn) throws SQLException {"));
    if (tableInfo.isParent()) {
      sb.append(String.format(
          "%n    return DBUtils.executeQuery(conn, String.format(QUERY_UPDATE_OBJECT, getTableName())"));
    } else {
      sb.append(String.format("%n    return DBUtils.executeQuery(conn, QUERY_UPDATE_OBJECT"));
    }
    List<FieldInfo> pkFields = new ArrayList<>();
    for (FieldInfo fi : tableInfo.getFields()) {
      if (fi.isPrimary()) {
        pkFields.add(fi);
        continue;
      }
      String fieldName = StringUtils.toLowerCamelCase(fi.getName());
      sb.append(String.format(", %s", fieldName));
    }
    for (FieldInfo fi : pkFields) {
      String fieldName = StringUtils.toLowerCamelCase(fi.getName());
      sb.append(String.format(", %s", fieldName));
    }
    sb.append(");");
    sb.append(String.format("%n  }"));
  }

}
