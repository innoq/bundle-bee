
package org.bundlebee.weaver.equinox;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;
import org.osgi.framework.Constants;
import javassist.CannotCompileException;
import javassist.ClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import org.eclipse.osgi.baseadaptor.loader.ClasspathEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author joergp
 */
public class Weaver {

    private static Logger LOG = LoggerFactory.getLogger(Weaver.class);


	static {
		ClassPool classPool = ClassPool.getDefault();
		// add some imports to the default ClassPool
		// @todo the reason, why they are needed it unclear, for all classnames are given fully qualified
		// but in case of org.bundlebee.remoteservicecall, lacking the import leads to a weave-time exception, in case
		// of the other two to a runtime exception
//	classPool.importPackage( "org.bundlebee.remoteservicecall" );
        classPool.importPackage( "org.bundlebee.weaver" );
        classPool.importPackage( "javassist.runtime" );
	}


	// name of a local variable
	private final static String RETURNOBJECT_VARNAME = "org_bundlebee_weaver_o";

    private static final String MANIFEST_PATH = "META-INF/MANIFEST.MF";

	public Weaver()	{
		// intentionally empty
	}


    /**
     * Weaves our grid code/aspect into a defined set of classes.
     *
     * @param classbytes       bytes of the class file
     * @param classpathEntry   classpathEntry
     * @param bundleEntry      bundle entry
     * @return the new byte array or null in case something went wrong, which will result in a behavior that
     *         is the same as if the hook wasn't there at all.
     * @see org.bundlebee.weaver.ServiceCallAspect
     */
	public byte[] weaveClass( byte[] classbytes, ClasspathEntry classpathEntry, ClassLoader bundleclassloader ) {

		// get the ClassPath from the bundles classloader
		ClassPath bundleloaderclasspath = new LoaderClassPath(bundleclassloader);

		// Create a new ClassPool.
		// This is necessary to fix #29. Obviously makeClass() does not work for different classes (from different bundle versions)
		// with the same name on a single ClassPool instance (leads to 'frozen class' error)
		// It does not remove the necessity to configure the ClassPool.getDefault() in the static area above.
		// Reason is unknown.
		// https://bundle-bee.org/issues/29
		ClassPool classPool = new ClassPool();

		try {
			// append the bundle classloader classpath to my classpool, so that I can resolve all the bundles classes while weaving
			classPool.appendClassPath( bundleloaderclasspath );

			// create a CtClass from the byte[]
            CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classbytes));

			// if the class is an interface, there is nothing to do
            if (ctClass.isInterface()) return null;

			// only if a method is patched, the class needs to be created
			boolean needToMakeClass = false;

            String bundleSymbolicNameVersion = null;

			//
			// all methods
			//
			for (final CtMethod method : ctClass.getDeclaredMethods()) {


				//
                // is instrumentation desired?
                //
                if ( InstrumentedMethods.mustInstrument(method) ) {

                    // look up bundlenameversion lazily
                    if (bundleSymbolicNameVersion == null) {
                        bundleSymbolicNameVersion = getBundleSymbolicNameVersion(classpathEntry);
                        if (LOG.isDebugEnabled()) LOG.debug("BundleSymbolicNameVersion: " + bundleSymbolicNameVersion);
                    }

					// mark the class for re-creation
                    needToMakeClass = true;

                    // weave our code into the method
                    insertServiceCallAspect(method, bundleSymbolicNameVersion);
                }
            }

