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
    private Long storageSiteId;
    private StorageSite editSite;

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
        
        if (sites == null) {
            try {
                sites = getCatalogue().getStorageSites();
            } catch (SQLException ex) {
                Logger.getLogger(ManageStorageSites.class.getName()).log(Level.SEVERE, null, ex);
                return ERROR;
            }
        }

        for (StorageSite site : sites) {
            if (site.getStorageSiteId() == storageSiteId) {
                editSite = site;
            }
        }

        return "edit";
    }

    public String view() {
        try {
            sites = getCatalogue().getStorageSites();
            for (StorageSite site : sites) {
                site.setCredential(null);
                site.setResourceURI("url");
            }
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

    public StorageSite getEditSite() {
        return editSite;
    }

    public void setEditSite(StorageSite editSite) {
        this.editSite = editSite;
    }

    public Long getStorageSiteId() {
        return storageSiteId;
    }

    public void setStorageSiteId(Long storageSiteId) {
        this.storageSiteId = storageSiteId;
    }
}
