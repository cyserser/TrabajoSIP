package proxy;

import java.io.IOException;
import java.net.SocketException;

import mensajesSIP.InviteMessage;
import mensajesSIP.RegisterMessage;
import mensajesSIP.RingingMessage;
import mensajesSIP.OKMessage;
import mensajesSIP.SIPMessage;
import mensajesSIP.NotFoundMessage;
import mensajesSIP.TryingMessage;
import mensajesSIP.BusyHereMessage;

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
		} 
		else if (sipMessage instanceof RingingMessage) {
			RingingMessage ringingMessage = (RingingMessage) sipMessage;
			userLayer.onRingingReceived(ringingMessage);
		}
		else if (sipMessage instanceof OKMessage) {
			OKMessage okMessage = (OKMessage) sipMessage;
			userLayer.onOKReceived(okMessage);
		}
		else if (sipMessage instanceof BusyHereMessage) {
			BusyHereMessage busyHereMessage = (BusyHereMessage) sipMessage;
			userLayer.onBusyHereReceived(busyHereMessage);;
		}
		else {
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
	
	// mensaje 100 trying
	public void echoTrying(TryingMessage tryingMessage, String address, int port) throws IOException {
		transportLayer.send(tryingMessage, address, port);
	}

	// mensaje 180 ringing
	public void echoRinging(RingingMessage ringingMessage, String address, int port) throws IOException {
		transportLayer.send(ringingMessage, address, port);
	}
	
	// mensaje 486 busy here
	public void echoBusyHere(BusyHereMessage busyHereMessage, String address, int port) throws IOException {
		transportLayer.send(busyHereMessage, address, port);
	}
			
		
	public void startListening() {
		transportLayer.startListening();
	}
}
