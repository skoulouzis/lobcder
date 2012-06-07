/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue;

/**
 *
 * @author skoulouz
 */
class NonExistingResourceException extends CatalogueException {

    public NonExistingResourceException(String msg) {
        super(msg);
    }
    
}
