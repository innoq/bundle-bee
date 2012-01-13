/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.bundlebee.weaver.equinox;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import javassist.CtMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author innoq
 */
public class InstrumentedMethods {

    private static Logger LOG = LoggerFactory.getLogger(InstrumentedMethods.class);

    private static final Set<Pattern> INSTRUMENTED_METHODS;
    private static final String ORG_BUNDLEBEE_WEAVER_INSTRUMENTEDMETHODS = "org.bundlebee.weaver.instrumentedmethods";

    static {

        Set<Pattern> set = new HashSet<Pattern>();
        try {
            set = readInstrumentedMethods();
        } catch (IOException e) {
            LOG.error(e.toString(), e);
        }
        INSTRUMENTED_METHODS = set;
        if (LOG.isDebugEnabled()) LOG.debug("Instrumented method patterns: " + INSTRUMENTED_METHODS);
    }

    private static Set<Pattern> readInstrumentedMethods() throws IOException {
        final File file = new File("bundlebee.ini");
        final Set<Pattern> set = new HashSet<Pattern>();
        if (file.exists()) {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                set.add(Pattern.compile(line));
            }
        } else {
            if (LOG.isInfoEnabled()) LOG.info(file + " does not exist.");
        }
        final String systemProperty = System.getProperty(ORG_BUNDLEBEE_WEAVER_INSTRUMENTEDMETHODS);
        if (systemProperty != null) {
            final String[] strings = systemProperty.split(";");
            for (final String s:strings) {
                set.add(Pattern.compile(s));
            }
        }
        return set;
    }


    /**
     * @return set of patterns for methods to be instrumented
     */
    private static Set<Pattern> get() {
        return INSTRUMENTED_METHODS;
    }


    /**
     * The only purpose of this class.
     * Determines, whether a given method must be instrumented or not.
     * This is determined based on patterns obtained from
     * {@link org.bundlebee.weaver.equinox.InstrumentedMethods#get()}.
     *
     * @param method method name
     * @return true, if it must be instrumented
     */
    public static boolean mustInstrument(CtMethod method) {

		// static methods cannot be woven, bc. the weaved code needs a 'this', see http://bundle-bee.org/issues/show/17
		boolean isStatic = 0 != (method.getModifiers() & java.lang.reflect.Modifier.STATIC);
		if( isStatic ) return false;

		//
		// look for the pattern
		//
		String methodname = method.getLongName();
        for (Pattern pattern : get()) {
            if (pattern.matcher(methodname).matches()) {
                return true;
            }
        }
        return false;
    }

}
