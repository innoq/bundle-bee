package org.bundlebee.repository.impl;

import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * HttpContext that simply serves files from the local filesystem - in this
 * case this should be the local maven2 repository.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class SimpleFileHttpContext implements HttpContext {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleFileHttpContext.class);
    private HttpContext httpContext;
    private File root;

    SimpleFileHttpContext(final HttpContext httpContext, final File root) throws IOException {
        this.httpContext = httpContext;
        this.root = root.getCanonicalFile();
    }

    public boolean handleSecurity(final HttpServletRequest httpServletRequest,
                                  final HttpServletResponse httpServletResponse) throws IOException {
        return httpContext.handleSecurity(httpServletRequest, httpServletResponse);
    }

    public URL getResource(final String name) {
        URL url = null;
        try {
            if (LOG.isDebugEnabled()) LOG.debug(Activator.BUNDLE_MARKER, "Requested Resource: " + name);
            final File file = new File(name).getCanonicalFile();
            if (file.toString().startsWith(root.toString())) {
                url = file.toURL();
            } else {
                if (LOG.isDebugEnabled()) LOG.debug(Activator.BUNDLE_MARKER, "Houston, we have a problem: Illegal request: " + name);
            }
        } catch (IOException e) {
            LOG.error(Activator.BUNDLE_MARKER, e.toString(), e);
        }
        return url;
    }

    public String getMimeType(final String name) {
        if (name.endsWith(".jar")) return "application/java-archive";
        return httpContext.getMimeType(name);
    }
}
