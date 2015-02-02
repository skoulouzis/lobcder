package com.bradmcevoy.common;

/**
 *
 * @author alex
 */
public class NameSpace {

    private String name;
    private String prefix;

    public NameSpace(String name, String prefix) {
        this.name = name;
        this.prefix = prefix;
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }
}
