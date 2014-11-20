/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.util;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;

/**
 *
 * @author dvasunin
 */
public class CatalogueHelper {

    private static JDBCatalogue catalogue = null;

    public JDBCatalogue getCatalogue() {
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
}
