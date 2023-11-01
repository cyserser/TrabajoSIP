package proxy;

public class ProxyWhiteList {
	
	private String userURI;
	private int listenPort;
	private String proxyAddress;
	private int proxyPort;

	public ProxyWhiteList(String userURI, String proxyAddress, int listenPort, int proxyPort) {
		super();
		this.userURI = userURI;
		this.proxyAddress = proxyAddress;
		this.listenPort = listenPort;
		this.proxyPort = proxyPort;
	}
	
	public ProxyWhiteList(String userURI) {
		super();
		this.userURI = userURI;
	}
	
	public String getUserURI() {
		return userURI;
	}
	public void setName(String userURI) {
		this.userURI = userURI;
	}
	public String getProxyAddress() {
		return proxyAddress;
	}
	public void setProxyAddress(String address) {
		this.proxyAddress = address;
	}

	public int getListenPort() {
		return listenPort;
	}

	public void setListenPort(int listenPort) {
		this.listenPort = listenPort;
	}

	public int getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}
}
