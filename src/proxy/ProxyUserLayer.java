package proxy;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import mensajesSIP.InviteMessage;
import mensajesSIP.RegisterMessage;
import mensajesSIP.RingingMessage;
import mensajesSIP.OKMessage;
import mensajesSIP.NotFoundMessage;
import mensajesSIP.SDPMessage;
import mensajesSIP.TryingMessage;
import mensajesSIP.RequestTimeoutMessage;
import mensajesSIP.BusyHereMessage;
import mensajesSIP.ServiceUnavailableMessage;

public class ProxyUserLayer {
	private ProxyTransactionLayer transactionLayer;
	
	private String originAddress;
	private int originPort;
	private ProxyWhiteListArray whiteList;

	public ProxyUserLayer(int listenPort) throws SocketException {
		this.transactionLayer = new ProxyTransactionLayer(listenPort, this);
		this.whiteList = new ProxyWhiteListArray();
	}

	public void onInviteReceived(InviteMessage inviteMessage) throws IOException {
		System.out.println("Received INVITE from " + inviteMessage.getFromName());
		ArrayList<String> vias = inviteMessage.getVias();
		String origin = vias.get(0);
		String[] originParts = origin.split(":");
		String originAddress = originParts[0];
		int originPort = Integer.parseInt(originParts[1]);
		
		int whiteListSize = whiteList.getWhiteList().size();
		System.out.println("\n");
		System.out.println("Se quiere invitar a: " + inviteMessage.getToName());
		System.out.println("El invite lo envio: " + inviteMessage.getFromName());
		
		//Comprobar si el usuario esta en la lista
		for(int i = 0; i < whiteListSize; i++)
		{
			if(getFromWhiteList(i).equalsIgnoreCase(inviteMessage.getToName())) {
				// INFORMAR AL USUARIO DE QUE SE ESTA INTENTANDO CONECTAR CON EL USUSARIO A LA QUE QUIERE INVITAR
				transactionLayer.echoTrying(TryingMessage(), originAddress,originPort);
				
				// INVITAR AL USUARIO (ES IMPORTANTE SABER QUE PUERTO TIENE Y DIRECCION
				// USAR EL OBJETO DE LA LISTA PARA OBTENER LO, PERO PRIMERO HAY QUE
				// HACER EL SET DE ESTAS VARIABLES
				transactionLayer.echoInvite(inviteMessage, originAddress, 9100);
				return;
			}
		}
		transactionLayer.echoNotfound(NotFoundMessage(), originAddress, originPort);
		System.out.println("UNKNOWN USER");
	}
	
	public void onRegisterReceived(RegisterMessage registerMessage) throws IOException {
		System.out.println("Received REGISTER from " + registerMessage.getFromName());
		//System.out.println(registerMessage.getFromName());
		//System.out.println("hgh "+ whiteList.getWhiteList().get(0).getUserURI());
		
		ArrayList<String> vias = registerMessage.getVias();
		String origin = vias.get(0);
		String[] originParts = origin.split(":");
		originAddress = originParts[0];
		originPort = Integer.parseInt(originParts[1]);
		
		int whiteListSize = whiteList.getWhiteList().size();
		
		//Comprobar si el usuario esta en la lista
		for(int i = 0; i < whiteListSize; i++)
		{
			if(getFromWhiteList(i).equals(registerMessage.getFromName())) {
				
				transactionLayer.echoOK(OKMessage(), originAddress, originPort);
				return;
			}
		}
		transactionLayer.echoNotfound(NotFoundMessage(), originAddress, originPort);
		System.out.println("UNKNOWN USER");
	}
	
	public void onRingingReceived(RingingMessage ringingMessage) throws IOException {
		System.out.println("Received Ringing from " + ringingMessage.getFromName());
		//System.out.println(registerMessage.getFromName());
		//System.out.println("hgh "+ whiteList.getWhiteList().get(0).getUserURI());
		
		ArrayList<String> vias = ringingMessage.getVias();
		String origin = vias.get(0);
		String[] originParts = origin.split(":");
		originAddress = originParts[0];
		originPort = Integer.parseInt(originParts[1]);
		
		int whiteListSize = whiteList.getWhiteList().size();
		
		//Comprobar si el usuario esta en la lista
		for(int i = 0; i < whiteListSize; i++)
		{
			if(getFromWhiteList(i).equals(ringingMessage.getToName().toLowerCase())) {
				transactionLayer.echoRinging(ringingMessage, originAddress, 9000);
				return;
			}
		}
		transactionLayer.echoNotfound(NotFoundMessage(), originAddress, originPort);
		System.out.println("UNKNOWN USER");
	}
	
