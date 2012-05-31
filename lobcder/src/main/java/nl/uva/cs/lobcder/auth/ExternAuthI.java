/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth;

import java.util.ArrayList;

/**
 *
 * @author dvasunin
 * 
 */
public interface ExternAuthI {
    /**
     * Checks if token is authenticated by an Authentication Service
     * 
     * @param token a string the user receive from Authentication Service
     * after authorization
     * @return if user is not authorized it returns null. If user is authorized 
     * it returns collection of integers. The first one is WPH-User identifier,
     * the rest are identifiers of the groups (roles) the user belongs to
     */
    public ArrayList<Integer> checkToken(String token);    
}
