package sipServlet;

import proxy.ProxyUserLayer;

public class ProxyImpl implements ProxyInterface{

	@Override
	public void proxyTo(String uri) {
		// TODO Auto-generated method stub
		ProxyUserLayer.servletToInvite = uri;
		
	}

}
