package sipServlet;

import java.util.Calendar;

public class BobSIPServlet implements SIPServletInterface{

	@Override
	public void doInvite(SipServletRequestInterface request) {
		// TODO Auto-generated method stub
		int current_hour;
		Calendar cal = Calendar.getInstance();
		current_hour = cal.get(Calendar.HOUR_OF_DAY);
		if (current_hour>10 && current_hour<22) {
			request.getProxy().proxyTo(request.getCalleeURI());
		} 
		else {
			request.createResponse(503).send();
		}
	}
}
