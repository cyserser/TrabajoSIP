package proxy;

public class ProxyWhiteList {
	
	private String userURI;
	private int userPort;
	private String userAddress;

	public ProxyWhiteList(String userURI, String userAddress, int userPort) {
		super();
		this.userURI = userURI;
		this.userAddress = userAddress;
		this.userPort = userPort;
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
	public String getUserAddress() {
		return userAddress;
	}
	public void setUserAddress(String address) {
		this.userAddress = address;
	}

	public int getUserPort() {
		return userPort;
	}

	public void setUserPort(int listenPort) {
		this.userPort = listenPort;
	}

}
