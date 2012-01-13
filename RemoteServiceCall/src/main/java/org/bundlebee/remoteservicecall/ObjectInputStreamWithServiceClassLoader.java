
package org.bundlebee.remoteservicecall;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.osgi.framework.BundleContext;

/**
 *
 * @author joergp
 */
public class ObjectInputStreamWithServiceClassLoader extends ObjectInputStream {
    private ClassLoader mClassLoader;

	private final BundleContext mContext;

	public ObjectInputStreamWithServiceClassLoader( InputStream in, BundleContext ctx ) throws IOException {
		this(in,null,ctx);
	}

	public ObjectInputStreamWithServiceClassLoader( InputStream in, ClassLoader cl ) throws IOException {
		this(in,cl,null);
	}

    public ObjectInputStreamWithServiceClassLoader(InputStream in, ClassLoader classloader, BundleContext ctx ) throws IOException {
        super(in);
        mClassLoader = null==classloader ? this.getClass().getClassLoader() : classloader;
		mContext = ctx;
    }

	public void useClassLoader( ClassLoader cl ) {
		mClassLoader = cl;
	}

	@Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws ClassNotFoundException {

		try {
			// find the class from the specified class loader
			return Class.forName(desc.getName(), true, mClassLoader);
		}
		catch (ClassNotFoundException ex) {
			// if this doesn't work, try a again with the base classloader
			// it happened that java.lang.Enum could not be loaded with the bundles classloader
			return Class.forName(desc.getName());
		}
    }

	BundleContext getBundleContext() {
		return mContext;
	}
}
