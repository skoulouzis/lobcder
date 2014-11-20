/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth;

/**
 *
 * @author dvasunin
 */
public interface PrincipalCacheI {

    MyPrincipal getPrincipal(String token);

    void putPrincipal(String token, MyPrincipal principal, long exp_date);
}
