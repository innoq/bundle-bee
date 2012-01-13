package org.bundlebee.weaver;

import org.bundlebee.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author joergp
 */
public class DispatchStrategyFactory
{
	private static Logger LOG = LoggerFactory.getLogger(DispatchStrategyFactory.class);

	//
	// classname from properties
	//
    private static final String SERVICE_CALL_DISPATCH_STRATEGY_PROPERTY_KEY = "org.bundlebee.weaver.servicecalldispatchstrategy";
    private static final String SERVICE_CALL_DISPATCH_STRATEGY_CLASS
									= System.getProperty(SERVICE_CALL_DISPATCH_STRATEGY_PROPERTY_KEY, BundleStateDispatchStrategy.class.getName());


	/**
	 * Create and configure a {@link ServiceCallDispatchStrategy}.
	 * Trys to guess a classname from the system property org.bundlebee.weaver.servicecalldispatchstrategy
	 * and falls back to {@link BundleStateDispatchStrategy} if that fails.
	 * 
	 * @param reg
	 * @param stats
	 * @return new strategy
	 */
	static ServiceCallDispatchStrategy create(Registry reg, ServiceCallStats stats) {
		ServiceCallDispatchStrategy strat;

		//
		// try to create one from the configured name
		//
		try {
			strat = (ServiceCallDispatchStrategy) Class.forName(SERVICE_CALL_DISPATCH_STRATEGY_CLASS).newInstance();
		} catch (Exception e) {
			LOG.error(e.toString(), e);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Using " + BundleStateDispatchStrategy.class.getName());
			}
			strat = new BundleStateDispatchStrategy();
		}

		//
		// configure strategy
		//
		strat.setRegistry(reg);
		strat.setServiceCallStats(stats);

		return strat;
	}
}
