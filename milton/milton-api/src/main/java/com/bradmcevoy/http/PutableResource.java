package com.bradmcevoy.http;

import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import java.io.IOException;
import java.io.InputStream;

/*
 * Implemented by collections which allow files to be created within them by
 * PUT requests.
 * <P/>
 * NOTE 1: This interface is not intended for the files which are created by a PUT
 * request. In milton terms a PUT is an operation on the folder, the result of
 * which is the creation of a new resource.
 * <P/>
 * Example<BR>
 * if a user does a PUT to /col/myFile.txt, milton will locate the /col resource
 * and check that it implements PutableResource. Then it will call createNew
 * passing it the name "myFile.txt". The collection resource should then
 * create this new resource and return a reference
 * <P/>
 * NOTE 2: PUT allows new resources to be created and existing ones to be overwritten.
 * It is up to the resource implementator to decide if they want to be able to replace
 * the content of an existing resource, or to remove it and create a new one.
 * <P/>
 * If you are replacing content you are strongly encouraged to implement ReplaceableResource
 * on the file being replaced. Then milton will call replaceContent on the file rathen
 * then createNew on the collection
 *
 *
 *
 */
public interface PutableResource extends CollectionResource {
    /**
     * Create a new resource, or overwrite an existing one
     *
     * @param newName - the name to create within the collection. E.g. myFile.txt
     * @param inputStream - the data to populate the resource with
     * @param length - the length of the data
     * @param contentType - the content type of the data being uploaded. This can be a list, such as "image/pjpeg,image/jpeg". It
	 * is the responsibility of the application to create a resource which also represents those content types, or a subset
     * @return - a reference to the new resource
     * @throws IOException
     * @throws ConflictException
     * @throws NotAuthorizedException
     */
    Resource createNew(String newName, InputStream inputStream, Long length, String contentType) throws IOException, ConflictException, NotAuthorizedException, BadRequestException;
}
