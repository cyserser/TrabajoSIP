package proxy;

public class ProxyWhiteList {
	
	private String userURI;
	private int userPort;
	private String userAddress;
	private String userName;
	private boolean bIsRegistered;
	private boolean bACKReceived;

	public ProxyWhiteList(String userURI, String userAddress, int userPort, 
			String userName, boolean bIsRegistered, boolean bACKReceived) {
		super();
		this.userURI = userURI;
		this.userAddress = userAddress;
		this.userPort = userPort;
		this.userName = userName;
		this.setIsRegistered(bIsRegistered);
		this.setbACKReceived(bACKReceived);
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

	public boolean getIsRegistered() {
		return bIsRegistered;
	}

	public void setIsRegistered(boolean bIsRegistered) {
		this.bIsRegistered = bIsRegistered;
	}

	public boolean isbACKReceived() {
		return bACKReceived;
	}

	public void setbACKReceived(boolean bACKReceived) {
		this.bACKReceived = bACKReceived;
	}

}
