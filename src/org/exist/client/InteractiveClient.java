/*
 * eXist Open Source Native XML Database Copyright (C) 2001-03 Wolfgang M.
 * Meier meier@ifs.tu-darmstadt.de http://exist.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id$
 */
package org.exist.client;

import java.awt.Cursor;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.PushbackInputStream;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;

import org.apache.avalon.excalibur.cli.CLArgsParser;
import org.apache.avalon.excalibur.cli.CLOption;
import org.apache.avalon.excalibur.cli.CLUtil;
import org.apache.xerces.parsers.DOMParser;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.exist.dom.XMLUtil;
import org.exist.schema.SchemaService;
import org.exist.security.Permission;
import org.exist.security.User;
import org.exist.util.CollectionScanner;
import org.exist.util.DirectoryScanner;
import org.exist.util.MimeTable;
import org.exist.util.MimeType;
import org.exist.util.Occurrences;
import org.exist.util.ProgressBar;
import org.exist.util.ProgressIndicator;
import org.exist.util.serializer.SAXSerializer;
import org.exist.util.serializer.SerializerPool;
import org.exist.xmldb.CollectionManagementServiceImpl;
import org.exist.xmldb.DatabaseInstanceManager;
import org.exist.xmldb.EXistResource;
import org.exist.xmldb.IndexQueryService;
import org.exist.xmldb.UserManagementService;
import org.exist.xmldb.XPathQueryServiceImpl;
import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineCompleter;
import org.gnu.readline.ReadlineLibrary;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.BinaryResource;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XPathQueryService;
import org.xmldb.api.modules.XUpdateQueryService;

/**
 * Command-line client based on the XML:DB API.
 *
 * @author wolf
 */
public class InteractiveClient {
    
    // ANSI colors for ls display
    // private final static String ANSI_BLUE = "\033[0;34m";
    private final static String ANSI_CYAN = "\033[0;36m";
    private final static String ANSI_WHITE = "\033[0;37m";
    
    // properties
    protected static String EDIT_CMD = "xemacs $file";
    protected static String ENCODING = "ISO-8859-1";
    protected static String PASS = null;
    protected static String URI = "xmldb:exist://localhost:8080/exist/xmlrpc";
    protected static String USER = org.exist.security.SecurityManager.DBA_USER;
    protected static int PARALLEL_THREADS = 5;
    protected static Properties defaultProps = new Properties();
    {
        defaultProps.setProperty("driver", driver);
        defaultProps.setProperty("uri", URI);
        defaultProps.setProperty("editor", EDIT_CMD);
        defaultProps.setProperty("indent", "true");
        defaultProps.setProperty("encoding", ENCODING);
        defaultProps.setProperty("user", USER);
        defaultProps.setProperty("colors", "false");
        defaultProps.setProperty("permissions", "false");
        defaultProps.setProperty("expand-xincludes", "true");
    }
    
    protected static final int colSizes[] = new int[]{10, 10, 10, -1};
    
    protected static String driver = "org.exist.xmldb.DatabaseImpl";
    protected static String configuration = null;
    
    protected TreeSet completitions = new TreeSet();
    protected LinkedList queryHistory = new LinkedList();
    protected File queryHistoryFile;
    protected File historyFile;
    
    protected Collection current = null;
    protected int nextInSet = 1;
    protected int maxResults = 10;
    protected String path = "/db";
    protected Properties properties;
    
    protected String[] resources = null;
    protected ResourceSet result = null;
    protected HashMap namespaceMappings = new HashMap();
    protected int filesCount = 0;
    
    protected boolean quiet = false;
    protected boolean verbose = false;
    protected boolean recurseDirs = false;
    protected boolean startGUI = true;
    
    protected Writer traceWriter = null;
    protected ClientFrame frame;
    
    public InteractiveClient() {
    }
    
    /** Display help on commands */
    protected void displayHelp() {
        messageln("--- general commands ---");
        messageln("ls                   list collection contents");
        messageln("cd [collection|..]   change current collection");
        messageln("put [file pattern] upload file or directory"
                + " to the database");
        messageln("edit [resource] open the resource for editing");
        messageln("mkcol collection     create new sub-collection in "
                + "current collection");
        messageln("rm document          remove document from current "
                + "collection");
        messageln("rmcol collection     remove collection");
        messageln("set [key=value]      set property. Calling set without ");
        messageln("                     argument shows current settings.");
        messageln("\n--- search commands ---");
        messageln("find xpath-expr      execute the given XPath expression.");
        messageln("show [position]      display query result value at position.");
        messageln("\n--- user management (may require dba rights) ---");
        messageln("users                list existing users.");
        messageln("adduser username     create a new user.");
        messageln("passwd username      change password for user. ");
        messageln("chown user group [resource]");
        messageln("                     change resource ownership. chown without");
        messageln("                     resource changes ownership of the current");
        messageln("                     collection.");
        messageln("chmod [resource] permissions");
        messageln("                     change resource permissions. Format:");
        messageln("                     [user|group|other]=[+|-][read|write|update].");
        messageln("                     chmod without resource changes permissions for");
        messageln("                     the current collection.");
        messageln("lock resource        put a write lock on the specified resource.");
        messageln("unlock resource      remove a write lock from the specified resource.");
        messageln("quit                 quit the program");
    }
    
