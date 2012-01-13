package org.bundlebee.manager.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import org.bundlebee.remoteservicecall.Callee;

/**
 * Handles service method call requests. 
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 * @author <a href="mailto:joerg.plewe@innoq.com">Jörg Plewe</a>
 */
class ServiceMethodCallServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceMethodCallServlet.class);
    private static final int DEFAULT_SERVICE_RESPONSE_BUFFER_SIZE = 64 * 1024;
    private static final String BUFFER_KEY = "org.bundlebee.manager.service.buffer";
    private int responseBufferSize = new Integer(System.getProperty(BUFFER_KEY, "" + DEFAULT_SERVICE_RESPONSE_BUFFER_SIZE));
    private ManagerImpl manager;

    public ServiceMethodCallServlet(final ManagerImpl manager) {
        this.manager = manager;
    }

	@Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
            
		// response.setBufferSize() is not supported by Servlet Spec 2.1,
		// which is the only one required by OSGi 4
		// so we call it via reflection
		// @todo joergp: what is this for?
		setResponseBufferSize(response, responseBufferSize);

		org.bundlebee.remoteservicecall.Result r = Callee.executeCall(request.getInputStream(), response.getOutputStream(), manager.getBundleContext());

		switch( r.getType() ) {
			case REGULAR:
			case TARGETEXCEPTION:
				response.setStatus(HttpServletResponse.SC_OK);
				break;
			case CALLRUNTIMEXCEPTION:
			case SERVICEEXCEPTION:
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				break;

		}
    }


    private void setResponseBufferSize(final HttpServletResponse response, final int size) {
        try {
            response.getClass().getMethod("setBufferSize", Integer.TYPE).invoke(response, size);
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) LOG.debug(org.bundlebee.manager.impl.Activator.BUNDLE_MARKER, "Failed to set response buffer size. This may be normal, " +
                    "since it is a Servlet Spec > 2.1 feature and does not have to be supported: " + e);
        }
    }
}
