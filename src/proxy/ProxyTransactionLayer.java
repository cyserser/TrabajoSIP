package proxy;

import java.io.IOException;
import java.net.SocketException;

import mensajesSIP.InviteMessage;
import mensajesSIP.RegisterMessage;
import mensajesSIP.OKMessage;
import mensajesSIP.SIPMessage;
import mensajesSIP.NotFoundMessage;

public class ProxyTransactionLayer {
	private static final int IDLE = 0;
	private static final int REGISTERING = 1;
	private int state = REGISTERING;

	private ProxyUserLayer userLayer;
	private ProxyTransportLayer transportLayer;

	public ProxyTransactionLayer(int listenPort, ProxyUserLayer userLayer) throws SocketException {
		this.userLayer = userLayer;
		this.transportLayer = new ProxyTransportLayer(listenPort, this);
	}

	public void onMessageReceived(SIPMessage sipMessage) throws IOException {
		if (sipMessage instanceof InviteMessage) {
			state = IDLE;
			InviteMessage inviteMessage = (InviteMessage) sipMessage;
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
		} else {
			System.err.println("Unexpected message, throwing away (REST)");
		}
	}

	public void echoInvite(InviteMessage inviteMessage, String address, int port) throws IOException {
		transportLayer.send(inviteMessage, address, port);
	}
	public void echoRegister(RegisterMessage registerMessage, String address, int port) throws IOException {
		transportLayer.send(registerMessage, address, port);
	}
	
	// mensaje 200 OK
	public void echoOK(OKMessage okMessage, String address, int port) throws IOException {
		transportLayer.send(okMessage, address, port);
	}
	
	// mensaje 404 not found
	public void echoNotfound(NotFoundMessage notFoundMessage, String address, int port) throws IOException {
		transportLayer.send(notFoundMessage, address, port);
	}

	public void startListening() {
		transportLayer.startListening();
	}
}