    /**
     * The main program for the InteractiveClient class.
     *
     * @param args
     *                   The command line arguments
     */
    public static void main(String[] args) {
        try {
            InteractiveClient client = new InteractiveClient();
            client.run(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Register XML:DB driver and retrieve root collection.
     *
     * @exception Exception   Description of the Exception
     */
    protected void connect() throws Exception {
        if (startGUI && frame != null)
            frame.setStatus("connecting to " + properties.getProperty("uri"));
        Class cl = Class.forName(properties.getProperty("driver"));
        Database database = (Database) cl.newInstance();
        database.setProperty("create-database", "true");
        if (properties.containsKey("configuration"))
            database.setProperty("configuration", properties
                    .getProperty("configuration"));
        DatabaseManager.registerDatabase(database);
        current = DatabaseManager.getCollection(properties.getProperty("uri")
        + path, properties.getProperty("user"), properties
                .getProperty("password"));
        if (startGUI && frame != null)
            frame.setStatus("connected to " + properties.getProperty("uri")
            + " as user " + properties.getProperty("user"));
    }
    
    /**
     * Returns the current collection.
     *
     * @return the current collection
     */
    protected Collection getCollection() {
        return current;
    }
    
    protected void reloadCollection() throws XMLDBException {
        current = DatabaseManager.getCollection(properties.getProperty("uri")
        + path, properties.getProperty("user"),
                properties.getProperty("password"));
        getResources();
    }
    
    protected void setProperties() throws XMLDBException {
        String key;
        for(Iterator i = properties.keySet().iterator(); i.hasNext(); ) {
            key = (String)i.next();
            current.setProperty(key, properties.getProperty(key));
        }
    }
    
    /**
     * Get list of resources contained in collection.
     *
     * @exception XMLDBException
     *                         Description of the Exception
     */
    protected void getResources() throws XMLDBException {
        if (current == null)
            return;
        setProperties();
        UserManagementService mgtService = (UserManagementService) current
                .getService("UserManagementService", "1.0");
        String childCollections[] = current.listChildCollections();
        String childResources[] = current.listResources();
        resources = new String[childCollections.length + childResources.length];
        int i = 0;
        Collection child;
        Permission perm;
        
        List tableData = new ArrayList(resources.length); // A list of ResourceDescriptor for the GUI
        
        String cols[] = new String[4];
        for (; i < childCollections.length; i++) {
            child = current.getChildCollection(childCollections[i]);
            perm = mgtService.getPermissions(child);
            if (properties.getProperty("permissions").equals("true")) {
                cols[0] = perm.toString();
                cols[1] = perm.getOwner();
                cols[2] = perm.getOwnerGroup();
                cols[3] = childCollections[i];
                resources[i] = 'd' + formatString(cols, colSizes);
            } else
                resources[i] = childCollections[i];
            
            if (startGUI) {
                tableData.add( new ResourceDescriptor.Collection(
                        childCollections[i],
                        perm.getOwner(),
                        perm.getOwnerGroup(),
                        perm.toString(), null /*lastModificationTime*/ ) );
            }
            completitions.add(childCollections[i]);
        }
        Resource res;
        for (int j = 0; j < childResources.length; i++, j++) {
            res = current.getResource(childResources[j]);
            perm = mgtService.getPermissions(res);
            if (perm == null)
                System.out.println("null");
            if (properties.getProperty("permissions").equals("true")) {
                resources[i] = '-' + perm.toString() + '\t' + perm.getOwner()
                + '\t' + perm.getOwnerGroup() + '\t'
                        + childResources[j];
            } else
                resources[i] = childResources[j];

            Date lastModificationTime = ((EXistResource)res).getLastModificationTime();
            resources[i] += "\t" + lastModificationTime;
            	
            if (startGUI) {
                tableData.add(new ResourceDescriptor.Document(
                        childResources[j],
                        perm.getOwner(),
                        perm.getOwnerGroup(),
                        perm.toString(),
                        lastModificationTime ) );
            }
            completitions.add(childResources[j]);
        }
        if (startGUI)
            frame.setResources(tableData);
    }
    
    /**
     * Display document on screen, by 24 lines.
     *
     * @param str string containing the document.
     */
    protected void more(String str) {
        LineNumberReader reader = new LineNumberReader(new StringReader(str));
        String line;
        // int count = 0;
        int ch;
        try {
            while (System.in.available() > 0)
                System.in.read();
            
            while ((line = reader.readLine()) != null) {
                if (reader.getLineNumber() % 24 == 0) {
                    System.out.print("line: " + reader.getLineNumber()
                    + "; press [return] for more or [q] for quit.");
                    ch = System.in.read();
                    if (ch == 'q' || ch == 'Q')
                        return;
                }
                System.out.println(line);
            }
        } catch (IOException ioe) {
            System.err.println("IOException: " + ioe);
        }
    }
    
    /**
     * In interactive mode, process a line entered by the user.
     *
     * @param line  the line entered
     * @return      true if command != quit
     */
    protected boolean process(String line) {
        if (startGUI)
            frame.setPath(path);
        String args[];
        if (line.startsWith("find")) {
            args = new String[2];
            args[0] = "find";
            args[1] = line.substring(5);
        } else {
            StreamTokenizer tok = new StreamTokenizer(new StringReader(line));
            tok.resetSyntax();
            tok.wordChars(0x21, 0x7FFF);
            tok.quoteChar('"');
            tok.whitespaceChars(0x20, 0x20);
            
            List argList = new ArrayList(3);
            // int i = 0;
            int token;
            try {
                while ((token = tok.nextToken()) != StreamTokenizer.TT_EOF) {
                    if (token == StreamTokenizer.TT_WORD || token == '"') {
                        argList.add(tok.sval);
                    }
                }
            } catch (IOException e) {
                System.err.println("Could not parse command line.");
                return true;
            }
            args = new String[argList.size()];
            argList.toArray(args);
        }
        String newPath = path;
        try {
            if (args[0].equalsIgnoreCase("ls")) {
                // list collection contents
                getResources();
                if (properties.getProperty("permissions").equals("true")) {
                    for (int i = 0; i < resources.length; i++)
                        messageln(resources[i]);
                } else {
                    StringBuffer buf;
                    for (int i = 0; i < resources.length; i++) {
                        buf = new StringBuffer();
                        int k = 0;
                        for (int j = 0; i < resources.length && j < 5; i++, j++) {
                            buf.append(resources[i] + '\t');
                            k = j;
                        }
                        if (k == 4 && i < resources.length)
                            i--;
                        messageln(buf.toString());
                    }
                }
            } else if (args[0].equalsIgnoreCase("cd")) {
                // change current collection
                completitions.clear();
                String tempPath = newPath;
                Collection temp;
                if (args.length < 2 || args[1] == null) {
                    tempPath = "/db";
                    temp = DatabaseManager.getCollection(properties
                            .getProperty("uri")
                            + "/db", properties.getProperty("user"), properties
                            .getProperty("password"));
                } else {
                    if (args[1].equals("..")) {
                        tempPath = newPath.equals("/db") ? "/db" : tempPath
                                .substring(0, newPath.lastIndexOf("/"));
                        if (tempPath.length() == 0)
                            tempPath = "/db";
                    } else if (args[1].startsWith("/"))
                        tempPath = args[1];
                    else
                        tempPath = tempPath + '/' + args[1];
                    
                    temp = DatabaseManager.getCollection(properties
                            .getProperty("uri")
                            + tempPath, properties.getProperty("user"),
                            properties.getProperty("password"));
                }
                if (temp != null) {
                    current = temp;
                    newPath = tempPath;
                    if (startGUI)
                        frame.setPath(newPath);
                } else {
                    messageln("no such collection.");
                }
                getResources();
            } else if (args[0].equalsIgnoreCase("cp")) {
                if (args.length != 3) {
                    messageln("cp requires two arguments.");
                    return true;
                }
                copy(args[1], args[2]);
                getResources();
                
            } else if (args[0].equalsIgnoreCase("edit")) {
                if (args.length == 2) {
                    editResource(args[1]);
                } else {
                    messageln("Please specify a resource.");
                }
            } else if (args[0].equalsIgnoreCase("get")) {
                if (args.length < 2) {
                    System.err.println("wrong number of arguments.");
                    return true;
                }
                Resource res = retrieve(args[1]);
                // display document
                if (res != null) {
                    String data;
                    if (res.getResourceType().equals("XMLResource"))
                        data = (String) res.getContent();
                    else
                        data = new String((byte[]) res.getContent());
                    if (startGUI) {
                        frame.setEditable(false);
                        frame.display(data);
                        frame.setEditable(true);
                    } else {
                        String content = data;
                        more(content);
                    }
                }
                return true;
            } else if (args[0].equalsIgnoreCase("find")) {
                // search
                if (args.length < 2) {
                    messageln("no query argument found.");
                    return true;
                }
                messageln(args[1]);
                long start = System.currentTimeMillis();
                result = find(args[1]);
                if (result == null)
                    messageln("nothing found");
                else
                    messageln("found " + result.getSize() + " hits in "
                            + (System.currentTimeMillis() - start) + "ms.");
                
                nextInSet = 1;
                
            } else if (args[0].equalsIgnoreCase("run")) {
                if (args.length < 2) {
                    messageln("please specify a query file.");
                    return true;
                }
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(
                            args[1]));
                    StringBuffer buf = new StringBuffer();
                    String nextLine;
                    while ((nextLine = reader.readLine()) != null) {
                        buf.append(nextLine);
                        buf.append('\n');
                    }
                    args[1] = buf.toString();
                    long start = System.currentTimeMillis();
                    result = find(args[1]);
                    if (result == null)
                        messageln("nothing found");
                    else
                        messageln("found " + result.getSize() + " hits in "
                                + (System.currentTimeMillis() - start) + "ms.");
                    
                    nextInSet = 1;
                } catch (Exception e) {
                    messageln("An error occurred: " + e.getMessage());
                }
            } else if (args[0].equalsIgnoreCase("show")) {
                // show search results
                if (result == null) {
                    messageln("no result set.");
                    return true;
                }
                try {
                    int start = nextInSet;
                    int count = 1;
                    if (args.length > 1)
                        start = Integer.parseInt(args[1]);
                    
                    if (args.length > 2)
                        count = Integer.parseInt(args[2]);
                    
                    final int s = (int) result.getSize();
                    if (start < 1 || start > s) {
                        messageln("start offset out of range");
                        return true;
                    }
                    --start;
                    if (start + count > s)
                        count = s - start;
                    
                    nextInSet = start + count + 1;
                    for (int i = start; i < start + count; i++) {
                        Resource r = result.getResource((long) i);
                        if (startGUI)
                            frame.display((String) r.getContent());
                        else
                            more((String) r.getContent());
                    }
                    messageln("displayed items " + (start + 1) + " to "
                            + (start + count) + " of " + result.getSize());
                } catch (NumberFormatException nfe) {
                    messageln("wrong argument");
                    return true;
                }
                
            } else if (args[0].equalsIgnoreCase("mkcol")) {
                // create collection
                if (args.length < 2) {
                    messageln("missing argument.");
                    return true;
                }
                CollectionManagementService mgtService = (CollectionManagementService) current
                        .getService("CollectionManagementService", "1.0");
                Collection newCollection = mgtService.createCollection(args[1]);
                if (newCollection == null)
                    messageln("could not create collection.");
                else
                    messageln("created collection.");
                
                // re-read current collection
                current = DatabaseManager.getCollection(properties
                        .getProperty("uri")
                        + path, properties.getProperty("user"), properties
                        .getProperty("password"));
                getResources();
                
            } else if (args[0].equalsIgnoreCase("put")) {
                // put a document or directory into the database
                if (args.length < 2) {
                    messageln("missing argument.");
                    return true;
                }
                boolean r = parse(args[1]);
                getResources();
                return r;
                
            } else if (args[0].equalsIgnoreCase("blob")) {
                // put a document or directory into the database
                if (args.length < 2) {
                    messageln("missing argument.");
                    return true;
                }
                storeBinary(args[1]);
                getResources();
                
            } else if (args[0].equalsIgnoreCase("rm")) {
                // remove document
                if (args.length < 2) {
                    messageln("missing argument.");
                    return true;
                }
                
                remove(args[1]);
                
                // re-read current collection
                current = DatabaseManager.getCollection(properties
                        .getProperty("uri")
                        + path, properties.getProperty("user"), properties
                        .getProperty("password"));
                getResources();
                
            } else if (args[0].equalsIgnoreCase("rmcol")) {
                // remove collection
                if (args.length < 2) {
                    messageln("wrong argument count.");
                    return true;
                }
                rmcol(args[1]);
                // re-read current collection
                current = DatabaseManager.getCollection(properties
                        .getProperty("uri")
                        + path, properties.getProperty("user"), properties
                        .getProperty("password"));
                getResources();
            } else if (args[0].equalsIgnoreCase("adduser")) {
                if (args.length < 2) {
                    System.err.println("Usage: adduser name");
                    return true;
                }
                if (startGUI) {
                    messageln("command not supported in GUI mode. Please use the \"Edit users\" menu option.");
                    return true;
                }
                try {
                    UserManagementService mgtService = (UserManagementService) current
                            .getService("UserManagementService", "1.0");
                    
                    String p1;
                    String p2;
                    while (true) {
                        p1 = Readline.readline("password: ");
                        p2 = Readline.readline("re-enter password: ");
                        if (p1.equals(p2))
                            break;
                        else
                            System.out
                                    .println("\nentered passwords differ. Try again...");
                        
                    }
                    String home = Readline.readline("home collection [none]: ");
                    User user = new User(args[1], p1);
                    if (home != null && home.length() > 0)
                        user.setHome(home);
                    String groups = Readline.readline("enter groups: ");
                    StringTokenizer tok = new StringTokenizer(groups, " ,");
                    String group;
                    while (tok.hasMoreTokens()) {
                        group = tok.nextToken();
                        if (group.length() > 0)
                            user.addGroup(group);
                    }
                    
                    mgtService.addUser(user);
                    System.out.println("user " + user + " created.");
                } catch (Exception e) {
                    System.out.println("ERROR: " + e.getMessage());
                    e.printStackTrace();
                }
            } else if (args[0].equalsIgnoreCase("users")) {
                UserManagementService mgtService = (UserManagementService) current
                        .getService("UserManagementService", "1.0");
                User users[] = mgtService.getUsers();
                System.out.println("User\t\tGroups");
                System.out.println("-----------------------------------------");
                for (int i = 0; i < users.length; i++) {
                    System.out.print(users[i].getName() + "\t\t");
                    for (Iterator j = users[i].getGroups(); j.hasNext(); ) {
                        System.out.print(j.next());
                        if (j.hasNext())
                            System.out.print(", ");
                        
                    }
                    System.out.println();
                }
            } else if (args[0].equalsIgnoreCase("passwd")) {
                if (startGUI) {
                    messageln("command not supported in GUI mode. Please use the \"Edit users\" menu option.");
                    return true;
                }
                if (args.length < 2) {
                    System.out.println("Usage: passwd username");
                    return true;
                }
                try {
                    UserManagementService mgtService = (UserManagementService) current
                            .getService("UserManagementService", "1.0");
                    User user = mgtService.getUser(args[1]);
                    if (user == null) {
                        System.out.println("no such user.");
                        return true;
                    }
                    String p1;
                    String p2;
                    while (true) {
                        p1 = Readline.readline("password: ");
                        p2 = Readline.readline("re-enter password: ");
                        if (p1.equals(p2))
                            break;
                        else
                            System.out
                                    .println("\nentered passwords differ. Try again...");
                        
                    }
                    user.setPassword(p1);
                    mgtService.updateUser(user);
                    properties.setProperty("password", p1);
                } catch (Exception e) {
                    System.err.println("ERROR: " + e.getMessage());
                }
            } else if (args[0].equalsIgnoreCase("chmod")) {
                if (args.length < 2) {
                    System.out.println("Usage: chmod [resource] mode");
                    return true;
                }
                
                Collection temp = null;
                if (args.length == 3) {
                    System.out.println("trying collection: " + args[1]);
                    temp = current.getChildCollection(args[1]);
                    if (temp == null) {
                        System.out.println("\ntrying resource: " + args[1]);
                        Resource r = current.getResource(args[1]);
                        if (r != null) {
                            UserManagementService mgtService = (UserManagementService) current
                                    .getService("UserManagementService", "1.0");
                            mgtService.chmod(r, args[2]);
                        } else
                            System.err.println("Resource " + args[1]
                                    + " not found.");
                    } else {
                        UserManagementService mgtService = (UserManagementService) temp
                                .getService("UserManagementService", "1.0");
                        mgtService.chmod(args[2]);
                    }
                } else {
                    UserManagementService mgtService = (UserManagementService) current
                            .getService("UserManagementService", "1.0");
                    mgtService.chmod(args[1]);
                }
                // re-read current collection
                current = DatabaseManager.getCollection(properties
                        .getProperty("uri")
                        + path, properties.getProperty("user"), properties
                        .getProperty("password"));
                getResources();
            } else if (args[0].equalsIgnoreCase("chown")) {
                if (args.length < 3) {
                    System.out
                            .println("Usage: chown username group [resource]");
                    return true;
                }
                
                Collection temp;
                if (args.length == 4)
                    temp = current.getChildCollection(args[3]);
                else
                    temp = current;
                if (temp != null) {
                    UserManagementService mgtService = (UserManagementService) temp
                            .getService("UserManagementService", "1.0");
                    User u = mgtService.getUser(args[1]);
                    if (u == null) {
                        System.out.println("unknown user");
                        return true;
                    }
                    mgtService.chown(u, args[2]);
                    System.out.println("owner changed.");
                    getResources();
                    return true;
                }
                Resource res = current.getResource(args[3]);
                if (res != null) {
                    UserManagementService mgtService = (UserManagementService) current
                            .getService("UserManagementService", "1.0");
                    User u = mgtService.getUser(args[1]);
                    if (u == null) {
                        System.out.println("unknown user");
                        return true;
                    }
                    mgtService.chown(res, u, args[2]);
                    getResources();
                    return true;
                }
                System.err.println("Resource " + args[3] + " not found.");
                
            } else if (args[0].equalsIgnoreCase("lock") || args[0].equalsIgnoreCase("unlock")) {
                if(args.length < 2) {
                    messageln("Usage: lock resource");
                    return true;
                }
                Resource res = current.getResource(args[1]);
                if (res != null) {
                    UserManagementService mgtService = (UserManagementService)
                    current.getService("UserManagementService", "1.0");
                    User user = mgtService.getUser(properties.getProperty("user", "guest"));
                    if(args[0].equalsIgnoreCase("lock"))
                        mgtService.lockResource(res, user);
                    else
                        mgtService.unlockResource(res);
                }
                
            } else if (args[0].equalsIgnoreCase("elements")) {
                System.out.println("Element occurrences in collection "
                        + current.getName());
                System.out
                        .println("--------------------------------------------"
                        + "-----------");
                IndexQueryService service = (IndexQueryService) current
                        .getService("IndexQueryService", "1.0");
                Occurrences[] elements = service.getIndexedElements(true);
                for (int i = 0; i < elements.length; i++) {
                    System.out
                            .println(formatString(elements[i].getTerm().toString(),
                            Integer.toString(elements[i]
                            .getOccurrences()), 50));
                }
                return true;
                
            } else if (args[0].equalsIgnoreCase("terms")) {
                if (args.length < 3) {
                    System.out
                            .println("Usage: terms [xpath] sequence-start sequence-end");
                    return true;
                }
                IndexQueryService service = (IndexQueryService) current
                        .getService("IndexQueryService", "1.0");
                Occurrences[] terms;
                if (args.length == 3)
                    terms = service.scanIndexTerms(args[1], args[2], true);
                else
                    terms = service.scanIndexTerms(args[1], args[2], args[3]);
                System.out.println("Element occurrences in collection "
                        + current.getName());
                System.out
                        .println("--------------------------------------------"
                        + "-----------");
                for (int i = 0; i < terms.length; i++) {
                    System.out.println(formatString(terms[i].getTerm().toString(), Integer
                            .toString(terms[i].getOccurrences()), 50));
                }
            } else if (args[0].equalsIgnoreCase("xupdate")) {
                if (startGUI) {
                    messageln("command not supported in GUI mode.");
                    return true;
                }
                String lastLine, command = "";
                try {
                    while (true) {
                        lastLine = Readline.readline("| ");
                        if (lastLine == null || lastLine.length() == 0)
                            break;
                        command += lastLine;
                    }
                } catch (EOFException e) {
                } catch (IOException e) {
                }
                String xupdate = "<xu:modifications version=\"1.0\" "
                        + "xmlns:xu=\"http://www.xmldb.org/xupdate\">"
                        + command + "</xu:modifications>";
                XUpdateQueryService service = (XUpdateQueryService) current
                        .getService("XUpdateQueryService", "1.0");
                long mods = service.update(xupdate);
                System.out.println(mods + " modifications processed.");
                
            } else if (args[0].equalsIgnoreCase("map")) {
                StringTokenizer tok = new StringTokenizer(args[1], "= ");
                String prefix;
                if (args[1].startsWith("="))
                    prefix = "";
                else {
                    if (tok.countTokens() < 2) {
                        messageln("please specify a namespace/prefix mapping as: prefix=namespaceURI");
                        return true;
                    }
                    prefix = tok.nextToken();
                }
                String uri = tok.nextToken();
                namespaceMappings.put(prefix, uri);
                
            } else if (args[0].equalsIgnoreCase("set")) {
                if (args.length == 1)
                    properties.list(System.out);
                else
                    try {
                        StringTokenizer tok = new StringTokenizer(args[1], "= ");
                        if (tok.countTokens() < 2) {
                            System.err
                                    .println("please specify a key=value pair");
                            return true;
                        }
                        String key = tok.nextToken();
                        String val = tok.nextToken();
                        System.out.println(key + " = " + val);
                        properties.setProperty(key, val);
                        current.setProperty(key, val);
                        getResources();
                    } catch (Exception e) {
                        System.err.println("Exception: " + e.getMessage());
                    }
            } else if (args[0].equalsIgnoreCase("shutdown")) {
                DatabaseInstanceManager mgr = (DatabaseInstanceManager) current
                        .getService("DatabaseInstanceManager", "1.0");
                if (mgr == null) {
                    messageln("Service is not available");
                    return true;
                }
                mgr.shutdown();
                return true;
            } else if (args[0].equalsIgnoreCase("help") || args[0].equals("?"))
                displayHelp();
            else if (args[0].equalsIgnoreCase("quit")) {
                return false;
                
            }  else if (args[0].equalsIgnoreCase("validate")) {
                if (args.length < 2)
                    messageln("missing document name.");
                else {
                    SchemaService schemaService = (SchemaService) current.getService("SchemaService", "1.0");
                    if (schemaService.validateResource(args[1]))
                        messageln("validated ok.");
                    else
                        messageln("there were errors.");
                }
                
            }  else if (args[0].equalsIgnoreCase("putschema")) {
                if (args.length < 2) {
                    messageln("missing schema file name.");
                } else {
                    importSchema(args[1]);
                    getResources();
                }
                return true;
                
            } else {
                messageln("unknown command");
                return true;
            }
            path = newPath;
            return true;
        } catch (Throwable e) {
            if (startGUI)
                ClientFrame.showErrorMessage(getExceptionMessage(e), e);
            else {
                messageln(getExceptionMessage(e));
                e.printStackTrace();
            }
            return true;
        }
    }
    
    /**
     * @param args
     */
    private void editResource(String name) {
        try {
            
            final Resource res = retrieve(name, properties.getProperty(
                    OutputKeys.INDENT, "yes"));
            DocumentView view = new DocumentView(getCollection(), res, properties);
            view.setSize(new Dimension(640, 400));
            if (res.getResourceType().equals("XMLResource"))
                view.setText((String) res.getContent());
            else
                view.setText(new String((byte[]) res.getContent()));
            
            // lock the resource for editing
            UserManagementService service = (UserManagementService)
            current.getService("UserManagementService", "1.0");
            User user = service.getUser(properties.getProperty("user"));
            String lockOwner = service.hasUserLock(res);
            if((lockOwner == null) || (JOptionPane.showConfirmDialog(this.frame,
                    "Resource is already locked by user " + lockOwner +
                    ". Should I try to relock it?",
                    "Resource locked",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)) {
                try {
                    service.lockResource(res, user);
                } catch(XMLDBException ex) {
                    System.out.println(ex.getMessage());
                    JOptionPane.showMessageDialog(this.frame,
                            "Resource cannot be locked. Opening read-only.");
                    view.setReadOnly();
                }
                view.setVisible(true);
            } else {
                view.dispose();
                this.frame.setCursor(Cursor.getDefaultCursor());
            }
        } catch (IllegalArgumentException ex) {
            messageln("Illegal argument: " + ex.getMessage());
        } catch (XMLDBException ex) {
            messageln("XMLDB error: " + ex.getMessage());
        }
    }
    
    /**
     * @param String filename of the file that contains the schema
     */
    private void importSchema(String filename) throws XMLDBException {
        SchemaService schemaService = (SchemaService) current.getService("SchemaService", "1.0");
        if (schemaService != null) {
            String schemaContents = null;
            try {
                DOMParser parser = new DOMParser();
                parser.parse(filename);
                Document document = parser.getDocument();
                StringWriter sw = new StringWriter();
                XMLSerializer serializer = new XMLSerializer(sw, new OutputFormat(document, "UTF-8", true));
                serializer.serialize(document);
                schemaContents = sw.toString();
                
                schemaService.putSchema(schemaContents);
                messageln("imported schema in file \"" + filename + "\".");
                
            } catch (SAXException saxEx) {
                messageln("Unable to parse schema in " + filename + ": " + saxEx.getMessage());
            } catch (IOException ioEx) {
                messageln("Uable to parse schema in " + filename + ": " + ioEx.getMessage());
            }
        }
    }
    
    private final ResourceSet find(String xpath) throws XMLDBException {
        if (xpath.charAt(xpath.length() - 1) == '\n')
            xpath = xpath.substring(0, xpath.length() - 1);
        if (traceWriter != null)
            try {
                traceWriter.write("<query>");
                traceWriter.write(xpath);
                traceWriter.write("</query>\r\n");
            } catch (IOException e) {
            }
        String sortBy = null;
        int p = xpath.indexOf(" sort by ");
        if (p > -1) {
            String xp = xpath.substring(0, p);
            sortBy = xpath.substring(p + " sort by ".length());
            xpath = xp;
            System.out.println("XPath =   " + xpath);
            System.out.println("Sort-by = " + sortBy);
        }
        XPathQueryServiceImpl service = (XPathQueryServiceImpl) current
                .getService("XPathQueryService", "1.0");
        service
                .setProperty(OutputKeys.INDENT, properties
                .getProperty("indent"));
        service.setProperty(OutputKeys.ENCODING, properties
                .getProperty("encoding"));
        Map.Entry mapping;
        for (Iterator i = namespaceMappings.entrySet().iterator(); i.hasNext(); ) {
            mapping = (Map.Entry) i.next();
            service.setNamespace((String) mapping.getKey(), (String) mapping
                    .getValue());
        }
        
        return (sortBy == null) ? service.query(xpath) : service.query(xpath,
                sortBy);
    }
    
    /** unused, for testing purposes ?? */
    private final void testQuery(String queryFile) {
        try {
            File f = new File(queryFile);
            if (!f.canRead()) {
                System.err.println("can't read query file: " + queryFile);
                return;
            }
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String line;
            ArrayList queries = new ArrayList(10);
            QueryThread thread = null;
            while ((line = reader.readLine()) != null)
                queries.add(line);
            for (int i = 0; i < PARALLEL_THREADS; i++) {
                thread = new QueryThread(queries);
                thread.setName("QueryThread" + i);
                thread.start();
            }
            try {
                thread.join();
            } catch (InterruptedException e) {
            }
        } catch (FileNotFoundException e) {
            System.err.println("ERROR: " + e);
        } catch (IOException e) {
            System.err.println("ERROR: " + e);
        }
    }
    
    private class QueryThread extends Thread {
        
        ArrayList queries;
        
        public QueryThread(ArrayList queries) {
            this.queries = queries;
        }
        
        public void run() {
            try {
                // Collection collection = 
                DatabaseManager.getCollection(
                        properties.getProperty("uri") + path, 
                        properties.getProperty("user"),
                        properties.getProperty("password"));
                XPathQueryService service = (XPathQueryService) current
                        .getService("XPathQueryService", "1.0");
                service.setProperty(OutputKeys.INDENT, "yes");
                service.setProperty(OutputKeys.ENCODING,
                		properties.getProperty("encoding"));
                Random r = new Random(System.currentTimeMillis());
                String query;
                for (int i = 0; i < 10; i++) {
                    query = (String) queries.get(r.nextInt(queries.size()));
                    System.out.println(getName() + " query: " + query);
                    ResourceSet result = service.query(query);
                    System.out.println(getName() + " found: "
                            + result.getSize());
                }
            } catch (XMLDBException e) {
                System.err.println("ERROR: " + e.getMessage());
            }
            System.out.println(getName() + " finished.");
        }
    }
    
    protected final Resource retrieve(String resource) throws XMLDBException {
        return retrieve(resource, properties.getProperty("indent"));
    }
    
    protected final Resource retrieve(String resource, String indent)
    throws XMLDBException {
        Resource res = current.getResource(resource);
        if (res == null) {
            messageln("document not found.");
            return null;
        } else
            return res;
    }
    
    private final void remove(String pattern) throws XMLDBException {
        Collection collection = current;
        if (pattern.startsWith("/")) {
            System.err
                    .println("path pattern should be relative to current collection");
            return;
        }
        Resource resources[];
        Resource res = collection.getResource(pattern);
        if (res == null)
            resources = CollectionScanner.scan(collection, "", pattern);
        else {
            resources = new Resource[1];
            resources[0] = res;
        }
        Collection parent;
        for (int i = 0; i < resources.length; i++) {
            message("removing document " + resources[i].getId() + " ...");
            parent = resources[i].getParentCollection();
            parent.removeResource(resources[i]);
            messageln("done.");
        }
    }
    
    private final void xupdate(String resource, String filename)
    throws XMLDBException, IOException {
        File file = new File(filename);
        if (!(file.exists() && file.canRead())) {
            messageln("cannot read file " + filename);
            return;
        }
        String commands = XMLUtil.readFile(file, "UTF-8");
        XUpdateQueryService service = (XUpdateQueryService) current.getService(
                "XUpdateQueryService", "1.0");
        long modifications = 0;
        if (resource == null)
            modifications = service.update(commands);
        else
            modifications = service.updateResource(resource, commands);
        messageln(modifications + " modifications processed " + "successfully.");
    }
    
    private final void rmcol(String collection) throws XMLDBException {
        CollectionManagementService mgtService = (CollectionManagementService) current
                .getService("CollectionManagementService", "1.0");
        message("removing collection " + collection + " ...");
        mgtService.removeCollection(collection);
        messageln("done.");
    }
    
    private final void copy(String source, String destination) throws XMLDBException {
        CollectionManagementServiceImpl mgtService = (CollectionManagementServiceImpl)
        current.getService("CollectionManagementService", "1.0");
        String destName = null;
        Collection destCol = resolveCollection(destination);
        if(destCol == null) {
            int p = destination.lastIndexOf('/');
            if(p < 0) {
                destName = destination;
                destination = current.getName();
            } else {
                destName = destination.substring(p + 1);
                destination = destination.substring(0, p);
            }
        }
        Resource srcDoc = resolveResource(source);
        if(srcDoc != null) {
            String resourcePath = srcDoc.getParentCollection().getName() + '/' + srcDoc.getId();
            messageln("Copying resource " + resourcePath + " to " + destination);
            mgtService.copyResource(resourcePath, destination, destName);
        } else
            messageln("Copying collection " + source + " to " + destination);
        mgtService.copy(source, destination, destName);
    }
    
    private final void reindex() throws XMLDBException {
        IndexQueryService service = (IndexQueryService)
        current.getService("IndexQueryService", "1.0");
        message("reindexing collection " + current.getName());
        service.reindexCollection();
        messageln("done.");
    }
    
    private final void storeBinary(String fileName) throws XMLDBException {
        File file = new File(fileName);
        if (file.canRead()) {
            MimeType mime = MimeTable.getInstance().getContentTypeFor(file.getName());
            BinaryResource resource = (BinaryResource) current.createResource(
                    file.getName(), "BinaryResource");
            resource.setContent(file);
            ((EXistResource)resource).setMimeType(mime == null ? "application/octet-stream" : mime.getName());
            current.storeResource(resource);
        }
    }
    
    private synchronized boolean findRecursive(Collection collection, File dir,
            String base) {
        File temp[] = dir.listFiles();
        Collection c;
        Resource document;
        CollectionManagementService mgtService;
        String next;
        MimeType mimeType;
        for (int i = 0; i < temp.length; i++) {
            next = base + '/' + temp[i].getName();
            try {
                if (temp[i].isDirectory()) {
                    messageln("entering directory " + temp[i].getAbsolutePath());
                    c = collection.getChildCollection(temp[i].getName());
                    if (c == null) {
                        mgtService = (CollectionManagementService) collection
                                .getService("CollectionManagementService",
                                "1.0");
                        c = mgtService.createCollection(temp[i].getName());
                    }
                    if (c instanceof Observable && verbose) {
                        ProgressObserver observer = new ProgressObserver();
                        ((Observable) c).addObserver(observer);
                    }
                    findRecursive(c, temp[i], next);
                } else {
                    long start1 = System.currentTimeMillis();
                    mimeType = MimeTable.getInstance().getContentTypeFor(temp[i].getName());
                    if(mimeType == null)
                        messageln("File " + temp[i].getName() + " has an unknown " +
                                "suffix. Cannot determine file type.");
                    else {
                        message("storing document " + temp[i].getName() + " (" + i
                                + " of " + temp.length + ") " + "...");
                        document = collection.createResource(temp[i]
                                .getName(), mimeType.getXMLDBType());
                        document.setContent(temp[i]);
                        ((EXistResource)document).setMimeType(mimeType.getName());
                        collection.storeResource(document);
                        ++filesCount;
                        messageln(" " + temp[i].length() + " bytes in "
                                + (System.currentTimeMillis() - start1) + "ms.");
                    }
                }
            } catch (XMLDBException e) {
                messageln("could not parse file " + temp[i].getAbsolutePath());
            }
        }
        return true;
    }
    
    /** stores given Resource
     * @param fileName simple file or directory
     * @return
     * @throws XMLDBException
     */
    protected synchronized boolean parse(String fileName) throws XMLDBException {
        fileName = fileName.replace('/', File.separatorChar).replace('\\',
                File.separatorChar);
        File file = new File(fileName);
        Resource document;
        // String xml;
        File files[];
        if (current instanceof Observable && verbose) {
            ProgressObserver observer = new ProgressObserver();
            ((Observable) current).addObserver(observer);
        }
        if (file.canRead()) {
            if (file.isDirectory()) {
                if (recurseDirs) {
                    filesCount = 0;
                    long start = System.currentTimeMillis();
                    boolean result = findRecursive(current, file, path);
                    messageln("storing " + filesCount + " files took "
                            + ((System.currentTimeMillis() - start) / 1000)
                            + "sec.");
                    return result;
                } else
                    files = file.listFiles();
            } else {
                files = new File[1];
                files[0] = file;
            }
        } else
            files = DirectoryScanner.scanDir(fileName);
        
        long start;
        long start0 = System.currentTimeMillis();
        long bytes = 0;
        MimeType mimeType;
		for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory())
                continue;
			start = System.currentTimeMillis();
            mimeType = MimeTable.getInstance().getContentTypeFor(files[i].getName());
			if(mimeType == null)
                mimeType = MimeType.BINARY_TYPE;
            document = current.createResource(files[i].getName(),
                    mimeType.getXMLDBType());
            message("storing document " + files[i].getName() + " (" + (i + 1)
            + " of " + files.length + ") ...");
            document.setContent(files[i]);
            ((EXistResource)document).setMimeType(mimeType.getName());
            current.storeResource(document);
            messageln("done.");
            messageln("parsing " + files[i].length() + " bytes took "
                    + (System.currentTimeMillis() - start) + "ms.\n");
            bytes += files[i].length();
        }
        messageln("parsed " + bytes + " bytes in "
                + (System.currentTimeMillis() - start0) + "ms.");
        return true;
    }
    
