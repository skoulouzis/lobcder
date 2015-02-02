package com.bradmcevoy.http.values;

/**
 * This class exists to convey type information even when a value is null.
 *
 * This is important because we will often want to select parses and formatters
 * based on knowledge of the type of the value, even when that value is null.
 * 
 *
 * @author brad
 */
public class ValueAndType {

	private final Object value;
	private final Class type;

	public ValueAndType(Object value, Class type) {
		if (type == null) {
			throw new IllegalArgumentException("type may not be null");
		}
		if (value != null) {
			if (value.getClass() != type) {
				throw new RuntimeException("Inconsistent type information: " + value + " != " + type);
			}
		}
		this.value = value;
		this.type = type;
	}

	public Class getType() {
		return type;
	}

	public Object getValue() {
		return value;
	}
}
