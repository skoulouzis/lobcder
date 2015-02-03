package com.bradmcevoy.http.http11.auth;

import junit.framework.TestCase;

/**
 *
 * @author bradm
 */
public class LdapSecurityManagerTest extends TestCase {
	
	LdapSecurityManager securityManager;
	
	public LdapSecurityManagerTest(String testName) {
		super(testName);
	}
	
	@Override
	protected void setUp() throws Exception {
		securityManager = new LdapSecurityManager();
	}



	public void testAuthoriseBasic() {
		System.out.println("testAuthoriseBasic ---------------------");
		Object result = securityManager.authenticate("brad", "xxxxx");
		System.out.println(result);
		System.out.println("testAuthoriseBasic - ooooooooooooooooooooooooooooo");
	}

	public void testAuthoriseDigest() {
	}
	
}
