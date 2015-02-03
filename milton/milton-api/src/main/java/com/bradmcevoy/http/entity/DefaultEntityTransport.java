package com.bradmcevoy.http.entity;

import com.bradmcevoy.http.Response;

/**
 * The normal, trivial, implementation of EntityTransport which simply
 * writes immediately and directly to the Response outputstream
 *
 * @author brad
 */
public class DefaultEntityTransport implements EntityTransport{

	public DefaultEntityTransport() {
	}

	
	
	@Override
	public void sendResponseEntity(Response response) throws Exception {
		response.getEntity().write(response, response.getOutputStream());
	}

	@Override
	public void closeResponse(Response response) {
		response.close(); 
	}

}
