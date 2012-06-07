/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import java.util.LinkedList;
import org.junit.Test;
import static org.junit.Assert.*;
import static nl.uva.cs.lobcder.auth.Permissions.*;

/**
 *
 * @author dvasunin
 */
public class PermissionsTest {
    
    @Test
    public void testDefaultPermConstructor() throws MyPrincipal.Exception {
        System.out.println("testDefaultPermConstructor");
        Integer[] blob = {1234, 3, 4, 5};
        MyPrincipal mp = new MyPrincipal("token", Arrays.asList(blob));
        Permissions p = new Permissions(mp);
        Integer[] expected = {1234, OWNER_ROLE | READWRITE, 3 | READ, 4 | READ, 5 | READ, REST_ROLE | NOACCESS};
        assertEquals(expected[0], p.getOwnerId());
        assertTrue(p.getRolesPerm().containsAll(Arrays.asList(expected)) && Arrays.asList(expected).containsAll(p.getRolesPerm()));        
    }

    @Test
    public void testConstructor() throws Permissions.Exception {
        System.out.println("testConstructor");
        Integer[] perm = {1234, 3 | READ, 0 | READWRITE, 4 | READ, 1 | NOACCESS, 5 | READ};  
        ArrayList<Integer> array = new ArrayList(Arrays.asList(perm)); 
        Permissions p = new Permissions(array);
        
        assertEquals(perm[0], p.getOwnerId());
        assertEquals(perm[0], p.getRolesPerm().iterator().next());
    }
    
    @Test
    public void testrmRolePerm() throws Permissions.Exception {
        System.out.println("rmRolePerm");
        Integer[] perm = {1234, 3 | READ, 0 | READWRITE, 4 | READ, 1 | NOACCESS, 5 | READ};
        ArrayList<Integer> array = new ArrayList(Arrays.asList(perm)); 
        Permissions p = new Permissions(array);
        p.rmRolePerm(3);
        assertFalse(p.getRolesPerm().subList(1, p.getRolesPerm().size()).contains(3));
    }
    
    @Test
    public void testBlobEquality() throws MyPrincipal.Exception{
        System.out.println("testBlobEquality");
        Integer[] blob = {1234, 3, 4, 5};
        MyPrincipal mp = new MyPrincipal("token", Arrays.asList(blob));
        Permissions p = new Permissions(mp);
        Integer[] perm = {1234, 3 | READ, 0 | READWRITE, 4 | READ, 1 | NOACCESS, 5 | READ};
        assertEquals(perm[0], mp.getUid());
        assertTrue(p.getRolesPerm().containsAll(Arrays.asList(perm)) && Arrays.asList(perm).containsAll(p.getRolesPerm()));
    }

    @Test(expected = Permissions.Exception.class)
    public void testConstructorExceptionNullParam() throws Permissions.Exception {
        System.out.println("testConstructorExceptionNullParam");
        List<Integer> perm = null;
        ArrayList<Integer> array = new ArrayList(Arrays.asList(perm)); 
        Permissions p = new Permissions(array);
        System.out.println(p.getOwnerId());
    }

    @Test(expected = Permissions.Exception.class)
    public void testConstructorExceptionEmptyList() throws Permissions.Exception {
        System.out.println("testConstructorExceptionEmptyList");
        LinkedList<Integer> perm = new LinkedList<Integer>();
        ArrayList<Integer> array = new ArrayList(Arrays.asList(perm)); 
        Permissions p = new Permissions(array);
        System.out.println(p.getOwnerId());
    }
    
    @Test(expected = Permissions.Exception.class)
    public void testConstructorExceptionOwnerIdOnly() throws Permissions.Exception {
        System.out.println("testConstructorExceptionOwnerIdOnly");
        LinkedList<Integer> perm = new LinkedList<Integer>();
        perm.add(1234);
        ArrayList<Integer> array = new ArrayList(Arrays.asList(perm)); 
        Permissions p = new Permissions(array);
        System.out.println(p.getOwnerId());
    }       

    @Test
    public void testCanWrite() throws MyPrincipal.Exception, Permissions.Exception {
        System.out.println("testCanWrite");
        Integer[] perm = {1234, 3 | READ, 0 | READWRITE, 4 | READ, 1 | NOACCESS, 5 | READ, 6 | READWRITE};  
        ArrayList<Integer> array = new ArrayList(Arrays.asList(perm)); 
        Permissions p = new Permissions(array);
        Integer[] mpb1 = {1234, 3, 4, 5};
        MyPrincipal mp = new MyPrincipal("token", Arrays.asList(mpb1));
        assertTrue(p.canWrite(mp));
        Integer[] mpb2 = {4321, 3, 4, 5};
        mp = new MyPrincipal("token", Arrays.asList(mpb2));
        assertFalse(p.canWrite(mp));
        Integer[] mpb3 = {4321, 3, 4, 5, 6};
        mp = new MyPrincipal("token", Arrays.asList(mpb3));
        assertTrue(p.canWrite(mp));
    }
    

    @Test
    public void testCanRead() throws MyPrincipal.Exception, Permissions.Exception {
        System.out.println("testCanRead");
        Integer[] perm = {1234, 3 | READ, 0 | READWRITE, 4 | READ, 1 | NOACCESS, 5 | READ, 6 | READWRITE};  
        ArrayList<Integer> array = new ArrayList(Arrays.asList(perm)); 
        Permissions p = new Permissions(array);
        Integer[] mpb1 = {1234, 7, 8, 9};
        MyPrincipal mp1 = new MyPrincipal("token", Arrays.asList(mpb1));
        assertTrue(p.canRead(mp1));
        Integer[] mpb2 = {4321, 3, 8, 9};
        MyPrincipal mp2 = new MyPrincipal("token", Arrays.asList(mpb2));
        assertTrue(p.canRead(mp2));
        Integer[] mpb3 = {4321, 7, 8, 9};
        MyPrincipal mp3 = new MyPrincipal("token", Arrays.asList(mpb3));
        assertFalse(p.canRead(mp3));
        Integer[] mpb4 = {1234, 6, 8, 9};
        MyPrincipal mp4 = new MyPrincipal("token", Arrays.asList(mpb4));
        assertTrue(p.canRead(mp4));
        Integer[] mpb5 = {4321, 6, 5, 9};
        MyPrincipal mp5 = new MyPrincipal("token", Arrays.asList(mpb5));
        assertTrue(p.canRead(mp5));
    }            
}
