package org.bundlebee.manager.impl;

import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.bundlebee.manager.impl.Activator.BUNDLE_MARKER;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Handles bundle lifecycles requests.
 * <p/>
 * Date: Jan 5, 2009
 * Time: 10:48:05 AM
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
abstract class BundleLifecycleServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(BundleLifecycleServlet.class);
    private int successStatusCode = HttpServletResponse.SC_OK;

    protected BundleLifecycleServlet() {
    }

    public BundleLifecycleServlet(final int successStatusCode) {
        this.successStatusCode = successStatusCode;
    }

    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "Lifecycle request: " + request.getServletPath() + request.getPathInfo());
        if (request.getPathInfo() == null || request.getPathInfo().length() < 2) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } else {
            final String[] nameVersion = request.getPathInfo().substring(1).split("/");
            if (nameVersion.length != 2) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } else {
                final String symbolicName = nameVersion[0];
                final String version = nameVersion[1];
                try {
                    post(symbolicName, version);
                    response.setStatus(successStatusCode);
                } catch (BundleException e) {
                    LOG.error(BUNDLE_MARKER, e.toString(), e);
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.setContentType("text/plain");
                    e.printStackTrace(response.getWriter());
                }
            }
        }
    }

    public abstract void post(final String symbolicName, final String version) throws BundleException;

}
