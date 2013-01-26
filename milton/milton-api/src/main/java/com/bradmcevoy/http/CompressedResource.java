package com.bradmcevoy.http;

import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.exceptions.NotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * 
 * 
 * This is just initial experimental design for solution to already compressed resources problem
 *
 * @author brad
 */
public interface CompressedResource {
	
	/**
	 * Return the supported encoding from the list of allowable encodings from the user agent
	 * 
	 * If none are supported return null.
	 * 
	 * @param acceptableEncodings
	 * @return - null if none of the given encodings are supported, otherwise the content encoding header value to be used
	 */
	String getSupportedEncoding(String acceptableEncodings);
	
	/**
	 * 
	 * @param contentEncoding - the supported encoding returned from getSupportedEncoding
	 * @param out
	 * @param range
	 * @param params
	 * @param contentType
	 * @throws IOException
	 * @throws NotAuthorizedException
	 * @throws BadRequestException
	 * @throws NotFoundException 
	 */
	void sendCompressedContent( String contentEncoding, OutputStream out, Range range, Map<String,String> params, String contentType ) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException;
	
	/**
	 * Return the content length, if known, for the given encoding. Otherwise 
	 * return null
	 * 
	 * @param contentEncoding
	 * @return - null, or the length of the content encoded with the given encoding
	 */
	Long getCompressedContentLength(String contentEncoding);
}
