package org.bundlebee.registry.net;

import org.bundlebee.registry.impl.RegistryImpl;
import static org.bundlebee.registry.impl.RegistryImpl.BUNDLE_MARKER;
import static org.bundlebee.registry.net.MultiCastMessage.Direction;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents the a node in the network and manages broadcast and private communication
 * between nodes.
 *
 * @author <a href="mailto:philipp.haussleiter@innoq.com">Philipp Haussleiter</a>
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class MultiCastNode implements MultiCastMessageListener, MultiCastMessageSource {

    private static org.slf4j.Logger		LOG									= LoggerFactory.getLogger(MultiCastNode.class);
    private static final int			BUFFER_SIZE							= 1024; // max size of a message
    protected static final int			MAX_PORT_RANGE			= 100;
    private static final String			MESSAGE_CHAR_ENCODING				= "UTF-8";
    private static final int			DEFAULT_GROUP_PORT					= 5555;
    protected static final int			DEFAULT_LOCAL_PORT					= 5556;
    private static final String			ORG_BUNDLEBEE_REGISTRY_GROUPPORT	= "org.bundlebee.registry.groupport";
    private static final String			ORG_BUNDLEBEE_REGISTRY_GROUPADDRESS = "org.bundlebee.registry.groupaddress";
    protected static final String		ORG_BUNDLEBEE_REGISTRY_LOCALADDRESS = "org.bundlebee.registry.localaddress";
    private static final String			ORG_BUNDLEBEE_REGISTRY_LOCALPORT	= "org.bundlebee.registry.localport";
    private static final int			GROUP_PORT							= getIntSystemProperty(DEFAULT_GROUP_PORT, ORG_BUNDLEBEE_REGISTRY_GROUPPORT);
    protected static final int			LOCAL_PORT							= getIntSystemProperty(DEFAULT_LOCAL_PORT, ORG_BUNDLEBEE_REGISTRY_LOCALPORT);
    private static final byte[]			DEFAULT_GROUP_HOST					= new byte[]{(byte) 228, (byte) 5, (byte) 6, (byte) 7};
    private static final byte[]			GROUP_ADDRESS						= getFourByteSystemProperty(DEFAULT_GROUP_HOST, ORG_BUNDLEBEE_REGISTRY_GROUPADDRESS);
    private static final int			DEFAULT_MAX_LS_REQUESTS				= 3;

    private static byte[] getFourByteSystemProperty(final byte[] defaultValue, final String key) {
        final String address = System.getProperty(key);
        if (address == null) {
            return defaultValue;
        }
        try {
            final String[] byteStrings = address.split("\\.");
            if (byteStrings.length != 4) {
                return defaultValue;
            }
            final byte[] bytes = new byte[4];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) Integer.parseInt(byteStrings[i]);
            }
            return bytes;
        } catch (NumberFormatException e) {
            LOG.error(BUNDLE_MARKER, e.toString(), e);
        }
        return defaultValue;
    }

    private static int getIntSystemProperty(final int defaultValue, final String key) {
        try {
            return Integer.parseInt(System.getProperty(key, "" + defaultValue));
        } catch (NumberFormatException e) {
            LOG.error(BUNDLE_MARKER, e.toString(), e);
        }
        return defaultValue;
    }
    private DatagramSocket	privateSocket;
    private MulticastSocket groupSocket;
    private InetAddress		groupAddress;
    private long			nodeId;
    private boolean			running;
    private Set<MultiCastMessageListener> multiCastMessageListeners = new HashSet<MultiCastMessageListener>();
    private int				lsRequestSent;
    private int				maxLsRequests = DEFAULT_MAX_LS_REQUESTS;
    private String			localAddress = System.getProperty(ORG_BUNDLEBEE_REGISTRY_LOCALADDRESS, InetAddress.getLocalHost().getHostAddress());

    public MultiCastNode(final long nodeId) throws IOException {
        try {
            this.nodeId = nodeId;
            this.groupAddress = InetAddress.getByAddress(GROUP_ADDRESS);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Initializing MultiCastNode for " + nodeId + " and group address " + groupAddress + ".");
            }
            setRunning(true);
            makePrivateSocket();
            startPrivateListener();
            joinGroup();
            startGroupListener();
            final MultiCastMessage hiMessage = new MultiCastMessage(nodeId, MultiCastMessage.Type.HI, getSender());
            sendToGroup(hiMessage);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Done initializing MultiCastNode for " + this.nodeId);
            }
        } catch (IOException e) {
            setRunning(false);
            throw e;
        }
    }

    /**
     * Add a listener if you want to be notified of registry related messages.
     *
     * @param multiCastMessageListener listener
     */
    public void addMultiCastMessageListener(final MultiCastMessageListener multiCastMessageListener) {
        multiCastMessageListeners.add(multiCastMessageListener);
    }

    /**
     * Is notified of multicast messages. These messages are sent to the group, if their direction
     * is {@link org.bundlebee.registry.net.MultiCastMessage.Direction#OUT}.
     *
     * @param multiCastMessage multiCastMessage
     */
    public void processMessage(final MultiCastMessage multiCastMessage) {
        if (Direction.OUT.equals(multiCastMessage.getDirection())) {
            this.sendToGroup(multiCastMessage);
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Not sending message, because its direction is not " + Direction.OUT
                        + ", but " + multiCastMessage.getDirection());
            }
        }
    }

    /**
     * Fires message to all registered listeners.
     *
     * @param multiCastMessage message
     */
    private void fireMessage(final MultiCastMessage multiCastMessage) {
        for (final MultiCastMessageListener multiCastMessageListener : multiCastMessageListeners) {
            multiCastMessageListener.processMessage(multiCastMessage);
        }
        if (multiCastMessageListeners.isEmpty() && LOG.isDebugEnabled()) {
            LOG.debug(BUNDLE_MARKER, "No MultiCastMessageListeners registered.");
        }
    }

    /**
     * Creates the socket used for private point to point communication between two nodes.
     *
     * @throws SocketException if the socket can't be created
     */
    private void makePrivateSocket() throws SocketException {
        this.privateSocket = getAvailableSocket(LOCAL_PORT);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Bound private socket to port " + privateSocket.getLocalPort());
        }
    }


	/**
	 * for test purposes
	 * @return
	 */
	int getPort() {
		return privateSocket.getLocalPort();
	}

    /**
     * Receive loop for private messaqes.
     * Can be stopped with {@link #setRunning(boolean)}
     */
    private void receivePrivateMessages() {
        while (isRunning()) {
            try {
                final byte[] buf = new byte[BUFFER_SIZE];
                final DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Waiting for private message, listening on port " + privateSocket.getLocalPort());
                }
                privateSocket.receive(receivePacket); // wait for a packet
                final String data = toString(receivePacket);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Got a private message: " + data);
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Received " + data + " from " + receivePacket.getAddress()
                            + ":" + receivePacket.getPort() + ".");
                }
                processPrivateMessage(data);
            } catch (IOException e) {
                LOG.error(BUNDLE_MARKER, e.toString(), e);
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Stopped listening to private messages.");
        }
    }

    /**
     * Processes a privately received message.
     *
     * @param message message
     * @throws IOException if a response request fails
     */
    private void processPrivateMessage(final String message) throws IOException {
        try {
            final MultiCastMessage multiCastMessage = new MultiCastMessage(message);
            if (multiCastMessage.getType() == MultiCastMessage.Type.LS) {
                final InetAddress address = multiCastMessage.getSenderAddress();
                final int port = multiCastMessage.getSenderPort();
                // sending everything to remote private port
                for (final MultiCastMessage mcm : ((RegistryImpl) RegistryImpl.getInstance()).getList()) {
                    sendPrivateMessage(mcm, address, port);
                }
            } else if (multiCastMessage.getType() == MultiCastMessage.Type.HI) {
                final InetAddress address = multiCastMessage.getSenderAddress();
                final int port = multiCastMessage.getSenderPort();
                if (multiCastMessage.getNodeId() == RegistryImpl.getInstance().getNodeId()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("HI message is from this node, doing nothing.");
                    }
                } else {
                    if (lsRequestSent < maxLsRequests) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Sending ls request to " + address);
                        }
                        sendPrivateMessage(new MultiCastMessage(nodeId, MultiCastMessage.Type.LS, getSender()), address, port);
                        lsRequestSent++;
                    } else {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Max ls requests already sent, doing nothing.");
                        }
                    }
                }
            } else {
                if (multiCastMessage.isValid()) {
                    multiCastMessage.setDirection(Direction.IN);
                    fireMessage(multiCastMessage);
                }
            }
        } catch (RuntimeException e) {
            LOG.error(BUNDLE_MARKER, "Failed to process private message: " + e.toString(), e);
        }
    }

    /**
     * Sends a private message.
     *
     * @param message message
     * @param address address to send the message to
     * @param port port to send the message to
     * @throws IOException if sending fails
     */
    private void sendPrivateMessage(final MultiCastMessage message, final InetAddress address, final int port) throws IOException {
        privateSocket.send(toDatagramPacket(message.toString(), address, port));
        if (LOG.isDebugEnabled()) {
            LOG.debug("Sent " + message + " to " + address + ":" + port);
        }
    }

    /**
     * Join the broadcast group.
     *
     * @throws IOException if something goes wrong
     */
    private void joinGroup() throws IOException {
        groupSocket = new MulticastSocket(GROUP_PORT);
        groupSocket.joinGroup(groupAddress);
        if (LOG.isDebugEnabled()) {
            LOG.debug("MultiCastNode for " + nodeId
                    + " joined group at " + groupAddress + ":" + GROUP_PORT);
        }
    }

    public void sendToGroup(final MultiCastMessage multiCastMessage) {
        if (multiCastMessage.isValid()) {
            // reset ls request count, when we send HI
            if (multiCastMessage.getType() == MultiCastMessage.Type.HI) {
                lsRequestSent = 0;
                // make sure we have a sender set
                // even if the directory does not know, who we are
                if (multiCastMessage.getValues().getProperty(MultiCastMessage.PROPERTYKEY_SENDER) == null) {
                    multiCastMessage.getValues().setProperty(MultiCastMessage.PROPERTYKEY_SENDER, getSender());
                }
            }
            sendToGroup(multiCastMessage.toString());
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug(BUNDLE_MARKER, "not sending message, because it is not valid: " + multiCastMessage);
            }
        }
    }

    /**
     * Send a packet to the multicast group. Messages have the form (name): msg
     *
     * @param message message
     */
    public void sendToGroup(final String message) {
        try {
            if (LOG.isInfoEnabled()) {
                LOG.info(BUNDLE_MARKER, "Sending packet to group: " + message);
            }
            final DatagramPacket sendPacket = toDatagramPacket(message, groupAddress, GROUP_PORT);
            groupSocket.send(sendPacket);
            if (LOG.isInfoEnabled()) {
                LOG.info(BUNDLE_MARKER, "Packet sent to group: " + message);
            }
        } catch (IOException ioe) {
            LOG.error(BUNDLE_MARKER, "Failed to send packet: " + ioe.toString(), ioe);
        }
    }

    /**
     * Repeatedly receive a packet from the group, process it. No messages are
     * sent from here.
     * sendPacket().
     */
    public void receiveGroupMessages() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Starting group receive loop for " + nodeId);
        }
        while (this.isRunning()) {
            try {
                final byte[] data = new byte[BUFFER_SIZE]; // set up an empty packet
                final DatagramPacket packet = new DatagramPacket(data, data.length);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Waiting for group message on " + groupSocket);
                }
                groupSocket.receive(packet); // wait for a packet
                final String msg = toString(packet);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Received group msg " + msg + " from " + packet.getAddress() + ":" + packet.getPort() + ".");
                }
                processGroupMessage(msg);
            } catch (SocketTimeoutException e) {
                if (LOG.isInfoEnabled()) {
                    LOG.info(BUNDLE_MARKER, "GroupSocket receive timed out.");
                }
            } catch (IOException e) {
                LOG.error(BUNDLE_MARKER, e.toString(), e);
            } catch (Throwable t) {
                LOG.error(BUNDLE_MARKER, t.toString(), t);
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Group receive loop for " + nodeId + " was stopped.");
        }
    }

    /**
     * Processes a message.
     *
     * @param message message
     * @throws IOException if something goes wrong
     */
    private void processGroupMessage(final String message) throws IOException {
        try {
            final MultiCastMessage multiCastMessage = new MultiCastMessage(message);
            if (multiCastMessage.getType() == MultiCastMessage.Type.HI && multiCastMessage.getNodeId() != RegistryImpl.getInstance().getNodeId()) {
                final InetAddress address = multiCastMessage.getSenderAddress();
                final int port = multiCastMessage.getSenderPort();
                // we don't use datagramPacket.getAddress(), because of http://bugs.sun.com/view_bug.do?bug_id=4717228
                // -hendrik
                sendPrivateMessage(new MultiCastMessage(nodeId, MultiCastMessage.Type.HI, getSender()), address, port);
            }
            if (LOG.isInfoEnabled()) {
                LOG.info(BUNDLE_MARKER, "(NODE) processing node message: " + message);
            }
            if (multiCastMessage.isValid()) {
                multiCastMessage.setDirection(Direction.IN);
                fireMessage(multiCastMessage);
            }
        } catch (RuntimeException e) {
            LOG.error(BUNDLE_MARKER, "Failed to process group message: " + e.toString(), e);
        }
    }

    /**
     * Converts a datagram into a String.
     *
     * @param datagramPacket packet
     * @return string
     * @throws UnsupportedEncodingException if {@link #MESSAGE_CHAR_ENCODING} is not supported
     */
    private String toString(final DatagramPacket datagramPacket) throws UnsupportedEncodingException {
        return new String(datagramPacket.getData(), 0, datagramPacket.getLength(), MESSAGE_CHAR_ENCODING);
    }

    /**
     * Converts a string to a datagram packet.
     *
     * @param message string message
     * @param address address to send to
     * @param port port to send to
     * @return packet
     * @throws UnsupportedEncodingException if {@link #MESSAGE_CHAR_ENCODING} is not supported (impossible)
     */
    private DatagramPacket toDatagramPacket(final String message, final InetAddress address, final int port) throws UnsupportedEncodingException {
        final byte[] bytes = message.getBytes(MESSAGE_CHAR_ENCODING);
        return new DatagramPacket(bytes, bytes.length, address, port);
    }

    /**
     * A string describing the sender of a message, <code>address:port</code>
     *
     * @return sender
     */
    private String getSender() {
        return localAddress + ":" + privateSocket.getLocalPort();
    }

    /**
     * Starts the datagram listener for private point to point communication with other peers.
     */
    private void startPrivateListener() {
        new Thread(new Runnable() {

            public void run() {
                receivePrivateMessages();
            }
        }, "PrivateListener").start();
    }

    /**
     * Starts the datagram listener for group communication.
     */
    private void startGroupListener() {
        new Thread(new Runnable() {

            public void run() {
                receiveGroupMessages();
            }
        }, "MultiCastNode[" + nodeId + "] GroupReceive").start();
    }

    /**
     * Stops all listeners.
     */
    public void stop() {
        if (LOG.isInfoEnabled()) {
            LOG.info(BUNDLE_MARKER, "Shutting down node " + this.nodeId);
        }
        // not necessary - we remove the node on the directoty level
        //sendToGroup(BYE + " " + nodeId);
        setRunning(false);
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public synchronized void setRunning(final boolean running) {
        this.running = running;
    }

    /**
     * Checks if the LOCAL_PORT is availible. If not try another one.
     * @param startport Start port, try first to open.
     * @return a DatagramSocket for the Registry.
     */
	private static DatagramSocket getAvailableSocket(final int startport) throws SocketException {

		SocketException lastex = null;
		for( int port=startport; port<LOCAL_PORT + MAX_PORT_RANGE; port++) {

			try {
				DatagramSocket ds = new DatagramSocket(port);
				LOG.info("LOCAL_PORT=" + port);
				return ds;
			} catch (SocketException e) {
				lastex = e;
				LOG.info("LOCAL_PORT " + port + " already in use - trying " + (port + 1));
			}
		}
		throw lastex;

    }
}