    protected synchronized boolean parse(File[] files, UploadDialog upload)
    throws XMLDBException {
        Resource document;
        if (!upload.isVisible())
            upload.setVisible(true);
        if (current instanceof Observable) {
            ((Observable) current).addObserver(upload.getObserver());
        }
        upload.setTotalSize(calculateFileSizes(files));
        long totalSize = 0;
        String resourceType;
        MimeType mimeType;
        for (int i = 0; i < files.length; i++) {
            if (files[i].canRead()) {
                if (files[i].isDirectory()) {
                    totalSize = findRecursive(current, files[i], path, upload,
                            totalSize);
                } else {
                    upload.reset();
                    upload.setCurrentDir(files[i].getParentFile()
                    .getAbsolutePath());
                    upload.setCurrent(files[i].getName());
                    upload.setCurrentSize(files[i].length());
                    try {
                        mimeType = MimeTable.getInstance().getContentTypeFor(files[i].getName());
                        if (mimeType == null)
                            mimeType = MimeType.BINARY_TYPE;
                        resourceType = mimeType.getType() == MimeType.XML ? "XMLResource" : "BinaryResource";
                        document = current.createResource(
                                files[i].getName(), resourceType);
                        ((EXistResource)document).setMimeType(mimeType.getName());
                        document.setContent(files[i]);
                        current.storeResource(document);
                        totalSize += files[i].length();
                        upload.setStoredSize(totalSize);
                    } catch (XMLDBException e) {
                        upload.showMessage("could not parse file "
                                + files[i].getAbsolutePath() + ": "
                                + e.getMessage());
                    }
                }
            }
        }
        if (current instanceof Observable)
            ((Observable) current).deleteObservers();
        upload.setVisible(false);
        return true;
    }
    