	// on OK received
	public void onOKReceived(OKMessage okMessage) throws IOException {
		System.out.println("Received Ringing from " + okMessage.getFromName());

		
		ArrayList<String> vias = okMessage.getVias();
		String origin = vias.get(0);
		String[] originParts = origin.split(":");
		originAddress = originParts[0];
		originPort = Integer.parseInt(originParts[1]);
		
		int whiteListSize = whiteList.getWhiteList().size();
		
		//Comprobar si el usuario esta en la lista
		for(int i = 0; i < whiteListSize; i++)
		{
			if(getFromWhiteList(i).equals(okMessage.getToName().toLowerCase())) {
				transactionLayer.echoOK(okMessage, originAddress, 9000);
				return;
			}
		}
		
		transactionLayer.echoNotfound(NotFoundMessage(), originAddress, originPort);
		System.out.println("UNKNOWN USER");
	}
	
	// on OK received
	public void onBusyHereReceived(BusyHereMessage busyHereMessage) throws IOException {
		System.out.println("Received busyHereMessage from " + busyHereMessage.getFromName());

		
		ArrayList<String> vias = busyHereMessage.getVias();
		String origin = vias.get(0);
		String[] originParts = origin.split(":");
		originAddress = originParts[0];
		originPort = Integer.parseInt(originParts[1]);
		
		int whiteListSize = whiteList.getWhiteList().size();
		
		//Comprobar si el usuario esta en la lista
		for(int i = 0; i < whiteListSize; i++)
		{
			if(getFromWhiteList(i).equals(busyHereMessage.getToName().toLowerCase())) {
				transactionLayer.echoBusyHere(busyHereMessage, originAddress, 9000);
				return;
			}
		}
		
		transactionLayer.echoNotfound(NotFoundMessage(), originAddress, originPort);
		System.out.println("UNKNOWN USER");
	}

	private String getFromWhiteList(int i) {
		String whiteListUser;
		whiteListUser = whiteList.getWhiteList().get(i).getUserURI().substring(0, whiteList.getWhiteList().get(i).getUserURI().indexOf("@"));
		return whiteListUser;
	}
	
	
	//** MENSAJES PARA ENVIAR **//
	
	
	// 200 OK message
	private OKMessage OKMessage() throws IOException {
		
		String callId = UUID.randomUUID().toString();
		
		OKMessage okMessage = new OKMessage();	
		
		okMessage.setVias(new ArrayList<String>(Arrays.asList(originAddress + ":" + originPort)));
		okMessage.setToName("Bob");
		okMessage.setToUri("sip:bob@SMA");
		okMessage.setFromName("Alice");
		okMessage.setFromUri("sip:alice@SMA");
		okMessage.setCallId(callId);
		okMessage.setcSeqNumber("1");
		okMessage.setcSeqStr("INVITE");
		okMessage.setContact(originAddress + ":" + originPort);
		
		return okMessage;
	}
	
	// 404 not found message
	private NotFoundMessage NotFoundMessage() throws IOException {
		
		String callId = UUID.randomUUID().toString();
		
		NotFoundMessage notFoundMessage = new NotFoundMessage();	
		
		notFoundMessage.setVias(new ArrayList<String>(Arrays.asList(originAddress + ":" + originPort)));
		notFoundMessage.setToName("Bob");
		notFoundMessage.setToUri("sip:bob@SMA");
		notFoundMessage.setFromName("Alice");
		notFoundMessage.setFromUri("sip:alice@SMA");
		notFoundMessage.setCallId(callId);
		notFoundMessage.setcSeqNumber("1");
		notFoundMessage.setcSeqStr("INVITE");
		notFoundMessage.setContact(originAddress + ":" + originPort);
		
		return notFoundMessage;
	}
	
	// 100 trying message
	private TryingMessage TryingMessage() throws IOException {
		
		String callId = UUID.randomUUID().toString();
		
		TryingMessage tryingMessage = new TryingMessage();	
		
		tryingMessage.setVias(new ArrayList<String>(Arrays.asList(originAddress + ":" + originPort)));
		tryingMessage.setToName("Bob");
		tryingMessage.setToUri("sip:bob@SMA");
		tryingMessage.setFromName("Alice");
		tryingMessage.setFromUri("sip:alice@SMA");
		tryingMessage.setCallId(callId);
		tryingMessage.setcSeqNumber("1");
		tryingMessage.setcSeqStr("INVITE");
		tryingMessage.setContentLength(0);

		return tryingMessage;
	}
	
	// 503 Service Unavailable
		private ServiceUnavailableMessage ServiceUnavailableMessage() throws IOException {
			
			String callId = UUID.randomUUID().toString();
			
			ServiceUnavailableMessage serviceUnavailableMessage = new ServiceUnavailableMessage();	
			
			serviceUnavailableMessage.setVias(new ArrayList<String>(Arrays.asList(originAddress + ":" + originPort)));
			serviceUnavailableMessage.setToName("Bob");
			serviceUnavailableMessage.setToUri("sip:bob@SMA");
			serviceUnavailableMessage.setFromName("Alice");
			serviceUnavailableMessage.setFromUri("sip:alice@SMA");
			serviceUnavailableMessage.setCallId(callId);
			serviceUnavailableMessage.setcSeqNumber("1");
			serviceUnavailableMessage.setcSeqStr("INVITE");
			serviceUnavailableMessage.setContentLength(0);

			return serviceUnavailableMessage;
		}

	public void startListening() {
		transactionLayer.startListening();
	}

}
