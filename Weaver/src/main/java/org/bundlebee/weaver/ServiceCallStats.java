package org.bundlebee.weaver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track of how long local/remote calls took.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class ServiceCallStats {

    private static Logger LOG = LoggerFactory.getLogger(ServiceCallStats.class);
    private static final int DEFAULT_MAX_SAMPLES = 1000;
    private static final int DEFAULT_MIN_SAMPLES = 5;

    private final Map<URI, Map<ServiceCall, Mean>> URICallMap = new ConcurrentHashMap<URI, Map<ServiceCall, Mean>>();
    private final Map<ServiceCall, Long> callCount = new HashMap<ServiceCall, Long>();
    private final int maxSamples;
    private final int minSamples;

    /**
     * @param maxSamples max number of recent samples considered for computing means.
     * @param minSamples min number of samples required for meaningful means
     */
    public ServiceCallStats(final int maxSamples, final int minSamples) {
        if (maxSamples < 1) throw new IllegalArgumentException("MaxSamples must be greater than 0.");
        if (minSamples > maxSamples) throw new IllegalArgumentException("MinSamples must be less or equal to MaxSamples.");
        if (minSamples < 1) throw new IllegalArgumentException("MinSamples must be greater than 0.");
        this.maxSamples = maxSamples;
        this.minSamples = minSamples;
    }

    public ServiceCallStats() {
        this(DEFAULT_MAX_SAMPLES, DEFAULT_MIN_SAMPLES);
    }

    /**
     * Max number of samples considered for computing averages.
     *
     * @return a positive integer
     */
    public int getMaxSamples() {
        return maxSamples;
    }

    /**
     * Min number of samples for meaningful means.
     *
     * @return min samples
     */
    public int getMinSamples() {
        return minSamples;
    }

    /**
     * Let's you log how long a local call took.
     *
     * @param service the service object
     * @param methodName the methodname
     * @param parameterTypes the parameter types
     * @param duration the duration (whatever unit you put in, you get out)
     */
    public void logLocalCall(final Object service, final String methodName,
                                          final Class[] parameterTypes, final long duration) {
        logCall(ServiceCallAspect.LOCAL_URI, service,  methodName, parameterTypes, duration);
    }

    /**
     * Let's you log how long a call took.
     *
     * @param uri URI - this can be {@link org.bundlebee.weaver.ServiceCallAspect#LOCAL_URI}
     * @param service the service object
     * @param methodName the methodname
     * @param parameterTypes the parameter types
     * @param duration the duration (whatever unit you put in, you get out)
     */
    public void logCall(final URI uri, final Object service, final String methodName,
                              final Class[] parameterTypes, final long duration) {
        final URI actualURI = uri == null ? ServiceCallAspect.LOCAL_URI : uri;
        if (LOG.isDebugEnabled()) LOG.debug("Call to " + actualURI +": " + duration + " time units");
        final ServiceCall serviceCall = new ServiceCall(service, methodName, parameterTypes);
        incrementCallCount(serviceCall);
        getMean(actualURI, serviceCall).add(duration);
    }

    private void incrementCallCount(final ServiceCall serviceCall) {
        synchronized (callCount) {
            final Long count = callCount.get(serviceCall);
            if (count == null) callCount.put(serviceCall, 1L);
            else callCount.put(serviceCall, count + 1L);
        }
    }

    /**
     * Indicates how many times a method was called regardless of which URI it was called on.
     *
     * @param service service
     * @param methodName method
     * @param parameterTypes parameter types
     * @return count
     */
    public long getCallCount(final Object service, final String methodName,
                                      final Class[] parameterTypes) {
        final ServiceCall serviceCall = new ServiceCall(service, methodName, parameterTypes);
        Long count;
        synchronized (callCount) {
            count = callCount.get(serviceCall);
            if (count == null) count = 0L;
        }
        return count;
    }


    /**
     * Let's you find out how long a local call took in the past (on average).
     *
     * @param service the service object
     * @param methodName the methodname
     * @param parameterTypes the parameter types
     * @return mean of earlier logged call durations or -1, if fewer than {@link #getMinSamples()} calls were logged
     */
    public long getLocalCallMean(final Object service, final String methodName,
                                           final Class[] parameterTypes) {
        return getMean(ServiceCallAspect.LOCAL_URI, service, methodName, parameterTypes).getMean();
    }

    /**
     * Let's you find out how long a remote call took in the past (on average).
     *
     * @param uri URI for the remote host in question
     * @param service the service object
     * @param methodName the methodname
     * @param parameterTypes the parameter types @return mean of earlier logged call durations or -1,
     * if fewer than {@link #getMinSamples()} calls were logged
     * @return mean of earlier logged call durations or -1, if fewer than {@link #getMinSamples()} calls were logged
     */
    public long getCallMean(final URI uri, final Object service, final String methodName,
                                  final Class[] parameterTypes) {
        return getMean(uri == null ? ServiceCallAspect.LOCAL_URI : uri, service, methodName, parameterTypes).getMean();
    }

    /**
     * Indicates, whether a local call should be cheaper.
     * This method is biased towards <code>false</code> as return value, meaning its biased towards
     * remote calls.
     *
     * @param uri URI of the remote manager to compare with
     * @param service service
     * @param methodName method name
     * @param parameterTypes parameter types
     * @return true, if a local call is cheaper AND we actually have enough data.
     * false, if we don't have enough data OR the remote call is cheaper
     */
    public boolean isLocalCallCheaper(final URI uri, final Object service, final String methodName,
                                      final Class[] parameterTypes) {
        final long localCallMean = getLocalCallMean(service, methodName, parameterTypes);
        final long remoteCallMean = getCallMean(uri, service, methodName, parameterTypes);
        return localCallMean > 0 && remoteCallMean > 0 && localCallMean < remoteCallMean;
    }

    /**
     * Indicates, whether a local call should be cheaper.
     * This method is biased towards <code>false</code> as return value, meaning its biased towards
     * remote calls.
     *
     * @param service service
     * @param methodName method name
     * @param parameterTypes parameter types
     * @return true, if a local call is cheaper AND we actually have enough data.
     * false, if we don't have enough data OR the remote call is cheaper
     */
    public boolean isLocalCallCheaper(final Object service, final String methodName,
                                      final Class[] parameterTypes) {
        final long localCallMean = getLocalCallMean(service, methodName, parameterTypes);
        long minRemoteCallMean = -1;
        for (final URI uri : this.URICallMap.keySet()) {
            if (uri == ServiceCallAspect.LOCAL_URI) continue;
            final long remoteCallMean = getCallMean(uri, service, methodName, parameterTypes);
            if (remoteCallMean > 0 && remoteCallMean < minRemoteCallMean) minRemoteCallMean = remoteCallMean;
        }
        return localCallMean > 0 && minRemoteCallMean > 0 && localCallMean < minRemoteCallMean;
    }

    /**
     * Returns the URI with the lowest call mean.
     *
     * @param service service
     * @param methodName method name
     * @param parameterTypes parameter types
     * @return regular URI, {@link org.bundlebee.weaver.ServiceCallAspect#LOCAL_URI}
     * or null, if we don't have enough data
     */
    public URI getMinCallMeanURI(final Object service, final String methodName,
                                      final Class[] parameterTypes) {
        long minCallMean = -1;
        URI minCallMeanURI = null;
        for (final URI uri : this.URICallMap.keySet()) {
            final long remoteCallMean = getCallMean(uri, service, methodName, parameterTypes);
            if (remoteCallMean > 0 && remoteCallMean < minCallMean) {
                minCallMean = remoteCallMean;
                minCallMeanURI = uri;
            }
        }
        return minCallMeanURI;
    }

    private Mean getMean(final URI uri, final Object service, final String methodName, final Class[] parameterTypes) {
        final ServiceCall serviceCall = new ServiceCall(service, methodName, parameterTypes);
        return getMean(uri, serviceCall);
    }

    private Mean getMean(final URI uri, final ServiceCall serviceCall) {
        Map<ServiceCall, Mean> calls = this.URICallMap.get(uri);
        if (calls == null) {
            calls = new ConcurrentHashMap<ServiceCall, Mean>();
            this.URICallMap.put(uri, calls);
        }
        Mean mean = calls.get(serviceCall);
        if (mean == null) {
            mean = new Mean(maxSamples, minSamples);
            calls.put(serviceCall, mean);
        }
        return mean;
    }

    /**
     * Mean.
     */
    private static class Mean {
        private List<Long> durations = new LinkedList<Long>();
        private long sum;
        private int maxSamples = 1000;
        private int minSamples = 5;

        private Mean(final int maxSamples, final int minSamples) {
            if (maxSamples < 1) throw new IllegalArgumentException();
            this.maxSamples = maxSamples;
            this.minSamples = minSamples;
        }

        public synchronized void add(final long duration) {
            // avoid overflow
            while ((Long.MAX_VALUE - sum < duration && !durations.isEmpty()) || durations.size() >= maxSamples) {
                sum -= durations.remove(0);
            }
            durations.add(duration);
            sum += duration;
        }

        public synchronized long getMean() {
            if (durations.size() < minSamples) return -1;
            return sum/durations.size();
        }

        /**
         * @return the number of samples this mean is based on
         */
        public synchronized int getSamples() {
            return durations.size();
        }

        public String toString() {
            return "Mean[sum=" + sum +",samples=" +durations.size() + "mean=" + getMean() +"]";
        }
    }

    /**
     * Service call key for hashmap.
     */
    private static class ServiceCall {
        private String className;
        private String methodName;
        private String[] parameterTypeNames;

        private ServiceCall(final Object service, final String methodName, final Class[] parameterTypes) {
            this.className = service.getClass().getName();
            this.methodName = methodName;
            this.parameterTypeNames = new String[parameterTypes.length];
            for (int i=0; i<parameterTypes.length; i++) {
                this.parameterTypeNames[i] = parameterTypes.getClass().getName();
            }
        }

        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final ServiceCall that = (ServiceCall) o;

            if (!className.equals(that.className)) return false;
            if (!methodName.equals(that.methodName)) return false;
            if (!Arrays.equals(parameterTypeNames, that.parameterTypeNames)) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = className.hashCode();
            result = 31 * result + methodName.hashCode();
            result = 31 * result + Arrays.hashCode(parameterTypeNames);
            return result;
        }

        public String toString() {
            return "ServiceCall[" + className + "#" + methodName + "(" + Arrays.asList(parameterTypeNames) + ")]";
        }
    }

}
