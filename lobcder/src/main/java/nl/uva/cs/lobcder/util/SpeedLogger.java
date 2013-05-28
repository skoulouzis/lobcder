/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 *
 * @author S. Koulouzis
 */
public class SpeedLogger {

    public static void logSpeed(String speed) {
        try {
            PrintWriter out = new PrintWriter((new FileWriter(System.getProperty("user.home")+"/speedLog", true)));
            out.println(speed);
            out.close();
        } catch (IOException e) {
            //oh noes!
        }
    }
}
