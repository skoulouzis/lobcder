/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author dvasunin
 */
public class Permissions {

    public static final int OWNER_ROLE = 0;
    public static final int REST_ROLE = 1;
    public static final int ROOT_ADMIN = 2;
    public static final int READWRITE = 3 << 30;
    public static final int READ = 2 << 30;
    public static final int WRITE = 1 << 30;
    public static final int NOACCESS = 0;
    public static final int ROLE_MASK = ~READWRITE;

    public class Exception extends java.lang.Exception {

        Exception(String reason) {
            super(reason);
        }
    }
    private ArrayList<Integer> rolesPerm;

    // first int = owner's user ID
    // role 0 = owner, 1 = rest
    public Permissions(ArrayList<Integer> rolesPerm) throws Exception {
        setRolesPerm(rolesPerm);
    }

    //Create a default one for new resource
    public Permissions(MyPrincipal mp) {
        rolesPerm = new ArrayList<Integer>();
        rolesPerm.add(mp.getUid());
        rolesPerm.add(OWNER_ROLE | READWRITE);
        rolesPerm.add(REST_ROLE | NOACCESS);
        for (Integer role : mp.getRoles()) {
            rolesPerm.add(role | READ);
        } 
    }

    public final synchronized void setRolesPerm(ArrayList<Integer> rolesPerm) throws Exception {
        if (rolesPerm == null || rolesPerm.size() < 3) {
            throw new Exception("Wrong parameter");
        }
        this.rolesPerm = rolesPerm;
    }

    public synchronized ArrayList<Integer> getRolesPerm() {
        return rolesPerm;
    }

    public synchronized void addRolePerm(int role, int perm) {
        rolesPerm.add(role | (perm << 30));
    }

    public synchronized boolean rmRolePerm(Integer role) {
        Iterator<Integer> it = rolesPerm.iterator();
        it.next();
        while (it.hasNext()) {
            Integer rp = it.next();
            if ((rp.intValue() & ROLE_MASK) == role) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    public synchronized boolean canRead(MyPrincipal mp) {
        if(mp == null)
            return false;
        if (mp.getUid().equals(getOwnerId())) {
            Iterator<Integer> it = rolesPerm.iterator();
            it.next(); // first rolePerm        
            while (it.hasNext()) {
                Integer rp = it.next();
                if ((rp.intValue() & ROLE_MASK) == OWNER_ROLE) {
                    if ((rp.intValue() & READ) != 0) {
                        return true;
                    } else {
                        break;
                    }
                }
            }
        }

        {
            Iterator<Integer> it = rolesPerm.iterator();
            it.next(); // first rolePerm        
            while (it.hasNext()) {
                Integer rp = it.next();
                if ((rp.intValue() & ROLE_MASK) == REST_ROLE) {
                    if ((rp.intValue() & READ) != 0) {
                        return true;
                    } else {
                        break;
                    }
                }
            }
        }
        {
            Iterator<Integer> it = rolesPerm.iterator();
            it.next(); // first rolePerm
            while (it.hasNext()) {
                Integer rp = it.next();
                if (((rp.intValue() & READ) != 0) && (mp.getRoles().contains(rp.intValue() & ROLE_MASK))){
                    return true;
                }
            }
        }
        return false;
    }

    public synchronized boolean canWrite(MyPrincipal mp) {
        if(mp == null)
            return false;
        if (mp.getUid().equals(getOwnerId())) {
            Iterator<Integer> it = rolesPerm.iterator();
            it.next(); // first rolePerm        
            while (it.hasNext()) {
                Integer rp = it.next();
                if ((rp.intValue() & ROLE_MASK) == OWNER_ROLE) {
                    if ((rp.intValue() & WRITE) != 0) {
                        return true;
                    } else {
                        break;
                    }
                }
            }
        }

        {
            Iterator<Integer> it = rolesPerm.iterator();
            it.next(); // first rolePerm        
            while (it.hasNext()) {
                Integer rp = it.next();
                if ((rp.intValue() & ROLE_MASK) == REST_ROLE) {
                    if ((rp.intValue() & WRITE) != 0) {
                        return true;
                    } 
                }
            }
        }
        {
            Iterator<Integer> it = rolesPerm.iterator();
            it.next(); // first rolePerm
            while (it.hasNext()) {
                Integer rp = it.next();
                if (((rp.intValue() & WRITE) != 0) && (mp.getRoles().contains(rp.intValue() & ROLE_MASK))){                  
                        return true;                    
                }
            }
        }
        return false;
    }

    public synchronized Integer getOwnerId() {
        return rolesPerm.iterator().next();
    }
}
