package com.ftphelper;

/**
 * Connection parameters for FTP or SFTP sessions.
 */
public class ConnectionDetails {

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final Protocol protocol;

    /**
     * Full constructor with explicit port.
     */
    public ConnectionDetails(String host, int port, String username, String password, Protocol protocol) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.protocol = protocol;
    }

    /**
     * Constructor using default ports: SFTP=22, FTP=21.
     */
    public ConnectionDetails(String host, String username, String password, Protocol protocol) {
        this(host, protocol == Protocol.SFTP ? 22 : 21, username, password, protocol);
    }

    public String getHost()       { return host; }
    public int getPort()          { return port; }
    public String getUsername()   { return username; }
    public String getPassword()   { return password; }
    public Protocol getProtocol() { return protocol; }

    @Override
    public String toString() {
        return protocol + "://" + username + "@" + host + ":" + port;
    }
}
