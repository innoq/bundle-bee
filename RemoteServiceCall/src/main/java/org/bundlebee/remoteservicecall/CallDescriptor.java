
package org.bundlebee.remoteservicecall;

import java.io.Serializable;

/**
 * Structure that describes a method for remote OSGi execution.
 * Put into an own object rather then in the Call class bc. it can be cashed and maybe even avoid
 * re-transmission on an ObjectStream in case the stream is reused.
 *
 * Everything here is unmutable for a certain method. So CallDescriptor might be precomputed on weave-time?
 *
 * @author joergp
 */
public class CallDescriptor implements Serializable {
	public final String				ServiceClassName;
    public final String				ServiceFilter;
    public final String				MethodName;
    public final String[]			ParameterTypeNames;

	public CallDescriptor( String svcclsname, String svcfilter, String method, Class<?>[] paramTypes ) {
		ServiceClassName		= svcclsname;
		ServiceFilter			= svcfilter;
		MethodName				= method;
		ParameterTypeNames		= getParameterTypeNames(paramTypes);
	}

	/**
	 * convert parameter types into strings
	 * @param parameterTypes
	 * @return array of strings with parameter type names
	 * @todo wouldn't it be possible/better to transmit the types by serialization instead of the names?
	 */
    private static String[] getParameterTypeNames( Class<?>[] parameterTypes ) {
        final String[] parameterTypeNames = new String[parameterTypes.length];
        for (int i=0; i<parameterTypes.length; i++) {
            parameterTypeNames[i] = parameterTypes[i].getName();
        }
        return parameterTypeNames;
    }

}
