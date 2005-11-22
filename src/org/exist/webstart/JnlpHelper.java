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

package org.exist.webstart;

import java.io.File;
import org.apache.log4j.Logger;

/**
 *  Helper class for webstart.
 *
 * @author Dannes Wessels
 */
public class JnlpHelper {
    
    private static Logger logger = Logger.getLogger(JnlpHelper.class);
    private String existHome=System.getProperty("exist.home");
    
    private File coreJarsFolder=null;
    private File existJarFolder=null;
    private File webappFolder=null;
    
    /** Creates a new instance of JnlpHelper */
    public JnlpHelper() {
        
        // Setup path based on installation (in jetty, container)
        if(isInWarFile()){
            // all files mixed in existHome/lib/
            logger.debug("eXist is running in container (.war).");
            coreJarsFolder= new File(existHome, "lib/");
            existJarFolder= new File(existHome, "lib/");
            webappFolder= new File(existHome);
            
        } else {
            // all files located in existHome/lib/core/
            logger.debug("eXist is running private jetty server.");
            coreJarsFolder= new File(existHome, "lib/core");
            existJarFolder= new File(existHome);
            webappFolder= new File(existHome, "webapp");
        }
        logger.debug("CORE jars location="+coreJarsFolder.getAbsolutePath());
        logger.debug("EXIST jars location="+existJarFolder.getAbsolutePath());
        logger.debug("WEBAPP location="+webappFolder.getAbsolutePath());
    }
    
    /**
     *  Check wether exist runs in Servlet container (as war file).
     * @return TRUE if exist runs in servlet container.
     */
    public boolean isInWarFile(){
        
        boolean retVal =true;
        if( new File(existHome, "lib/core").isDirectory() ) {
            retVal=false;
        }
        return retVal;
    }
    
    
    public File getWebappFolder(){
        return webappFolder;
    }
    
    public File getCoreJarsFolder(){
        return coreJarsFolder;
    }
    
    public File getExistJarFolder(){
        return existJarFolder;
    }
    
}
