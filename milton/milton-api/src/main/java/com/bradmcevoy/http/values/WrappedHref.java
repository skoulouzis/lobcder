package com.bradmcevoy.http.values;

/**
 *
 * @author alex
 */
public class WrappedHref {
  private String value;

  public WrappedHref(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
