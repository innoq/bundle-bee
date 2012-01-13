package org.bundlebee.registry.net;


import static org.bundlebee.registry.impl.RegistryImpl.BUNDLE_MARKER;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

/**
 * @author <a href="mailto:philipp.haussleiter@innoq.com">Philipp Haussleiter</a>
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class MultiCastMessage {

    private static org.slf4j.Logger LOG = LoggerFactory.getLogger(MultiCastMessage.class);

    public enum Operation {
        ADD, REMOVE, UDPATE
    }

    public enum Direction {
        IN, OUT
    }

    public enum Type {
        HI, LS, MCM, REPOSITORY, MANAGER, BUNDLE, MEMORY, NODE
    }

    public static final String PROPERTYKEY_TYPE = "type";
    public static final String PROPERTYKEY_URL = "url";
    public static final String PROPERTYKEY_NAME = "name";
    public static final String PROPERTYKEY_STATE = "state";
    public static final String PROPERTYKEY_SENDER = "sender";
    public static final String PROPERTYKEY_RECEIVER = "receiver";
    public static final String PROPERTYKEY_FREE_MEMORY = "free_memory";
    public static final String PROPERTYKEY_MAX_MEMORY = "max_memory";

	private Long nodeId;
	private String fallback = null;
	private Properties values;
	private Operation operation;
	private Direction direction;

    public MultiCastMessage() {
		this.nodeId = null;
		this.values = new Properties();
		this.values.setProperty(PROPERTYKEY_TYPE, "");
		this.operation = MultiCastMessage.Operation.ADD;
		this.direction = MultiCastMessage.Direction.OUT;
	}

	public MultiCastMessage(final String nodeId, final Properties values) {
        this(Long.parseLong(nodeId), values);
	}
    
    public MultiCastMessage(final long nodeId, final Properties values) {
        this.nodeId = nodeId;
        this.values = values;
        this.operation = MultiCastMessage.Operation.ADD;
        this.direction = MultiCastMessage.Direction.OUT;
    }

    public MultiCastMessage(final long nodeId, final Type type, final String address) {
        this.nodeId = nodeId;
        this.values = new Properties();
        this.values.put(PROPERTYKEY_TYPE, type.toString());
        if (address != null) this.values.put(PROPERTYKEY_SENDER, address);
        this.direction = MultiCastMessage.Direction.OUT;
        this.operation = MultiCastMessage.Operation.ADD;
    }

    /**
     * Fills an empty MCM with the given data (a serialized MCM as String).
     *
     * @param data data
     */
	public MultiCastMessage(final String data){
		this.fromString(data);
	}

    public void setNodeId(final Long nodeId) {
        this.nodeId = nodeId;
    }
    
    public Long getNodeId() {
		return nodeId;
	}

    public boolean hasNodeId() {
        return nodeId != null;
    }

    public void setValue(final String key, final String value){
		this.values.setProperty(key, value);
	}

    public Properties getValues() {
		return values;
	}

	public Operation getOperation(){
		return this.operation;
	}

	public void setOperation(final Operation operation){
		this.operation = operation;
	}

	public void setDirection(final Direction direction){
		this.direction = direction;
	}
	
	public Direction getDirection(){
		return this.direction;
	}

    public boolean hasURL() {
        return values.containsKey(PROPERTYKEY_URL);
    }

    public URL getURL() {
        URL url = null;
        if (hasURL()) {
            try {
                url = new URL(values.getProperty(PROPERTYKEY_URL));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        return url;
    }

    public boolean hasType() {
        return values.containsKey(PROPERTYKEY_TYPE);
    }

    /**
     * @return type or {@link Type#MCM} if none is set
     */
    public Type getType() {
        Type type = Type.MCM;
        if (hasType()) {
            try {
                type = Type.valueOf((String)values.get(PROPERTYKEY_TYPE));
            } catch (RuntimeException e) {
                LOG.debug(BUNDLE_MARKER, e.toString(), e);
            }
        }
        return type;
    }

    public InetAddress getSenderAddress() throws UnknownHostException {
        final String sender = (String)values.get(PROPERTYKEY_SENDER);
        if (sender == null) return null;
        return InetAddress.getByName(sender.substring(0, sender.indexOf(':')));
    }

    public int getSenderPort() {
        final String sender = values.getProperty(PROPERTYKEY_SENDER);
        if (sender == null) {
            LOG.debug("got port -1 for message " + this + ", == sender = " + sender);
            return -1;
        }
        return Integer.parseInt(sender.substring(sender.indexOf(':')+1));
    }

    public boolean hasName() {
        return values.containsKey(PROPERTYKEY_NAME);
    }

    public String getName() {
        return values.getProperty(PROPERTYKEY_NAME);
    }

    public boolean hasState() {
        return values.containsKey(PROPERTYKEY_STATE);
    }

    public Integer getState() {
        Integer state = null;
        final String stateString = values.getProperty(PROPERTYKEY_STATE);
        if (stateString != null) {
            try {
                state = Integer.parseInt(stateString);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return state;
    }

    public boolean hasFreeMemory() {
        return values.containsKey(PROPERTYKEY_FREE_MEMORY);
    }

    public Long getFreeMemory() {
        return getLong(PROPERTYKEY_FREE_MEMORY);
    }

    public boolean hasMaxMemory() {
        return values.containsKey(PROPERTYKEY_MAX_MEMORY);
    }

    public Long getMaxMemory() {
        return getLong(PROPERTYKEY_MAX_MEMORY);
    }

    private Long getLong(final String key) {
        Long l = null;
        final String lString = values.getProperty(key);
        if (lString != null) {
            try {
                l = Long.parseLong(lString);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return l;
    }

    public boolean hasSender() {
        return values.containsKey(PROPERTYKEY_SENDER);
    }

    @Override
    public String toString() {
		if (this.fallback != null) {
			return this.fallback;
		}
		final StringBuilder sb = new StringBuilder();
		sb.append(this.nodeId + ",");
		sb.append(this.operation + ",");
		sb.append(this.direction + ",");
		sb.append(this.values.toString());
		if (LOG.isInfoEnabled()) LOG.info(BUNDLE_MARKER, "toString() OUT.. ["+sb.toString()+"]");
		return sb.toString();
	}

	public void fromString(String s) {
		if (LOG.isInfoEnabled()) LOG.info(BUNDLE_MARKER, "toString() IN.. ["+s+"]");
		this.fallback = null;
		this.nodeId = null;
		this.operation = null;
		this.direction = null;
		this.values = new Properties();		
		s = s.replace("{", "");
		s = s.replace("}", "");
		final String[] sArray = s.split(",");
		int index = 0;
		if (sArray.length < 2) {
			if (LOG.isInfoEnabled()) LOG.info(BUNDLE_MARKER, "use fallback!");
			this.fallback = s;
		} else {
			this.nodeId = Long.parseLong(sArray[index++]);
            this.operation = Operation.valueOf(sArray[index++]);
            this.direction = Direction.valueOf(sArray[index++]);
			String[] sProp;
			for (int i = index; i < sArray.length; i++) {
				sProp = sArray[i].split("=");
				if(sProp.length > 1)
				this.values.put(sProp[0].trim(), sProp[1].trim());
			}
			this.toString();
		}
	}

	public boolean isValid() {
        return this.fallback == null;
    }

}