			//
			// either deliver the new code for the class or nothing
			//
            if (needToMakeClass) {
                return ctClass.toBytecode();
            }
            else {
				return null;
			}

        } catch (IOException e) {
            LOG.error(e.toString(), e);
			throw new RuntimeException(e);

		} catch (CannotCompileException e) {
            LOG.error(e.toString(), e);
			throw new RuntimeException(e);
        }
		finally {

			// get rid of the additional classloaders path again
			classPool.removeClassPath(bundleloaderclasspath);
		}
    }

	private String getBundleSymbolicNameVersion(final ClasspathEntry classpathEntry) {
        String nameVersion = null;
        try {
            final BundleFile bundleFile = classpathEntry.getBundleFile();
            final BundleEntry entry = bundleFile.getEntry(MANIFEST_PATH);
            if (entry != null) {
                final Manifest manifest = new Manifest(entry.getInputStream());
                final Attributes attributes = manifest.getMainAttributes();
                final String symbolicName = attributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
                final String version = attributes.getValue(Constants.BUNDLE_VERSION);
                if (symbolicName != null) {
                    final StringBuilder sb = new StringBuilder();
                    final int semiColon = symbolicName.indexOf(';');
                    if (semiColon == -1) {
                        sb.append(symbolicName);
                    } else {
                        sb.append(symbolicName.substring(0, semiColon));
                    }
                    if (version != null) {
                        sb.append('/').append(version);
                    }
                    nameVersion = sb.toString();
                }
            }
        } catch (IOException e) {
            LOG.error(e.toString(), e);
        }
        return nameVersion;
    }


    /**
     * Inserts calls to {@link org.bundlebee.weaver.ServiceCallAspect} into the given method.
     *
     * @param method a service method
     * @param bundleSymbolicNameVersion bundle / version
     */
    private void insertServiceCallAspect( CtMethod method, String bundleSymbolicNameVersion ) {
        try {

			// get the name of the method
            String methodName = method.getName();
            if (LOG.isInfoEnabled()) LOG.info("Instrumenting method: " + method.getLongName());

			// compute a Java expression for the return value 'o'
			String returnExpression = getReturnExpression(method);

			//
			// Code to insert in front of the existing method body.
			// Ask for a remote call and give back the result.
			// If it cannot happen (no suitables nodes, exceptions occured, better to run locally...), run locally.
			//

			//
			// javassist generates code that resolves paramter class for $sig with Class.forName() by default.
			// For some reasons, bundle types cannot be resolved by that.
			// Therefore, we temporarily switch javassist.runtim.Desc to use Thread.currentThread().getContextClassLoader().
			// When the bundle runs a separate thread to access the method (e.g. a Swing thread started from the bundle),
			// I had to configure the threads contextclassloader to the one of 'this'
			// This is a bare workaround!! I have no idea about the reasons.
			// see javassist.runtim.Desc.getClassObject()
			// see http://bundle-bee.org/issues/show/18
			// @todo clarify classloader situation and remove that workaround
			// @todo clarify thread dependency
			//
			String determineSignature = "javassist.runtime.Desc.useContextClassLoader = true;"
			+ "Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());";

			String o = RETURNOBJECT_VARNAME; // just to make it bit shorter
			String prelude = determineSignature
			+  "Object "+o+" = org.bundlebee.weaver.ServiceCallAspect.tryRemote( $0, \"" +methodName+ "\", \"" +bundleSymbolicNameVersion+ "\", $sig, $args);\n"
			+ "if( "+o+" != org.bundlebee.weaver.ServiceCallAspect.CANNOT_EXECUTE_REMOTELY ) { return "+returnExpression+"; }";

			// code to append to normal method execution
			String postlude = "org.bundlebee.weaver.ServiceCallAspect.finishLocal( $0, \"" +methodName+ "\", $sig );";

			// weave in prelude code
            if (LOG.isDebugEnabled()) LOG.debug("weaving before:\n" + prelude);
            method.insertBefore( prelude );

			// weave in postlude code
            if (LOG.isDebugEnabled()) LOG.debug("weaving after:\n" + postlude);
            method.insertAfter( postlude, true );

		} catch (CannotCompileException e) {
            if (LOG.isErrorEnabled()) LOG.error(e.toString());

			//
			// this is severe and cannot be corrected by the caller, so convert it to a RuntimeException
			//
			throw new RuntimeException( e );
        }
    }


	//
	// map primitive return types to suitable expressions
	//
	private final static Map<CtClass,String> PRIMITIVE_RETURN_EXPRESSIONS = new HashMap<CtClass,String>() {{
		put( CtClass.voidType,		"" );
		put( CtClass.intType,		"((Integer)"	+ RETURNOBJECT_VARNAME + ").intValue()" );
		put( CtClass.shortType,		"((Short)"		+ RETURNOBJECT_VARNAME + ").shortValue()" );
		put( CtClass.longType,		"((Long)"		+ RETURNOBJECT_VARNAME + ").longValue()" );
		put( CtClass.floatType,		"((Float)"		+ RETURNOBJECT_VARNAME + ").floatValue()" );
		put( CtClass.doubleType,	"((Double)"		+ RETURNOBJECT_VARNAME + ").doubleValue()" );
		put( CtClass.booleanType,	"((Boolean)"	+ RETURNOBJECT_VARNAME + ").booleanValue()" );
		put( CtClass.byteType,		"((Byte)"		+ RETURNOBJECT_VARNAME + ").byteValue()" );
		put( CtClass.charType,		"((Character)"	+ RETURNOBJECT_VARNAME + ").charValue()" );
	}};

    /**
     * Creates a String that reflects the return value, casted to the correct type.
     * If the return value is VOID, this method will return an empty String.
     * Otherwise something like <code>(methodReturnType)returnValueVar</code> will be returned.
     *
     * @param method method to create a return expr for
     * @param returnValueVarName the name of the variable that contains the return value
     * @return either "" or "(methodReturnType)o"
     */
    private String getReturnExpression(CtMethod method) {
		String returnValue = "";
        try {
            final CtClass returnType = method.getReturnType();

			if( null == (returnValue = PRIMITIVE_RETURN_EXPRESSIONS.get(returnType) ) )
                returnValue = "(" + returnType.getName() + ")" + RETURNOBJECT_VARNAME;

        } catch (NotFoundException e) {
            String mes = "Return type for method " + method.getName() + " could not be found.";
            LOG.error(mes, e);
            throw new RuntimeException(mes, e);
        }
        return returnValue;
    }
}