    private long calculateFileSizes(File[] files) throws XMLDBException {
        long size = 0;
        for (int i = 0; i < files.length; i++) {
            if (!files[i].canRead())
                continue;
            if (files[i].isDirectory())
                size += calculateFileSizes(files[i].listFiles());
            else
                size += files[i].length();
        }
        return size;
    }
    
    private long findRecursive(Collection collection, File dir,
            String base, UploadDialog upload, long totalSize) {
        upload.setCurrentDir(dir.getAbsolutePath());
        File temp[] = dir.listFiles();
        Collection c;
        Resource document;
        CollectionManagementService mgtService;
        String next;
        MimeType mimeType;
        for (int i = 0; i < temp.length; i++) {
            next = base + '/' + temp[i].getName();
            try {
                if (temp[i].isDirectory()) {
                    upload.setCurrentDir(temp[i].getAbsolutePath());
                    c = collection.getChildCollection(temp[i].getName());
                    if (c == null) {
                        mgtService = (CollectionManagementService) collection
                                .getService("CollectionManagementService",
                                "1.0");
                        c = mgtService.createCollection(temp[i].getName());
                        
                    }
                    if (c instanceof Observable) {
                        ((Observable) c).addObserver(upload.getObserver());
                    }
                    totalSize = findRecursive(c, temp[i], next, upload,
                            totalSize);
                } else {
                    upload.reset();
                    upload.setCurrent(temp[i].getName());
                    upload.setCurrentSize(temp[i].length());
                    
                    mimeType = MimeTable.getInstance().getContentTypeFor(temp[i].getName());
                    if(mimeType == null)
                        upload.showMessage("File " + temp[i].getName() + " has an unknown " +
                                "suffix. Cannot determine file type.");
                    else {
                        document = collection.createResource(temp[i]
                                .getName(), mimeType.getXMLDBType());
                        ((EXistResource) document).setMimeType(mimeType.getName());
                        document.setContent(temp[i]);
                        collection.storeResource(document);
                    }
                    ++filesCount;
                    totalSize += temp[i].length();
                    upload.setStoredSize(totalSize);
                }
            } catch (XMLDBException e) {
                upload.showMessage("could not parse file "
                        + temp[i].getAbsolutePath() + ": " + e.getMessage());
            }
        }
        return totalSize;
    }
    
