package proxy;

import java.util.ArrayList;

public class ProxyWhiteListArray {
	
	private ArrayList<ProxyWhiteList> whiteList;

	public ProxyWhiteListArray() {
		super();
		
		if(this.whiteList == null) this.whiteList = new ArrayList<>();
		
		addToWhitelist();
	}

	public ArrayList<ProxyWhiteList> getWhiteList() {
		return whiteList;
	}

	public void setWhiteList(ArrayList<ProxyWhiteList> whiteList) {
		this.whiteList = whiteList;
	}
	
	private void addToWhitelist()
	{
		ProxyWhiteList whiteListAlice = new ProxyWhiteList("alice@SMA");
		ProxyWhiteList whiteListBob = new ProxyWhiteList("bob@SMA");
		ProxyWhiteList whiteListJuan = new ProxyWhiteList("juan@SMA");
		
		this.whiteList.add(whiteListAlice);
		this.whiteList.add(whiteListBob);
		this.whiteList.add(whiteListJuan);
	}
	public void insertToWhiteList(int port, String direccion)
	{
		//ProxyWhiteList whiteListNew = new ProxyWhiteList("alice@SMA", "loqueseea", 2);
		//this.whiteList.add(whiteListNew);
	}
}
