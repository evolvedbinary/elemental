/*
 * eXist-db Open Source Native XML Database
 * Copyright (C) 2001 The eXist-db Authors
 *
 * info@exist-db.org
 * http://www.exist-db.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.exist.http.servlets;

import jakarta.servlet.annotation.MultipartConfig;
import org.apache.logging.log4j.Logger;
import org.exist.EXistException;
import org.exist.http.urlrewrite.XQueryURLRewrite;
import org.exist.security.AuthenticationException;
import org.exist.security.SecurityManager;
import org.exist.security.Subject;
import org.exist.security.XmldbPrincipal;
import org.exist.security.internal.web.HttpAccount;
import org.exist.storage.BrokerPool;
import org.exist.util.Configuration;
import org.exist.util.DatabaseConfigurationException;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.XMLDBException;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@MultipartConfig
public abstract class AbstractExistHttpServlet extends HttpServlet {

	private static final long serialVersionUID = 804071766041263220L;

	public final static String DEFAULT_ENCODING = UTF_8.name();
    
    private BrokerPool pool;
    private String formEncoding = DEFAULT_ENCODING;
    private String containerEncoding = DEFAULT_ENCODING;
    private String defaultUsername = SecurityManager.GUEST_USER;
    private String defaultPassword = SecurityManager.GUEST_USER;
    private Authenticator authenticator;
    private Subject defaultUser = null;
    private boolean internalOnly = false;
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        
        //prepare the database
        try {
            setPool(getOrCreateBrokerPool(config));
        } catch (final EXistException e) {
                throw new ServletException("No database instance available");
        } catch (final DatabaseConfigurationException e) {
                throw new ServletException("Unable to configure database instance: " + e.getMessage(), e);
        }
        
        //general eXist Servlet config
        doGeneralExistServletConfig(config);
    }
    
    @Override
    public void destroy() {
        super.destroy();
        BrokerPool.stopAll(false);
    }
    
    public abstract Logger getLog();
    
    private BrokerPool getOrCreateBrokerPool(final ServletConfig config) throws EXistException, DatabaseConfigurationException, ServletException {

        // Configure BrokerPool
        if(BrokerPool.isConfigured()) {
            getLog().info("Database already started. Skipping configuration ...");
        } else {
            final String confFile = Optional.ofNullable(config.getInitParameter("configuration")).orElse("conf.xml");

            final Optional<Path> dbHome = Optional.ofNullable(config.getInitParameter("basedir"))
                    .map(baseDir ->
                                    Optional.ofNullable(config.getServletContext().getRealPath(baseDir))
                                            .map(rp -> Optional.of(Paths.get(rp)))
                                            .orElse(
                                                    Optional.ofNullable(config.getServletContext().getRealPath("/"))
                                                            .map(dir -> Paths.get(dir).resolve("WEB-INF").toAbsolutePath())
                                            )
                    )
                    .orElse(Optional.ofNullable(config.getServletContext().getRealPath("/")).map(Paths::get));

            getLog().info("EXistServlet: exist.home={}", dbHome.map(Path::toString).orElse("null"));

            final Path cf = dbHome.map(h -> h.resolve(confFile)).orElse(Paths.get(confFile));
            getLog().info("Reading configuration from {}", cf.toAbsolutePath().toString());
            if (!Files.isReadable(cf)) {
                throw new ServletException("Configuration file " + confFile + " not found or not readable");
            }

            final Configuration configuration = new Configuration(confFile, dbHome);
            final String start = config.getInitParameter("start");
            if(start != null && "true".equals(start)) {
                doDatabaseStartup(configuration);
            }
        }

        return BrokerPool.getInstance();
    } 
    
    private void doDatabaseStartup(Configuration configuration) throws ServletException {
        if(configuration == null) {
            throw new ServletException("Database has not been " + "configured");
        }
        
        getLog().info("Configuring eXist instance");
        
        try {
            if(!BrokerPool.isConfigured()) {
                BrokerPool.configure(1, 5, configuration);
            }
        } catch(final EXistException | DatabaseConfigurationException e) {
            throw new ServletException(e.getMessage(), e);
        }

        try {
            getLog().info("Registering XMLDB driver");
            final Class<?> clazz = Class.forName("org.exist.xmldb.DatabaseImpl");
            final Database database = (Database) clazz.newInstance();
            DatabaseManager.registerDatabase(database);
        } catch(final ClassNotFoundException | XMLDBException | IllegalAccessException | InstantiationException e) {
            getLog().info("ERROR", e);
        }
    }
    
    private void doGeneralExistServletConfig(ServletConfig config) {
        String option = config.getInitParameter("use-default-user");
        boolean useDefaultUser = true;
        if(option != null) {
            useDefaultUser = "true".equals(option.trim());
        }
        if(useDefaultUser) {
            option = config.getInitParameter("user");
            if(option != null) {
                setDefaultUsername(option);

                option = config.getInitParameter("password");
                if(option != null) {
                    setDefaultPassword(option);
                }

                if(getDefaultUsername() != null) {
                    try {
                        setDefaultUser(getPool().getSecurityManager().authenticate(getDefaultUsername(), getDefaultPassword()));
                    } catch(final AuthenticationException e) {
                        setDefaultUser(null);
                    }
                } else {
                    setDefaultUser(null);
                }
            } else {
                setDefaultUser(pool.getSecurityManager().getGuestSubject());
            }

            if (getDefaultUser() != null) {
                getLog().info("Using default user {} for all unauthorized requests.", getDefaultUsername());
            } else {
                getLog().error("Default user {} cannot be found.  A BASIC AUTH challenge will be the default.", getDefaultUsername());
            }
        } else {
            getLog().info("No default user.  All requires must be authorized or will result in a BASIC AUTH challenge.");
            setDefaultUser(null);
        }
        
        setAuthenticator(new BasicAuthenticator(getPool()));

        // get form and container encoding's
        final String configFormEncoding = config.getInitParameter("form-encoding");
        if(configFormEncoding != null) {
            setFormEncoding(configFormEncoding);
        }
        
        final String configContainerEncoding = config.getInitParameter("container-encoding");
        if(configContainerEncoding != null) {
            setContainerEncoding(configContainerEncoding);
        }
        
        final String param = config.getInitParameter("hidden");
        if(param != null) {
            internalOnly = Boolean.parseBoolean(param);
        }
    }
    
    protected Subject authenticate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        
        if(isInternalOnly() && request.getAttribute(XQueryURLRewrite.RQ_ATTR) == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        
        Principal principal = HttpAccount.getUserFromServletRequest(request);
        if (principal != null) {return (Subject) principal;}

        // Try to validate the principal if passed from the Servlet engine
        principal = request.getUserPrincipal();

        if (principal != null) {
	        if ( XmldbPrincipal.class.isAssignableFrom( principal.getClass() ) ) {
	
	            final String username = ((XmldbPrincipal) principal).getName();
	            final String password = ((XmldbPrincipal) principal).getPassword();

                getLog().info("Validating Principle: {}", username);
	            try {
	                return getPool().getSecurityManager().authenticate(username, password);
	            } catch (final AuthenticationException e) {
	                getLog().info(e.getMessage());
	            }
	        }
	
	        if (principal instanceof Subject) {
	            return (Subject)principal;
	        }
        }

        // Secondly try basic authentication if there is no Authorization header or the Authorization header does not indicate Basic auth
        final String auth = request.getHeader("Authorization");
        if ((auth == null || !auth.toLowerCase().startsWith("basic ")) && getDefaultUser() != null) {
            return getDefaultUser();
        }
        return getAuthenticator().authenticate(request, response, true);
    }

    protected boolean isInternalOnly() {
        return internalOnly;
    }

    private void setInternalOnly(boolean internalOnly) {
        this.internalOnly = internalOnly;
    }
    
    protected Subject getDefaultUser() {
        return defaultUser;
    }

    private void setDefaultUser(Subject defaultUser) {
        this.defaultUser = defaultUser;
    }

    protected Authenticator getAuthenticator() {
        return authenticator;
    }

    private void setAuthenticator(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    protected String getDefaultPassword() {
        return defaultPassword;
    }

    private void setDefaultPassword(String defaultPassword) {
        this.defaultPassword = defaultPassword;
    }

    protected String getDefaultUsername() {
        return defaultUsername;
    }

    private void setDefaultUsername(String defaultUsername) {
        this.defaultUsername = defaultUsername;
    }
    
    protected String getContainerEncoding() {
        return containerEncoding;
    }

    private void setContainerEncoding(String containerEncoding) {
        this.containerEncoding = containerEncoding;
    }

    protected String getFormEncoding() {
        return formEncoding;
    }

    private void setFormEncoding(String formEncoding) {
        this.formEncoding = formEncoding;
    }

    protected BrokerPool getPool() {
        return pool;
    }
    
    private void setPool(BrokerPool pool) {
        this.pool = pool;
    }
}