package com.ettrema.http.acl;

import com.ettrema.http.AccessControlledResource.Priviledge;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author brad
 */
public class PriviledgeList extends ArrayList<Priviledge>{
    private static final long serialVersionUID = 1L;

    PriviledgeList( List<Priviledge> list ) {
        super(list);
    }

}
