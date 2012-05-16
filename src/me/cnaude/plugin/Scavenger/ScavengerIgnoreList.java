/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.cnaude.plugin.Scavenger;

import java.io.*;
import java.util.ArrayList;

/**
 *
 * @author naudec
 */
public class ScavengerIgnoreList implements Serializable{
    private static ArrayList<String> ignoreList = new ArrayList<String>();
    private static final String IGNORE_FILE = "plugins/Scavenger/ignores.ser";
    
    public static void load(Scavenger plug) {        
        File file = new File(IGNORE_FILE);
        if (!file.exists()) {
            plug.logInfo("Ignore file '"+file.getAbsolutePath()+"' does not exist.");
            return;
        }
        try {                
            FileInputStream f_in = new FileInputStream(file);
            ObjectInputStream obj_in = new ObjectInputStream (f_in);
            ignoreList = (ArrayList<String>) obj_in.readObject();
            obj_in.close();               
            plug.logInfo("Loaded ignore list. (Count = "+ignoreList.size()+")");
        }
        catch(Exception e) {
          Scavenger.get().logError(e.getMessage());
          return;
        }
    }
    
    public static void save(Scavenger plug) {
        try {
            File file = new File(IGNORE_FILE); 
            FileOutputStream f_out = new FileOutputStream (file);
            ObjectOutputStream obj_out = new ObjectOutputStream (f_out);
            obj_out.writeObject (ignoreList);
            obj_out.close();
            plug.logInfo("Saved ignore list. (Count = "+ignoreList.size()+")");
        }
        catch(Exception e) {
          Scavenger.get().logError(e.getMessage());
        }
    }
    
    public static void addPlayer(String s,Scavenger plug) {
        if (ignoreList.contains(s))
            return;
        plug.logInfo("Adding " + s + " to ignore list.");
        ignoreList.add(s);
    }
    
    public static void removePlayer(String s,Scavenger plug) {
        plug.logInfo("Removing " + s + " from ignore list.");
        ignoreList.remove(s);
    }
    
    public static boolean isIgnored(String s) {
        if (ignoreList.contains(s))
            return true;
        else
            return false;
    }
}