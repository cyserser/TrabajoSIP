package SIPServlet2;

public interface SipServletRequestInterface {
	public String getCallerURI();
	public String getCalleeURI();
	public SipServletResponseInterface createResponse(int statuscode);
	public ProxyInterface getProxy();
}
