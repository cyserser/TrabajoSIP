package ua;

import java.io.IOException;
import java.net.SocketException;

import mensajesSIP.InviteMessage;
import mensajesSIP.RegisterMessage;
import mensajesSIP.OKMessage;
import mensajesSIP.NotFoundMessage;
import mensajesSIP.SIPMessage;
import mensajesSIP.TryingMessage;

public class UaTransactionLayer {
	private static final int IDLE = 0;
	private static final int REGISTERING = 1;
	private static final int TRYING = 2;
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
				userLayer.onInviteReceived(inviteMessage);
				break;
			default:
				System.err.println("Unexpected message, throwing away (INVITE)");
				break;
			}
		} 
		else if (sipMessage instanceof RegisterMessage) {
			RegisterMessage registerMessage = (RegisterMessage) sipMessage;
			switch (state) {
			case REGISTERING:
				userLayer.onRegisterReceived(registerMessage);
				break;
			default:
				System.err.println("Unexpected message, throwing away (REGISTER)");
				break;
			}
		} 
		
		// 100 trying
		else if (sipMessage instanceof TryingMessage) {
			TryingMessage tryingMessage = (TryingMessage) sipMessage;
			if(tryingMessage != null)
			userLayer.onTryingReceived(tryingMessage);
		}
		
		// 200 ok
		else if(sipMessage instanceof OKMessage) {
			OKMessage okMessage = (OKMessage) sipMessage;
			if(okMessage != null)
			userLayer.onOKReceived(okMessage);
		}
		
		// 404 not found
		else if(sipMessage instanceof NotFoundMessage) {
			NotFoundMessage notFoundMessage = (NotFoundMessage) sipMessage;
			if(notFoundMessage != null)
			userLayer.onNotFoundReceived(notFoundMessage);
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
	public void callR(RegisterMessage registerMessage) throws IOException {
		transportLayer.sendToProxy(registerMessage);
	}
}
