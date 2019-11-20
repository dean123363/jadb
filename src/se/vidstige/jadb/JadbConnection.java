package se.vidstige.jadb;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class JadbConnection implements ITransportFactory {

    private final String host;
    private final int port;

    private static final int DEFAULTPORT = 5037;

    private final int timeout;
    
    public JadbConnection() {
        this("localhost", DEFAULTPORT, 0);
    }

    public JadbConnection(String host, int port) {
        this(host, port, 0);
    }

    public JadbConnection(String host, int port, int timeout) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
    }
    
    public Transport createTransport() throws IOException {
        return createTransport(this.timeout);
    }

    public Transport createTransport(int timeout) throws IOException {
    	Socket socket = new Socket(host, port);
    	if (timeout > 0)
    		socket.setSoTimeout(timeout);
        return new Transport(socket);
    }
    
    public String getHostVersion() throws IOException, JadbException {
        try (Transport transport = createTransport()) {
            transport.send("host:version");
            transport.verifyResponse();
            return transport.readString();
        }
    }

    public InetSocketAddress connectToTcpDevice(InetSocketAddress inetSocketAddress)
            throws IOException, JadbException, ConnectionToRemoteDeviceException {
        try (Transport transport = createTransport()) {
            return new HostConnectToRemoteTcpDevice(transport).connect(inetSocketAddress);
        }
    }

    public InetSocketAddress disconnectFromTcpDevice(InetSocketAddress tcpAddressEntity)
            throws IOException, JadbException, ConnectionToRemoteDeviceException {
        try (Transport transport = createTransport()) {
            return new HostDisconnectFromRemoteTcpDevice(transport).disconnect(tcpAddressEntity);
        }
    }

    public List<JadbDevice> getDevices() throws IOException, JadbException {
        try (Transport transport = createTransport()) {
            transport.send("host:devices");
            transport.verifyResponse();
            String body = transport.readString();
            return parseDevices(body);
        }
    }

    public DeviceWatcher createDeviceWatcher(DeviceDetectionListener listener) throws IOException, JadbException {
        Transport transport = createTransport();
        transport.send("host:track-devices");
        transport.verifyResponse();
        return new DeviceWatcher(transport, listener, this);
    }

    public List<JadbDevice> parseDevices(String body) {
        String[] lines = body.split("\n");
        ArrayList<JadbDevice> devices = new ArrayList<>(lines.length);
        for (String line : lines) {
            String[] parts = line.split("\t");
            if (parts.length > 1) {
                devices.add(new JadbDevice(parts[0], this)); // parts[1] is type
            }
        }
        return devices;
    }

    public JadbDevice getAnyDevice() {
        return JadbDevice.createAny(this);
    }
}
