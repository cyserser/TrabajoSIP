package ua;

import java.io.IOException;
import java.net.SocketException;

import mensajesSIP.InviteMessage;
import mensajesSIP.RegisterMessage;
import mensajesSIP.RingingMessage;
import mensajesSIP.OKMessage;
import mensajesSIP.NotFoundMessage;
import mensajesSIP.SIPMessage;
import mensajesSIP.TryingMessage;
import mensajesSIP.RequestTimeoutMessage;
import mensajesSIP.BusyHereMessage;

public class UaTransactionLayer {
	/// MAL
	private static final int REGISTERING = 1;
	private static final int TRYING = 2;
	
	//Estados llamante
	private static final int IDLE = 0;
	private static final int CALLING = 1;
	private static final int PROCEEDING_A = 2;
	private static final int COMPLETED_A = 3;
	private static final int TERMINATED_A = 4;
	
	//Estados llamado
	private static final int PROCEEDING_B = 5;
	private static final int COMPLETED_B = 6;
	private static final int TERMINATED_B = 7;
	
	private int state;

	private UaUserLayer userLayer;
	private UaTransportLayer transportLayer;

	public UaTransactionLayer(int listenPort, String proxyAddress, int proxyPort, UaUserLayer userLayer)
			throws SocketException {
		this.userLayer = userLayer;
		this.transportLayer = new UaTransportLayer(listenPort, proxyAddress, proxyPort, this);
	}

	public void onMessageReceived(SIPMessage sipMessage) throws IOException {
		if (sipMessage instanceof InviteMessage) {
			InviteMessage inviteMessage = (InviteMessage) sipMessage;
			//String estado = inviteMessage.getcSeqStr();
			switch (state) {
			case IDLE:
				state = PROCEEDING_B;
				userLayer.onInviteReceived(inviteMessage);
				break;
			default:
				System.err.println("Unexpected message, throwing away (INVITE)");
				break;
			}
		} 
		/*else if (sipMessage instanceof RegisterMessage) {
			RegisterMessage registerMessage = (RegisterMessage) sipMessage;
			switch (state) {
			case IDLE:
				userLayer.onRegisterReceived(registerMessage);
				break;
			default:
				System.err.println("Unexpected message, throwing away (REGISTER)");
				break;
			}
		} */
		
		// 100 trying
		else if (sipMessage instanceof TryingMessage) {
			TryingMessage tryingMessage = (TryingMessage) sipMessage;
			if(tryingMessage != null)
			{
				state = PROCEEDING_A;
				userLayer.onTryingReceived(tryingMessage);
			}
			
		}
		
		// 200 ok
		else if(sipMessage instanceof OKMessage) {
			OKMessage okMessage = (OKMessage) sipMessage;
			if(okMessage != null)
			{
				// LLAMANTE
				if(state == CALLING || state == PROCEEDING_A) {
					state = TERMINATED_A;
				} 	
			}	
			userLayer.onOKReceived(okMessage);
		}
		
		// 404 not found
		else if(sipMessage instanceof NotFoundMessage) {
			NotFoundMessage notFoundMessage = (NotFoundMessage) sipMessage;
			if(notFoundMessage != null)
			{
				state = COMPLETED_A;
				userLayer.onNotFoundReceived(notFoundMessage);
			}
			
		}
		
		// 180 ringing
		else if (sipMessage instanceof RingingMessage) {
			RingingMessage ringingMessage = (RingingMessage) sipMessage;
			if(ringingMessage != null)
			{
				state = PROCEEDING_A;
				userLayer.onRingingReceived(ringingMessage);
			}
			
		}
		
		// 486 busy
		else if (sipMessage instanceof BusyHereMessage) {
			BusyHereMessage busyMessage = (BusyHereMessage) sipMessage;
			if(busyMessage != null)
			{
				state = COMPLETED_A;
				userLayer.onBusyHereReceived(busyMessage);
			}
		}
		
		else {
			System.err.println("Unexpected message, throwing away (RESTO)");
		}
	}

	public void startListeningNetwork() {
		transportLayer.startListening();
	}

	public void call(InviteMessage inviteMessage) throws IOException {
		transportLayer.sendToProxy(inviteMessage);
	}
	public void callRegister(RegisterMessage registerMessage) throws IOException {
		transportLayer.sendToProxy(registerMessage);
	}
	public void callRinging(RingingMessage ringingMessage) throws IOException {
		transportLayer.sendToProxy(ringingMessage);
	}
	
	// MENSAJES QUE ENVIAMOS
	
	// 200 OK que enviamos (BOB)
	public void callOK(OKMessage okMessage) throws IOException {
		transportLayer.sendToProxy(okMessage);
	}
	
	// 486 Busy here (BOB)
	public void callBusyHere(BusyHereMessage busyHereMessage) throws IOException {
		transportLayer.sendToProxy(busyHereMessage);
	}
}
