package sipServlet;

import java.util.Calendar;

public class AliceSIPServlet implements SIPServletInterface{

	@Override
	public void doInvite(SipServletRequestInterface request) {
		// TODO Auto-generated method stub
		int current_hour;
		Calendar cal = Calendar.getInstance();
		current_hour = cal.get(Calendar.HOUR_OF_DAY);
		if (current_hour>0 && current_hour<8) {
			request.getProxy().proxyTo(request.getCalleeURI());
		}
		else if (current_hour>8 && current_hour<10) {
			request.getProxy().proxyTo("sip:juan@SMA");
		}
		else {
			request.createResponse(503).send();
		}
		
	}

}
