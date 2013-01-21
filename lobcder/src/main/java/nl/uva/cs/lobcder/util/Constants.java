/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.util;

import javax.xml.namespace.QName;

/**
 *
 * @author S. Koulouzis
 */
public class Constants {

    public static final String LOBCDER_CONF_DIR = System.getProperty("user.home") + "/.lobcder-test/";
    public static final String VPH_USERNAMES = "vph.users";
    public static final String STORAGE_SITE_USERNAME = "storage.site.username";
    public static final String STORAGE_SITE_PASSWORD = "storage.site.password";
    public static final String STORAGE_SITE_GRID_PROXY = "storage.site.grid.proxy";
    public static final String STORAGE_SITE_ENDPOINT = "storage.site.endpoint";
    public static final String LOGICAL_DATA = "logical.data";
    public static final String LOGICAL_FILE = "logical.file";
    public static final String LOGICAL_FOLDER = "logical.folder";
    public static final String LOBCDER_STORAGE_PREFIX = "lobcder.storage.prefix";
    public static final String STORAGE_SITES_PROP_FILES = "lobcder.storage.site.prop.files";
    public static final QName DATA_DIST_PROP_NAME = new QName("custom:", "data-distribution");
    public static final QName DRI_SUPERVISED_PROP_NAME = new QName("custom:", "dri-supervised");
    public static final QName DRI_CHECKSUM_PROP_NAME = new QName("custom:", "dri-checksum-MD5");
    public static final QName DRI_LAST_VALIDATION_DATE_PROP_NAME = new QName("custom:", "dri-last-validation-date-ms");
    public static final QName DAV_CURRENT_USER_PRIVILAGE_SET_PROP_NAME = new QName("current-user-privilege-set");
    public static final QName DAV_ACL_PROP_NAME = new QName("DAV:", "acl");
    public static final QName[] PROP_NAMES = new QName[]{new QName("custom:", "data-distribution"), new QName("custom:", "dri-supervised"), new QName("custom:", "dri-checksum-MD5"), new QName("custom:", "dri-last-validation-date-ms"), new QName("DAV:", "current-user-privilege-set"),};
    public static final int BUF_SIZE = 2097152;
    public static Long LOCK_TIME = new Long(60000);
}