    private void mkcol(String collPath) throws XMLDBException {
        System.out.println("creating " + collPath);
        if (collPath.startsWith("/db"))
            collPath = collPath.substring("/db".length());
        CollectionManagementService mgtService;
        Collection c;
        String p = "/db", token;
        StringTokenizer tok = new StringTokenizer(collPath, "/");
        while (tok.hasMoreTokens()) {
            token = tok.nextToken();
            p = p + '/' + token;
            c = DatabaseManager.getCollection(
                    properties.getProperty("uri") + p, properties
                    .getProperty("user"), properties
                    .getProperty("password"));
            if (c == null) {
                mgtService = (CollectionManagementService) current.getService(
                        "CollectionManagementService", "1.0");
                current = mgtService.createCollection(token);
            } else
                current = c;
        }
        path = p;
    }
    
    protected Collection getCollection(String path) throws XMLDBException {
        return DatabaseManager.getCollection(properties.getProperty("uri")
        + path, properties.getProperty("user"), properties
                .getProperty("password"));
    }
    
    /** NEVER USED !!! Reads user password from given input stream. */
    private char[] readPassword(InputStream in) throws IOException {
        
        char[] lineBuffer;
        char[] buf;
        // int i;
        
        buf = lineBuffer = new char[128];
        
        int room = buf.length;
        int offset = 0;
        int c;
        
        loop : while (true)
            switch (c = in.read()) {
                case -1 :
                case '\n' :
                    break loop;
                case '\r' :
                    int c2 = in.read();
                    if ((c2 != '\n') && (c2 != -1)) {
                        if (!(in instanceof PushbackInputStream))
                            in = new PushbackInputStream(in);
                        
                        ((PushbackInputStream) in).unread(c2);
                    } else
                        break loop;
                default :
                    if (--room < 0) {
                        buf = new char[offset + 128];
                        room = buf.length - offset - 1;
                        System.arraycopy(lineBuffer, 0, buf, 0, offset);
                        Arrays.fill(lineBuffer, ' ');
                        lineBuffer = buf;
                    }
                    buf[offset++] = (char) c;
                    break;
            }
            
            if (offset == 0)
                return null;
            
            char[] ret = new char[offset];
            System.arraycopy(buf, 0, ret, 0, offset);
            Arrays.fill(buf, ' ');
            
            return ret;
    }
    
    
    private Properties loadClientProperties(){
        
        Properties clientProps = new Properties();
        
        
        String home = System.getProperty("exist.home");
        File propFile;
        if (home == null)
            propFile = new File("client.properties");
        else
            propFile = new File(home
                    + System.getProperty("file.separator", "/")
                    + "client.properties");
        
        InputStream pin = null;
        
        // Try to load from file
        try{
            pin = new FileInputStream(propFile);
        } catch (FileNotFoundException ex) {
            // File not found, no exception handling
        }
        
        if(pin == null){
            // Try to load via classloader
            pin = InteractiveClient.class
                    .getResourceAsStream("client.properties");
        }
        
        if (pin != null){
            
            // Try to load properties from stream
            try{
                clientProps.load(pin);
            } catch (IOException ex) {
                //
            }
        }
        
        return clientProps;
    }
    
