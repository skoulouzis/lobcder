/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bradmcevoy.common;

/**
 * Represents a string object that contains an extra information of language. 
 * 
 * @author nabil.shams
 */
public class InternationalizedString{
        private String language;
        private String value;
        public InternationalizedString(String language, String value){
            this.language = language;
            this.value = value;
        }
        
        public String getLanguage(){ return language;}
        public String getValue() {return value;}
        
    }
