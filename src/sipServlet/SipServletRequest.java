package sipServlet;

import mensajesSIP.InviteMessage;

public class SipServletRequest implements SipServletRequestInterface{

	InviteMessage inviteMessage;
	
	public SipServletRequest(InviteMessage inviteMessage) {
		this.inviteMessage = inviteMessage;
	}
	
	@Override
	public String getCallerURI() {
		// TODO Auto-generated method stub
		return inviteMessage.getFromUri();
	}

	@Override
	public String getCalleeURI() {
		// TODO Auto-generated method stub
		return inviteMessage.getToUri();
	}

	@Override
	public SipServletResponseInterface createResponse(int statuscode) {
		// TODO Auto-generated method stub
		SipServletResponse sipServletResponse = new SipServletResponse();
		return sipServletResponse;
	}

	@Override
	public ProxyInterface getProxy() {
		// TODO Auto-generated method stub
		ProxyImpl proxy = new ProxyImpl();
		return proxy;
	}

}
