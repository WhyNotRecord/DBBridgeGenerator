package ru.rexchange.db.bridge_gen.container;

import ru.rexchange.exception.SystemException;

import java.util.List;
import java.util.stream.Collectors;

public class TableInfoContainer {
  String name;
  String parent;
  boolean isTransient = false;
  List<FieldInfo> fields;

  public interface DataType {
    String STRING = "string";
    String INTEGER = "int";
    String BOOLEAN = "boolean";
    String LONG = "long";
    String ID = "id";
    String UUID = "uuid";
    String DOUBLE = "double";
    String FLOAT = "float";
    String DATE = "date";//todo избавиться от использования устаревшего класса
    String DATETIME = "datetime";//todo избавиться от использования устаревшего класса
  }

  public static class FieldInfo {
    String name;
    DomainInfo domain;
    boolean primary = false;
    boolean generated = true;

    public static class DomainInfo {
      String type;
      Integer size = null;

      public DomainInfo(String type, Integer size) {
        this.type = type;
        this.size = size;
      }

      public DomainInfo(String type) {
        this.type = type;
      }

      public String getType() {
        return type;
      }

      public String getJavaType() {
        return TableInfoContainer.getJavaType(type);
      }

      public void setType(String type) {
        this.type = type;
      }

      public Integer getSize() {
        return size;
      }

      public void setSize(Integer size) {
        this.size = size;
      }
    }

    public FieldInfo(String name, DomainInfo domain, boolean primary, boolean generated) {
      this.name = name;
      this.domain = domain;
      this.primary = primary;
      this.generated = generated;
    }

    public FieldInfo(String name, DomainInfo domain, boolean primary) {
      this.name = name;
      this.domain = domain;
      this.primary = primary;
      if (primary && DataType.ID.equals(domain.getType()))
        generated = true;
    }

    /*public FieldInfo(String name, String type, boolean primary) {
    	this.name = name;
    	this.domain = new DomainInfo(type, null);
    	this.primary = primary;
    }*/

    public FieldInfo(String name, DomainInfo domain) {
      this.name = name;
      this.domain = domain;
    }

    /*public FieldInfo(String name, String type) {
      this.name = name;
      this.domain = new DomainInfo(type, null);
    }*/

    public String getName() {
      return name.toUpperCase();
    }

    public void setName(String name) {
      this.name = name;
    }

    public DomainInfo getDomain() {
      return domain;
    }

    public void setDomain(DomainInfo domain) {
      this.domain = domain;
    }

    public boolean isPrimary() {
      return primary;
    }

    public void setPrimary(boolean primary) {
      this.primary = primary;
    }

    public boolean isGenerated() {
      return generated;
    }

    public void setGenerated(boolean generated) {
      this.generated = generated;
    }
  }

  public String getName() {
    return name.toUpperCase();
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<FieldInfo> getFields() {
    return fields;
  }

  public List<FieldInfo> getPrimaryFields() {
    return fields.stream().filter(FieldInfo::isPrimary).collect(Collectors.toList());
  }

  public void setFields(List<FieldInfo> fields) {
    this.fields = fields;
  }

  public boolean isTransient() {
    return isTransient;
  }

  public void setTransient(boolean aTransient) {
    isTransient = aTransient;
  }

  public String getParent() {
    return parent;
  }

  public void setParent(String parent) {
    this.parent = parent;
  }

  private static String getJavaType(String jsonType) {
    switch (jsonType) {
      case TableInfoContainer.DataType.STRING:
      case TableInfoContainer.DataType.UUID:
        return "String";
      case TableInfoContainer.DataType.INTEGER:
        return "Integer";// integer
      case TableInfoContainer.DataType.BOOLEAN:
        return "Boolean";// smallint
      case TableInfoContainer.DataType.LONG:
      case TableInfoContainer.DataType.ID:
        return "Long";// bigint
      case TableInfoContainer.DataType.DOUBLE:
        return "Double";// double precision
      case TableInfoContainer.DataType.FLOAT://он загружается как Double из базы
        return "Float";// float
      case TableInfoContainer.DataType.DATE:
        return "Date";// date
      case TableInfoContainer.DataType.DATETIME:
        return "Timestamp";// timestamp
      default:
        throw new SystemException("Unknown type - " + jsonType);
    }
  }
}