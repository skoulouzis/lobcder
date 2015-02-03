package com.bradmcevoy.http;

import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import java.util.Map;

public interface PostableResource extends  GetableResource {
    String processForm(Map<String,String> parameters, Map<String,FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException;
}
