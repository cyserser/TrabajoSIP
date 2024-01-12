package sipServlet;

import proxy.ProxyUserLayer;

public class SipServletResponse implements SipServletResponseInterface{

	
	
	@Override
	public void send() {
		// TODO Auto-generated method stub
		ProxyUserLayer.denyCall = true;
	}

}
