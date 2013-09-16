/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.frontend.web;

import com.opensymphony.xwork2.ActionSupport;
import java.util.ArrayList;
import java.util.List;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.resources.StorageSite;

/**
 *
 * @author S. Koulouzis
 */
public class ListStorageSites extends ActionSupport {

//    private JDBCatalogue catalogue;
    private List<String> sites;

    @Override
    public String execute() {

        sites = new ArrayList<String>();
        sites.add("site1");
        sites.add("site2");

        return SUCCESS;


    }

    public List<String> getSites() {
        return sites;
    }

    public void setSites(List<String> sites) {
        this.sites = sites;
    }
}
