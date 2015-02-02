/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.util;

/**
 *
 * @author dvasunin
 */
public abstract class Debug {

    protected abstract boolean debug();
    
    protected void debug(String msg) {
        if (debug()) {
            System.err.println(this.getClass().getName() + ": " + msg);
        }
    }
}