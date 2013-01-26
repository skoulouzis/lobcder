package com.bradmcevoy.http.http11;

import com.bradmcevoy.http.HandlerHelper;
import junit.framework.TestCase;

import static org.easymock.classextension.EasyMock.*;

/**
 *
 * @author brad
 */
public class GetHandlerTest extends TestCase {
	
	GetHandler getHandler;
	Http11ResponseHandler responseHandler;
	HandlerHelper handlerHelper;
	
	public GetHandlerTest(String testName) {
		super(testName);
	}
	
	@Override
	protected void setUp() throws Exception {
		responseHandler = createMock(Http11ResponseHandler.class);
		handlerHelper = createMock(HandlerHelper.class);
		getHandler = new GetHandler(responseHandler, handlerHelper);
	}

	public void testProcess() throws Exception {
	}

	public void testProcessResource() throws Exception {
	}

	public void testProcessExistingResource() throws Exception {
	}


	public void testGetMethods() {
	}

	public void testIsCompatible() {
	}
}
