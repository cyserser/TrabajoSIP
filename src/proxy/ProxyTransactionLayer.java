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
	//MAL
	//private static final int REGISTERING = 1;
	//private int state = REGISTERING;
	
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
	
	private int stateA;
	private int stateB;

	private ProxyUserLayer userLayer;
	private ProxyTransportLayer transportLayer;

	public ProxyTransactionLayer(int listenPort, ProxyUserLayer userLayer) throws SocketException {
		this.userLayer = userLayer;
		this.transportLayer = new ProxyTransportLayer(listenPort, this);
	}

	public void onMessageReceived(SIPMessage sipMessage) throws IOException {
		if (sipMessage instanceof InviteMessage) {
			stateA = CALLING;
			InviteMessage inviteMessage = (InviteMessage) sipMessage;
			userLayer.onInviteReceived(inviteMessage);
		}
		
		else if (sipMessage instanceof RegisterMessage) {
			RegisterMessage registerMessage = (RegisterMessage) sipMessage;
			userLayer.onRegisterReceived(registerMessage);
			
		} 
		else if (sipMessage instanceof RingingMessage) {
			RingingMessage ringingMessage = (RingingMessage) sipMessage;
			stateB = PROCEEDING_B;
			userLayer.onRingingReceived(ringingMessage);
		}
		else if (sipMessage instanceof OKMessage) {
			OKMessage okMessage = (OKMessage) sipMessage;
			stateB = TERMINATED_B;
			userLayer.onOKReceived(okMessage);
		}
		else if (sipMessage instanceof BusyHereMessage) {
			BusyHereMessage busyHereMessage = (BusyHereMessage) sipMessage;
			stateB = COMPLETED_B;
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