    /**
     *  Parse command line options, store into dedicated object
     * @param args   Arguments
     * @param props  Client configuration
     * @return       Object representing commandline parametres.
     */
    protected CommandlineOptions getCommandlineOptions(String args[], Properties props){
        
        // parse command-line options
        CLArgsParser optParser = new CLArgsParser(args, CommandlineOptions.OPTIONS);
        
        if (optParser.getErrorString() != null) {
            System.err.println("ERROR: " + optParser.getErrorString());
            return null;
        }
        
        List opt = optParser.getArguments();
        int size = opt.size();
        
        CLOption option;
        
        CommandlineOptions cOpt = new CommandlineOptions();
        
        for (int i = 0; i < size; i++) {
            option = (CLOption) opt.get(i);
            switch (option.getId()) {
                case CommandlineOptions.HELP_OPT :
                    printUsage();
                    return null;
                case CommandlineOptions.NO_GUI_OPT :
                    startGUI = false;
                    break;
                case CommandlineOptions.QUIET_OPT :
                    quiet = true;
                    break;
                case CommandlineOptions.VERBOSE_OPT :
                    verbose = true;
                    break;
                case CommandlineOptions.LOCAL_OPT :
                    props.setProperty("uri", "xmldb:exist://");
                    break;
                case CommandlineOptions.USER_OPT :
                    props.setProperty("user", option.getArgument());
                    if (!cOpt.passwdSpecified)
                        cOpt.needPasswd = true;
                    break;
                case CommandlineOptions.PASS_OPT :
                    props.setProperty("password", option.getArgument());
                    cOpt.needPasswd = false;
                    cOpt.passwdSpecified = true;
                    break;
                case CommandlineOptions.CONFIG_OPT :
                    properties.setProperty("configuration", option
                            .getArgument());
                    break;
                case CommandlineOptions.COLLECTION_OPT :
                    path = option.getArgument();
                    cOpt.foundCollection = true;
                    break;
                case CommandlineOptions.RESOURCE_OPT :
                    cOpt.optionResource = option.getArgument();
                    break;
                case CommandlineOptions.OUTPUT_FILE_OPT :
                    cOpt.optionOutputFile = option.getArgument();
                    break;
                case CommandlineOptions.PARSE_OPT :
                    cOpt.doStore = true;
                    if (option.getArgumentCount() == 1)
                        cOpt.optionalArgs.add(option.getArgument());
                    cOpt.interactive = false;
                    break;
                case CommandlineOptions.RECURSE_DIRS_OPT :
                    recurseDirs = true;
                    break;
                case CommandlineOptions.REMOVE_OPT :
                    cOpt.optionRemove = option.getArgument();
                    cOpt.interactive = false;
                    break;
                case CommandlineOptions.GET_OPT :
                    cOpt.optionGet = option.getArgument();
                    cOpt.interactive = false;
                    break;
                case CommandlineOptions.MKCOL_OPT :
                    cOpt.optionMkcol = option.getArgument();
                    cOpt.foundCollection = true;
                    break;
                case CommandlineOptions.RMCOL_OPT :
                    cOpt.optionRmcol = option.getArgument();
                    cOpt.foundCollection = true;
                    cOpt.interactive = false;
                    break;
                case CommandlineOptions.FIND_OPT :
                    cOpt.optionXpath = (option.getArgumentCount() == 1 ? option
                            .getArgument() : "stdin");
                    cOpt.interactive = false;
                    break;
                case CommandlineOptions.RESULTS_OPT :
                    try {
                        maxResults = Integer.parseInt(option.getArgument());
                    } catch (NumberFormatException e) {
                        System.err.println("parameter -n needs a valid number");
                        return null;
                    }
                    break;
                case CommandlineOptions.OPTION_OPT :
                    properties.setProperty(option.getArgument(0), option
                            .getArgument(1));
                    break;
                case CommandlineOptions.QUERY_FILE_OPT :
                    cOpt.optionQueryFile = option.getArgument();
                    cOpt.interactive = false;
                    break;
                case CommandlineOptions.THREADS_OPT :
                    try {
                        PARALLEL_THREADS = Integer.parseInt(option
                                .getArgument());
                    } catch (NumberFormatException e) {
                        System.err.println("parameter -t needs a valid number");
                    }
                    break;
                case CommandlineOptions.XUPDATE_OPT :
                    cOpt.optionXUpdate = option.getArgument();
                    cOpt.interactive = false;
                    break;
                case CommandlineOptions.TRACE_QUERIES_OPT :
                    String traceFile = option.getArgument();
                    File f = new File(traceFile);
                    try {
                        traceWriter = new OutputStreamWriter(
                                new FileOutputStream(f, false), "UTF-8");
                        traceWriter.write("<?xml version=\"1.0\"?>\r\n");
                        traceWriter.write("<query-log>\r\n");
                    } catch (UnsupportedEncodingException e1) {
                    } catch (FileNotFoundException e1) {
                        messageln("Cannot open file " + traceFile);
                    } catch (IOException e) {
                    }
                    break;
                case CommandlineOptions.REINDEX_OPT :
                    cOpt.doReindex = true;
                    cOpt.interactive = false;
                    break;
                case CommandlineOptions.QUERY_GUI_OPT :
                    cOpt.openQueryGui = true;
                    break;
                case CLOption.TEXT_ARGUMENT :
                    cOpt.optionalArgs.add(option.getArgument());
                    break;
            }
        }
        
        return cOpt;
    }
    
