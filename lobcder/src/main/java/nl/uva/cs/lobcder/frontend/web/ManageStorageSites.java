/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.frontend.web;

import com.opensymphony.xwork2.ActionSupport;
import java.sql.SQLException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.resources.StorageSite;
import nl.uva.cs.lobcder.util.CatalogueHelper;

/**
 *
 * @author S. Koulouzis
 */
public class ManageStorageSites extends ActionSupport {

    private Collection<StorageSite> sites;
    private static JDBCatalogue catalogue = null;
    private int storageSiteId;

    private JDBCatalogue getCatalogue() {
        if (catalogue == null) {
            String jndiName = "bean/JDBCatalog";
            javax.naming.Context ctx;
            try {
                ctx = new InitialContext();
                if (ctx == null) {
                    throw new Exception("JNDI could not create InitalContext ");
                }
                javax.naming.Context envContext = (javax.naming.Context) ctx.lookup("java:/comp/env");
                catalogue = (JDBCatalogue) envContext.lookup(jndiName);
            } catch (Exception ex) {
                Logger.getLogger(CatalogueHelper.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return catalogue;
    }

    @Override
    public String execute() {
        return SUCCESS;

    }

    public String edit() {
        System.out.println(storageSiteId);
        
        return INPUT;
    }

    public String view() {
        try {
            sites = getCatalogue().getStorageSites();
            return SUCCESS;
        } catch (SQLException ex) {
            return ERROR;
        }
    }

    public Collection<StorageSite> getSites() {
        return sites;
    }

    public void setSites(Collection<StorageSite> sites) {
        this.sites = sites;
    }

    public int getStorageSiteId() {
        return storageSiteId;
    }

    public void setStorageSiteId(int storageSiteId) {
        this.storageSiteId = storageSiteId;
    }
}
