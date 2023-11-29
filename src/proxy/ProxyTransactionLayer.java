package proxy;

import java.io.IOException;
import java.net.SocketException;

import mensajesSIP.InviteMessage;
import mensajesSIP.RegisterMessage;
import mensajesSIP.RequestTimeoutMessage;
import mensajesSIP.RingingMessage;
import mensajesSIP.OKMessage;
import mensajesSIP.SIPMessage;
import mensajesSIP.NotFoundMessage;
import mensajesSIP.TryingMessage;
import mensajesSIP.ACKMessage;
import mensajesSIP.BusyHereMessage;
import mensajesSIP.ByeMessage;
import mensajesSIP.ServiceUnavailableMessage;

public class ProxyTransactionLayer {
	
	private ProxyUserLayer userLayer;
	private ProxyTransportLayer transportLayer;

	public ProxyTransactionLayer(int listenPort, ProxyUserLayer userLayer) throws SocketException {
		this.userLayer = userLayer;
		this.transportLayer = new ProxyTransportLayer(listenPort, this);
	}
	
	// Setteamos la direccion y puerto del proxy
	public void setProxyData(String address, int port) {
		userLayer.setProxyData(address, port);
	}

	public void onMessageReceived(SIPMessage sipMessage) throws IOException {
		if (sipMessage instanceof InviteMessage) {
			InviteMessage inviteMessage = (InviteMessage) sipMessage;
			userLayer.onInviteReceived(inviteMessage);
		}
		
		else if (sipMessage instanceof RegisterMessage) {
			RegisterMessage registerMessage = (RegisterMessage) sipMessage;
			userLayer.onRegisterReceived(registerMessage);
			
		} 
		else if (sipMessage instanceof RingingMessage) {
			RingingMessage ringingMessage = (RingingMessage) sipMessage;
			userLayer.onRingingReceived(ringingMessage);
		}
		else if (sipMessage instanceof OKMessage) {
			OKMessage okMessage = (OKMessage) sipMessage;
			userLayer.onOKReceived(okMessage);
		}
		else if (sipMessage instanceof RequestTimeoutMessage) {
			RequestTimeoutMessage timeoutMessage = (RequestTimeoutMessage) sipMessage;
			userLayer.onRequestTimeoutReceived(timeoutMessage);;
		}
		else if (sipMessage instanceof BusyHereMessage) {
			BusyHereMessage busyHereMessage = (BusyHereMessage) sipMessage;
			userLayer.onBusyHereReceived(busyHereMessage);;
		}
		else if (sipMessage instanceof ACKMessage) {
			ACKMessage ACKMessage = (ACKMessage) sipMessage;
			userLayer.onACKReceived(ACKMessage);;
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
	
	// mensaje 406 request timeout
	public void echoTimeout(RequestTimeoutMessage timeoutMessage, String address, int port) throws IOException {
		transportLayer.send(timeoutMessage, address, port);
	}

	// mensaje 486 busy here
	public void echoBusyHere(BusyHereMessage busyHereMessage, String address, int port) throws IOException {
		transportLayer.send(busyHereMessage, address, port);
	}
	// mensaje 503 service unavailable
	public void echoServiceUnavailable(ServiceUnavailableMessage serviceUnavailableMessage, String address, int port) throws IOException {
		transportLayer.send(serviceUnavailableMessage, address, port);
	}
	
	// mensaje ACK
	public void echoACK(ACKMessage ACKMessage, String address, int port) throws IOException {
		transportLayer.send(ACKMessage, address, port);
	}
	
	// mensaje bye bye
	public void echoBye(ByeMessage byeMessage, String address, int port) throws IOException {
		transportLayer.send(byeMessage, address, port);
	}
		
	public void startListening() {
		transportLayer.startListening();
	}
}
