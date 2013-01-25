/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth;

/**
 *
 * @author dvasunin
 */
public interface AuthI {

    MyPrincipal checkToken(String token);
    
}