    /**
     *  Process the command line options
     * @param cOpt Object representing commandline options
     * @throws java.lang.Exception
     * @return TRUE is all successfull, FALSE of not.
     */
    private boolean processCommandLineActions(CommandlineOptions cOpt)  throws Exception {
        
        
        // process command-line actions
        if (cOpt.doReindex) {
            if(!cOpt.foundCollection) {
                System.err.println("Please specify target collection with --collection");
                shutdown(false);
                return false;
            }
            try {
                reindex();
            } catch (XMLDBException e) {
                System.err.println("XMLDBException while removing collection: "
                        + getExceptionMessage(e));
                e.printStackTrace();
            }
        }
        
        if (cOpt.optionRmcol != null) {
            if (!cOpt.foundCollection) {
                System.err
                        .println("Please specify target collection with --collection");
                shutdown(false);
                return false;
            }
            try {
                rmcol(cOpt.optionRmcol);
            } catch (XMLDBException e) {
                System.err.println("XMLDBException while removing collection: "
                        + getExceptionMessage(e));
                e.printStackTrace();
            }
        }
        
        if (cOpt.optionMkcol != null) {
            try {
                mkcol(cOpt.optionMkcol);
            } catch (XMLDBException e) {
                System.err.println("XMLDBException during mkcol: "
                        + getExceptionMessage(e));
                e.printStackTrace();
            }
        }
        
        if (cOpt.optionGet != null) {
            try {
                Resource res = retrieve(cOpt.optionGet);
                if (res != null) {
                    // String data;
                    if (res.getResourceType().equals("XMLResource")) {
                        if (cOpt.optionOutputFile != null)
                            writeOutputFile(cOpt.optionOutputFile, res.getContent());
                        else
                            System.out.println(res.getContent().toString());
                    } else {
                        if (cOpt.optionOutputFile != null)
                            writeOutputFile(cOpt.optionOutputFile, res.getContent());
                        else
                            System.out.println(new String((byte[]) res
                                    .getContent()));
                    }
                }
            } catch (XMLDBException e) {
                System.err
                        .println("XMLDBException while trying to retrieve document: "
                        + getExceptionMessage(e));
                e.printStackTrace();
            }
        } else if (cOpt.optionRemove != null) {
            if (!cOpt.foundCollection) {
                System.err
                        .println("Please specify target collection with --collection");
            } else {
                try {
                    remove(cOpt.optionRemove);
                } catch (XMLDBException e) {
                    System.out.println("XMLDBException during parse: "
                            + getExceptionMessage(e));
                    e.printStackTrace();
                }
            }
        } else if (cOpt.doStore) {
            if (!cOpt.foundCollection) {
                System.err
                        .println("Please specify target collection with --collection");
            } else {
                for (Iterator i = cOpt.optionalArgs.iterator(); i.hasNext(); )
                    try {
                        parse((String) i.next());
                    } catch (XMLDBException e) {
                        System.out.println("XMLDBException during parse: "
                                + getExceptionMessage(e));
                        e.printStackTrace();
                    }
            }
        } else if (cOpt.optionXpath != null || cOpt.optionQueryFile != null) {
            if (cOpt.optionQueryFile != null) {
                BufferedReader reader = new BufferedReader(new FileReader(
                        cOpt.optionQueryFile));
                StringBuffer buf = new StringBuffer();
                String line;
                while ((line = reader.readLine()) != null) {
                    buf.append(line);
                    buf.append('\n');
                }
                cOpt.optionXpath = buf.toString();
            }
            // if no argument has been found, read query from stdin
            if (cOpt.optionXpath.equals("stdin")) {
                try {
                    BufferedReader stdin = new BufferedReader(
                            new InputStreamReader(System.in));
                    StringBuffer buf = new StringBuffer();
                    String line;
                    while ((line = stdin.readLine()) != null)
                        buf.append(line + '\n');
                    cOpt.optionXpath = buf.toString();
                } catch (IOException e) {
                    System.err.println("failed to read query from stdin");
                    cOpt.optionXpath = null;
                }
            }
            if (cOpt.optionXpath != null) {
                try {
                    ResourceSet result = find(cOpt.optionXpath);
                    if (maxResults <= 0)
                        maxResults = (int) result.getSize();
                    if (cOpt.optionOutputFile == null) {
                        for (int i = 0; i < maxResults && i < result.getSize(); i++)
                            System.out.println(((Resource) result
                                    .getResource((long) i)).getContent());
                    } else {
                        FileWriter writer = new FileWriter(cOpt.optionOutputFile,
                                false);
                        for (int i = 0; i < maxResults && i < result.getSize(); i++)
                            writer.write(((Resource) result
                                    .getResource((long) i)).getContent()
                                    .toString());
                        writer.close();
                    }
                } catch (XMLDBException e) {
                    System.err.println("XMLDBException during query: "
                            + getExceptionMessage(e));
                    e.printStackTrace();
                }
            }
            //		} else if (optionQueryFile != null) {
            //			testQuery(optionQueryFile);
        } else if (cOpt.optionXUpdate != null) {
            try {
                xupdate(cOpt.optionResource, cOpt.optionXUpdate);
            } catch (XMLDBException e) {
                System.err.println("XMLDBException during xupdate: "
                        + getExceptionMessage(e));
            } catch (IOException e) {
                System.err.println("IOException during xupdate: "
                        + getExceptionMessage(e));
            }
        }
        
        return true;
    }
    
    /**
     *  Ask user for login data using gui.
     * @param props     Client properties
     * @return          FALSE when pressed cancel, TRUE is sucessfull.
     */
    private boolean getGuiLoginData(Properties props){
        
        String[] loginData = ClientFrame.getLoginData(properties
                .getProperty("user"), properties.getProperty("uri"));
        if (loginData == null) {
            // User pressed <cancel>
            return false;
        }
        props.setProperty("user", loginData[0]);
        props.setProperty("password", loginData[1]);
        props.setProperty("uri", loginData[2]);
        
        return true;
    }
    
    /**
     *  Reusable method for connecting to database.
     * @return TRUE if successfull,
     */
    private boolean connectToDatabase(){
        try {
            connect();
        } catch (Exception cnf) {
            if (startGUI && frame != null)
                frame.setStatus("Connection to database failed; message: "
                        + cnf.getMessage());
            else
                System.err.println("Connection to database failed; message: "
                        + cnf.getMessage());
            cnf.printStackTrace();
            System.exit(0);
        }
        return true;
    }
    
    /**
     * Main processing method for the InteractiveClient object
     *
     * @param args arguments from main()
     */
    public void run(String args[]) throws Exception {
        
        // initialize with default properties, add client properties
        properties = new Properties(defaultProps);
        properties.putAll(loadClientProperties());
        
        // parse command-line options
        CommandlineOptions cOpt = getCommandlineOptions(args, properties);
        if(cOpt==null){
            // An error occured during parsing. exit program.
            return;
        }
        
        // Fix "uri" property: Excalibur CLI can't parse dashes, so we need to URL encode them:
        properties.setProperty("uri", java.net.URLDecoder.decode(properties.getProperty("uri"), "UTF-8"));
        
        if (!quiet)
            printNotice();
        
        // prompt for password if needed
        if (cOpt.interactive && startGUI) {
            
            boolean haveLoginData = getGuiLoginData(properties);
            if(!haveLoginData){
                System.exit(0);
            }
            
        } else if (cOpt.needPasswd) {
            try {
                properties.setProperty("password", Readline
                        .readline("password: "));
            } catch (Exception e) {
            }
        }
        
        String home = System.getProperty("exist.home");
        
        if (home == null)
            home = System.getProperty("user.dir");
        
        historyFile = new File(home + File.separatorChar + ".exist_history");
        queryHistoryFile = new File(home + File.separatorChar
                + ".exist_query_history");
        
        if (queryHistoryFile.canRead())
            readQueryHistory();
        
        if (cOpt.interactive) {
            // in gui mode we use Readline for history management
            // initialize Readline library
            try {
                Readline.load(ReadlineLibrary.GnuReadline);
                System.out
                        .println("GNU Readline found. IMPORTANT: Don't use GNU Readline");
                System.out
                        .println("to work with other character encodings than ISO-8859-1.");
            } catch (UnsatisfiedLinkError ule) {
                if (!quiet) {
                    System.out
                            .println("GNU Readline not found. Using System.in.");
                    System.out
                            .println("If GNU Readline is available on your system,");
                    System.out
                            .println("add directory ./lib to your LD_LIBRARY_PATH");
                }
            }
            Readline.setEncoding("UTF-8");
            Readline.initReadline("exist");
            Readline.setCompleter(new CollectionCompleter());
            if (historyFile.canRead())
                try {
                    Readline.readHistoryFile(historyFile.getAbsolutePath());
                } catch (Exception e) {
                    // No error handling
                }
        }
        
        // connect to the db
        connectToDatabase();
        
        if (current == null) {
            if (startGUI && frame != null)
                frame.setStatus("Could not retrieve collection " + path);
            else
                System.err.println("Could not retrieve collection " + path);
            shutdown(false);
            return;
        }
        
        boolean processingOK = processCommandLineActions(cOpt);
        if(!processingOK){
            return;
        }
        
        if (cOpt.interactive) {
            if (startGUI) {
                frame = new ClientFrame(this, path, properties);
                frame.setLocation(100, 100);
                frame.setSize(500, 500);
                frame.setVisible(true);
            }
            
            // enter interactive mode
            if( (!startGUI) || (frame == null) ){
                
                // No gui
                try {
                    getResources();
                } catch (XMLDBException e) {
                    System.out.println("XMLDBException while "
                            + "retrieving collection contents: "
                            + getExceptionMessage(e));
                    e.getCause().printStackTrace();
                }
                
            } else {
                
                // with gui ; re-login posibility
                boolean retry=true;
                
                while(retry){
                    
                    String errorMessage="";
                    try {
                        getResources();
                    } catch (XMLDBException e) {
                        
                        errorMessage=getExceptionMessage(e);
                        ClientFrame.showErrorMessage(
                                "XMLDBException occurred while retrieving collection: "
                                + errorMessage, e);
                    }
                    
                    // Determine error text. For special reasons we can retry
                    // to connect.
                    if( errorMessage.matches("^.*Invalid password for user.*$") ||
                            errorMessage.matches("^.*User .* unknown.*") ||
                            errorMessage.matches("^.*Connection refused: connect.*") ){
                        
                        boolean haveLoginData = getGuiLoginData(properties);
                        if(!haveLoginData){
                            // pressed cancel
                            System.exit(0);
                        }
                        
                        // Need to shutdown ?? ask wolfgang
                        shutdown(false);
                        
                        // connect to the db
                        connectToDatabase();
                        
                    } else {
                        
                        if(errorMessage!=""){
                            // No pattern match, but we have an error. stop here
                            frame.dispose();
                            System.exit(1);
                        } else {
                            // No error message, continue startup.
                            retry=false;
                        }
                    }
                }
            }
            
            messageln("\ntype help or ? for help.");
            
            if (cOpt.openQueryGui) {
                QueryDialog qd = new QueryDialog(this, current, properties);
                qd.setLocation(100, 100);
                qd.setVisible(true);
            } else if (!startGUI)
                readlineInputLoop(home);
            else
                frame.displayPrompt();
        } else
            shutdown(false);
    }
    
