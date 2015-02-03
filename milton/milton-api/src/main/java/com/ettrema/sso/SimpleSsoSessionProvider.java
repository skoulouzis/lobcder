package com.ettrema.sso;

/**
 * Just for testing and debugging, this uses a constant string as the "session id"
 *
 * @author brad
 */
public class SimpleSsoSessionProvider implements SsoSessionProvider {

	private String prefix;
	
	public Object getUserTag(String firstComp) {
		if( firstComp != null && firstComp.equals(prefix) ) {
			return "aUser";
		} else {
			return null;
		}
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	
	
	
}
