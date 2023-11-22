package proxy;

public class ProxyWhiteList {
	
	private String userURI;
	private int userPort;
	private String userAddress;
	private String userName;

	public ProxyWhiteList(String userURI, String userAddress, int userPort, String userName) {
		super();
		this.userURI = userURI;
		this.userAddress = userAddress;
		this.userPort = userPort;
		this.userName = userName;
	}
	
	public ProxyWhiteList(String userURI) {
		super();
		this.userURI = userURI;
	}
	
	public String getUserName()
	{
		return this.userName;
	}
	
	public void setUserName(String userName)
	{
		this.userName = userName;
	}
	
	public String getUserURI() {
		return userURI;
	}
	public void setUserURI(String userURI) {
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
