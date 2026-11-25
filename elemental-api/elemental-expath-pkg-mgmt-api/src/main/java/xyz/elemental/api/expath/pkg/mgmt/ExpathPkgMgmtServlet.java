package xyz.elemental.api.expath.pkg.mgmt;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.EXistException;
import org.exist.http.servlets.AbstractExistHttpServlet;
import org.exist.repo.ExistRepository;
import org.exist.security.Subject;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.Txn;
import org.exist.util.serializer.SAXSerializer;
import org.exist.util.serializer.SerializerPool;
import org.expath.pkg.repo.Packages;
import org.expath.pkg.repo.Package;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import xyz.elemental.mediatype.MediaType;

import javax.xml.transform.OutputKeys;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Optional;
import java.util.Properties;


public class ExpathPkgMgmtServlet extends AbstractExistHttpServlet {

    private final static Logger LOG = LogManager.getLogger(ExpathPkgMgmtServlet.class);

    protected final static Properties defaultProperties = new Properties();

    static {
        defaultProperties.setProperty(OutputKeys.INDENT, "yes");
        defaultProperties.setProperty(OutputKeys.MEDIA_TYPE, MediaType.APPLICATION_XML);
    }

    private static final String PKG_API_NS = "http://expath.org/ns/pkg-api";
    private static final String PKG_NS = "http://expath.org/ns/pkg";
    private static final String PKG_PREFIX = "pkg";

    @Override
    public Logger getLog() {
        return LOG;
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

        // authenticate the user
        final Subject user = authenticate(request, response);
        if (user == null) {
            // You now get a HTTP Authentication challenge if there is no user
            return;
        }
        // process the request and a connection for this user
        try (final DBBroker broker = getPool().get(Optional.of(user));
             final Txn transaction = getPool().getTransactionManager().beginTransaction()) {

            try {
                final Optional<ExistRepository> repo = broker.getBrokerPool().getExpathRepo();

                if (repo.isPresent()) {
                    writePackageList(response, getContainerEncoding(), repo.get());
                } else {
                    throw new ServletException("expath repository not available");
                }

                transaction.commit();

            } catch (final Throwable t) {
                transaction.abort();
                throw t;
            }

        } catch (final EXistException e) {
            if (response.isCommitted()) {
                throw new ServletException(e.getMessage(), e);
            }
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (final Throwable e) {
            getLog().error(e.getMessage(), e);
            throw new ServletException("An error occurred: " + e.getMessage(), e);
        }
    }

    protected void writePackageList(final HttpServletResponse response, final String encoding,
                                    final ExistRepository repo) throws IOException {

        response.setContentType(MediaType.APPLICATION_XML + "; charset=" + encoding);

        SAXSerializer serializer = null;

        Writer writerToClose = null;
        try {
            final Writer writer = new OutputStreamWriter(response.getOutputStream(), encoding);
            serializer = (SAXSerializer) SerializerPool.getInstance().borrowObject(SAXSerializer.class);

            serializer.setOutput(writer, defaultProperties);

            serializer.startDocument();
            serializer.startPrefixMapping("", PKG_API_NS);
            serializer.startPrefixMapping(PKG_PREFIX, PKG_NS);

            final AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute("", "spec", "spec", "CDATA", "1.0.0");

            serializer.startElement(PKG_API_NS, "package-list", "package-list", attrs);

            for (final Packages pkgs : repo.getParentRepo().listPackages()) {
                final Package pkg = pkgs.latest();
                attrs.clear();
                attrs.addAttribute("", "name", "name", "CDATA", pkgs.name());
                attrs.addAttribute("", "abbrev", "abbrev", "CDATA", pkg.getAbbrev());
                attrs.addAttribute("", "version", "version", "CDATA", pkg.getVersion());
                serializer.startElement(PKG_NS, "package", PKG_PREFIX + ":package", attrs);
                serializer.endElement(PKG_NS, "package", PKG_PREFIX + ":package");
            }

            serializer.endElement(PKG_API_NS, "package-list", "package-list");

            serializer.endDocument();

            writer.flush();
            writerToClose = writer;

        } catch (final SAXException e) {
            // should never happen
            LOG.warn("Error while serializing package list: {}", e.getMessage(), e);
        } finally {
            if (serializer != null) {
                SerializerPool.getInstance().returnObject(serializer);
            }
            if (writerToClose != null) {
                writerToClose.close();
            }
        }
    }

}