    public final static String getExceptionMessage(Throwable e) {
        Throwable cause;
        while((cause = e.getCause()) != null)
            e = cause;
        return e.getMessage();
    }
    
    /**
     * @param queryHistoryFile
     */
    protected void readQueryHistory() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory
                    .newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(queryHistoryFile);
            NodeList nodes = doc.getElementsByTagName("query");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element query = (Element) nodes.item(i);
                StringBuffer value = new StringBuffer();
                Node next = query.getFirstChild();
                while (next != null) {
                    value.append(next.getNodeValue());
                    next = next.getNextSibling();
                }
                queryHistory.addLast(value.toString());
            }
        } catch (Exception e) {
            if (startGUI)
                ClientFrame.showErrorMessage(
                        "Error while reading query history: " + e.getMessage(),
                        e);
            else
                messageln("Error while reading query history: "
                        + e.getMessage());
        }
    }
    
    protected void addToHistory(String query) {
        queryHistory.add(query);
    }
    
    protected void writeQueryHistory() {
        try {
            Readline.writeHistoryFile(historyFile.getAbsolutePath());
        } catch (Exception e) {
        }
        Readline.cleanup();
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(
                    queryHistoryFile));
            SAXSerializer serializer = (SAXSerializer) SerializerPool.getInstance().borrowObject(SAXSerializer.class);
            serializer.setOutput(writer, null);
            int p = 0;
            if (queryHistory.size() > 20)
                p = queryHistory.size() - 20;
            AttributesImpl attrs = new AttributesImpl();
            serializer.startDocument();
            serializer.startElement("", "history", "history", attrs);
            for (ListIterator i = queryHistory.listIterator(p); i.hasNext(); ) {
                serializer.startElement("", "query", "query", attrs);
                String next = (String) i.next();
                serializer.characters(next.toCharArray(), 0, next.length());
                serializer.endElement("", "query", "query");
            }
            serializer.endElement("", "history", "history");
            serializer.endDocument();
            writer.close();
            SerializerPool.getInstance().returnObject(serializer);
        } catch (IOException e) {
            System.err.println("IO error while writing query history.");
        } catch (SAXException e) {
            System.err.println("SAX exception while writing query history.");
        }
        
    }
    
    public void readlineInputLoop(String home) {
        String line;
        boolean cont = true;
        while (cont)
            try {
                if (properties.getProperty("colors").equals("true"))
                    line = Readline.readline(ANSI_CYAN + "exist:" + path + ">"
                            + ANSI_WHITE);
                else
                    line = Readline.readline("exist:" + path + ">");
                if (line != null)
                    cont = process(line);
                
            } catch (EOFException e) {
                break;
            } catch (IOException ioe) {
                System.err.println(ioe);
            } catch (Exception e) {
                System.err.println(e);
            }
        try {
            Readline.writeHistoryFile(historyFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Could not write history File to " + historyFile.getAbsolutePath() );
        }
        Readline.cleanup();
        shutdown(false);
        messageln("quit.");
    }
    
    protected final void shutdown(boolean force) {
        if (traceWriter != null)
            try {
                traceWriter.write("</query-log>");
                traceWriter.close();
            } catch (IOException e1) {
            }
        try {
            DatabaseInstanceManager mgr = (DatabaseInstanceManager) current
                    .getService("DatabaseInstanceManager", "1.0");
            if (mgr == null) {
                System.err.println("service is not available");
            } else if (mgr.isLocalInstance() || force) {
                System.out.println("shutting down database...");
                mgr.shutdown();
            }
        } catch (XMLDBException e) {
            System.err.println("database shutdown failed: ");
            e.printStackTrace();
        }
    }
    
    private final void printUsage() {
        System.out.println("Usage: java " + InteractiveClient.class.getName()
        + " [options]");
        System.out.println(CLUtil.describeOptions(CommandlineOptions.OPTIONS).toString());
    }
    
    public void printNotice() {
        messageln("eXist version 1.0, Copyright (C) 2004 Wolfgang Meier");
        messageln("eXist comes with ABSOLUTELY NO WARRANTY.");
        messageln("This is free software, and you are welcome to "
                + "redistribute it\nunder certain conditions; "
                + "for details read the license file.\n");
    }
    
    private final void message(String msg) {
        if (!quiet) {
            if (startGUI && frame != null)
                frame.display(msg);
            else
                System.out.print(msg);
        }
    }
    
    private final void messageln(String msg) {
        if (!quiet) {
            if (startGUI && frame != null)
                frame.display(msg + '\n');
            else
                System.out.println(msg);
        }
    }
    
    private Collection resolveCollection(String path) throws XMLDBException {
        return DatabaseManager.getCollection(properties.getProperty("uri")
        + path, properties.getProperty("user"), properties.getProperty("password"));
    }
    
    private Resource resolveResource(String path) throws XMLDBException {
        String collectionPath;
        String resourceName = path;
        int p = path.lastIndexOf('/');
        if(p < 0) {
            collectionPath = current.getName();
        } else {
            collectionPath = path.substring(0, p);
            resourceName = path.substring(p + 1);
        }
        Collection collection = resolveCollection(collectionPath);
        if(collection == null) {
            messageln("Collection " + collectionPath + " not found.");
            return null;
        }
        messageln("Locating resource " + resourceName + " in collection " + collection.getName());
        return collection.getResource(resourceName);
    }
    
    private class CollectionCompleter implements ReadlineCompleter {
        
        Iterator possibleValues;
        
        public String completer(String text, int state) {
            if (state == 0)
                possibleValues = completitions.tailSet(text).iterator();
            
            if (possibleValues.hasNext()) {
                String nextKey = (String) possibleValues.next();
                if (nextKey.startsWith(text))
                    return nextKey;
            }
            return null;
            // we reached the last choice.
        }
    }
    
    public static class ProgressObserver implements Observer {
        
        ProgressBar elementsProgress = new ProgressBar("storing elements");
        Observable lastObservable = null;
        ProgressBar parseProgress = new ProgressBar("storing nodes   ");
        ProgressBar wordsProgress = new ProgressBar("storing words   ");
        
        public void update(Observable o, Object obj) {
            ProgressIndicator ind = (ProgressIndicator) obj;
            if (lastObservable == null || o != lastObservable)
                System.out.println();
            
            if (o instanceof org.exist.storage.ElementIndex)
                elementsProgress.set(ind.getValue(), ind.getMax());
            else if (o instanceof org.exist.storage.TextSearchEngine)
                wordsProgress.set(ind.getValue(), ind.getMax());
            else
                parseProgress.set(ind.getValue(), ind.getMax());
            
            lastObservable = o;
        }
    }
    
    private void writeOutputFile(String fileName, Object data)
    throws Exception {
        File file = new File(fileName);
        FileOutputStream os = new FileOutputStream(file);
        if (data instanceof byte[]) {
            os.write((byte[]) data);
            os.close();
        } else {
            OutputStreamWriter writer = new OutputStreamWriter(os, Charset
                    .forName(properties.getProperty("encoding")));
            writer.write(data.toString());
            writer.close();
        }
    }
    
    private static String formatString(String s1, String s2, int width) {
        StringBuffer buf = new StringBuffer(width);
        if (s1.length() > width)
            s1 = s1.substring(0, width - 1);
        buf.append(s1);
        int fill = width - (s1.length() + s2.length());
        for (int i = 0; i < fill; i++)
            buf.append(' ');
        buf.append(s2);
        return buf.toString();
    }
    
    private static String formatString(String[] args, int[] sizes) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < args.length; i++) {
            if (sizes[i] < 0) {
                buf.append(args[i]);
            } else {
                for (int j = 0; j < sizes[i] && j < args[i].length(); j++)
                    buf.append(args[i].charAt(j));
            }
            for (int j = 0; j < sizes[i] - args[i].length(); j++)
                buf.append(' ');
        }
        return buf.toString();
    }
    
}
