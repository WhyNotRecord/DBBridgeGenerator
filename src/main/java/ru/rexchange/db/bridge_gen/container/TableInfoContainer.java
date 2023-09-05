package ru.rexchange.db.bridge_gen.container;

import java.util.List;
import java.util.stream.Collectors;

public class TableInfoContainer {
	String name;
	List<FieldInfo> fields;

	public static interface DataType {
		public static final String STRING = "string";
		public static final String INTEGER = "int";
		public static final String LONG = "long";
		public static final String ID = "id";
		public static final String DOUBLE = "double";
    public static final String FLOAT = "float";
		public static final String DATE = "date";
		public static final String DATETIME = "datetime";
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
		return fields.stream().filter((fi) -> fi.isPrimary()).collect(Collectors.toList());
	}

	public void setFields(List<FieldInfo> fields) {
		this.fields = fields;
	}

	private static String getJavaType(String jsonType) {
		switch (jsonType) {
		case TableInfoContainer.DataType.STRING:
			return "String";
		case TableInfoContainer.DataType.INTEGER:
			return "Integer";// integer
		case TableInfoContainer.DataType.LONG:
			return "Long";// bigint
		case TableInfoContainer.DataType.ID:
			return "Long";// bigint
		case TableInfoContainer.DataType.DOUBLE:
			return "Double";// double precision
    case TableInfoContainer.DataType.FLOAT:
      return "Float";// float
		case TableInfoContainer.DataType.DATE:
			return "Date";// date
		case TableInfoContainer.DataType.DATETIME:
			return "Timestamp";// timestamp
		}
		return null;
	}
}