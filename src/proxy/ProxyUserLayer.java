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
	

	private ProxyWhiteListArray whiteList;
	
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
	
	//NO CASO ESTA MAL
	private int originPort = 0;
	private String originAddress = "";
	private String userURI = "";
	private String userName = "";
	private String userURIB = "";
	private String userNameB = "";
	
	private String firstLine;
	private String userA;
	private String userB;

	public ProxyUserLayer(int listenPort, String firstLine) throws SocketException {
		this.transactionLayer = new ProxyTransactionLayer(listenPort, this);
		this.whiteList = new ProxyWhiteListArray();
		this.firstLine = firstLine;
	}

	// RECIBO MENSAJE INVITE DEL LLAMANTE
	public void onInviteReceived(InviteMessage inviteMessage) throws IOException {
		// Para mostrar el mensaje completo o solo la primera linea
		String[] splittedMessage = inviteMessage.toStringMessage().split("\n", 2);
		String messageToPrint;
		messageToPrint = ((this.firstLine.equals("true")) ? splittedMessage[0]: inviteMessage.toStringMessage());
		System.out.println(messageToPrint + "\n");
		//
		
		userA=inviteMessage.getFromName();
		userB=inviteMessage.getToName();
		
		ArrayList<String> vias = inviteMessage.getVias();
		String origin = vias.get(0);
		String[] originParts = origin.split(":");
		String originAddress = originParts[0];
		int originPort = Integer.parseInt(originParts[1]);
		
		int whiteListSize = whiteList.getWhiteList().size();
		
		//Comprobar si el llamante esta en la lista
		for(int i = 0; i < whiteListSize; i++)
		{
			if(getFromWhiteList(i).equalsIgnoreCase(inviteMessage.getFromName())) {
				stateA = CALLING;
				System.out.println(inviteMessage.toStringMessage());
			} 
			else
			{
				//PASA EXPIRES
				//MANDAR UN MENSAJE DE AUTOREGISTER PARA QUE EL UA SE VUELVE A REGISTRAR
				//transactionLayer.echoTrying(TryingMessage(), originAddress,originPort);
				//return;
			}
		}
		
		System.out.println("\n");
		System.out.println("Se quiere invitar a: " + inviteMessage.getToName());
		System.out.println("El invite lo envio: " + inviteMessage.getFromName());
		
		//Comprobar si el LLAMADO
		for(int i = 0; i < whiteListSize; i++)
		{
			if(getFromWhiteList(i).equalsIgnoreCase(inviteMessage.getToName())) {
				System.out.println(whiteList.getWhiteList().get(i).getUserPort());
				String destinationAddress = whiteList.getWhiteList().get(i).getUserAddress();
				int destinationPort = whiteList.getWhiteList().get(i).getUserPort();
				
				// El usuario puede estar en la lista pero no estar registrado
				if(destinationPort!=0)
				{
					// INVITAMOS AL LLAMADO
					transactionLayer.echoInvite(inviteMessage, destinationAddress, destinationPort);
					System.out.println(inviteMessage.toStringMessage());
					
					// Informar al LLAMANTE de que se esta intentando
					transactionLayer.echoTrying(TryingMessage(), originAddress,originPort);
					//System.out.println(TryingMessage().toStringMessage());
					
					return;
				}
			}
		}
		
		// Si el llamado no esta conectado/registrado
		transactionLayer.echoNotfound(NotFoundMessage(), originAddress, originPort);
		System.out.println(NotFoundMessage().toStringMessage());
	}
	
	// Se recibe el mensaje de register 
	public void onRegisterReceived(RegisterMessage registerMessage) throws IOException {
		// Para mostrar el mensaje completo o solo la primera linea
		String[] splittedMessage = registerMessage.toStringMessage().split("\n", 2);
		String messageToPrint;
		messageToPrint = ((this.firstLine.equals("true")) ? splittedMessage[0]: registerMessage.toStringMessage());
		System.out.println(messageToPrint + "\n");
		//
		
		int whiteListSize = whiteList.getWhiteList().size();
		
		ArrayList<String> vias = registerMessage.getVias();
		String origin = vias.get(0);
		String[] originParts = origin.split(":");
		String originAddress = originParts[0];
		int originPort = Integer.parseInt(originParts[1]);
		
		
		//Comprobar si el usuario esta en la lista
		for(int i = 0; i < whiteListSize; i++)
		{
			if(getFromWhiteList(i).equals(registerMessage.getFromName())) {
				
				//System.out.println("Puerto origen: " + originPort );
				// Guardamos su direccion y puerto
				
				whiteList.getWhiteList().get(i).setUserAddress(originAddress);
				whiteList.getWhiteList().get(i).setUserPort(originPort);
				whiteList.getWhiteList().get(i).setUserName(registerMessage.getFromName());
				/*this.originAddress = originAddress;
				this.originPort = originPort;
				this.userURI = registerMessage.getFromUri();
				this.userName = registerMessage.getFromName();*/
		
				transactionLayer.echoOK(OKMessage(whiteList.getWhiteList().get(i)), originAddress, originPort);
				//System.out.println(OKMessage(whiteList.getWhiteList().get(i)).toStringMessage());
				return;
			}
		}
		
		transactionLayer.echoNotfound(NotFoundMessage(), originAddress, originPort);
		System.out.println(NotFoundMessage().toStringMessage());
	}
	
	public void onRingingReceived(RingingMessage ringingMessage) throws IOException {
		// Para mostrar el mensaje completo o solo la primera linea
		String[] splittedMessage = ringingMessage.toStringMessage().split("\n", 2);
		String messageToPrint;
		messageToPrint = ((this.firstLine.equals("true")) ? splittedMessage[0]: ringingMessage.toStringMessage());
		System.out.println(messageToPrint + "\n");
		//
		
		ArrayList<String> vias = ringingMessage.getVias();
		String origin = vias.get(0);
		String[] originParts = origin.split(":");
		String originAddress = originParts[0];
		int originPort = Integer.parseInt(originParts[1]);
	
		stateB = PROCEEDING_B;
		
		int whiteListSize = whiteList.getWhiteList().size();
		
		//Comprobar si el usuario esta en la lista
		for(int i = 0; i < whiteListSize; i++)
		{
			if(getFromWhiteList(i).equals(ringingMessage.getToName().toLowerCase())) {
				String destinationAddress = whiteList.getWhiteList().get(i).getUserAddress();
				int destinationPort = whiteList.getWhiteList().get(i).getUserPort();
				transactionLayer.echoRinging(ringingMessage, destinationAddress, destinationPort);
				//System.out.println(ringingMessage);
				return;
			}
		}
		transactionLayer.echoNotfound(NotFoundMessage(), originAddress, originPort);
		System.out.println(NotFoundMessage().toStringMessage());
	}
	
	// on OK received
	public void onOKReceived(OKMessage okMessage) throws IOException {
		// Para mostrar el mensaje completo o solo la primera linea
		String[] splittedMessage = okMessage.toStringMessage().split("\n", 2);
		String messageToPrint;
		messageToPrint = ((this.firstLine.equals("true")) ? splittedMessage[0]: okMessage.toStringMessage());
		System.out.println(messageToPrint + "\n");
		//
		
		ArrayList<String> vias = okMessage.getVias();
		String origin = vias.get(0);
		String[] originParts = origin.split(":");
		String originAddress = originParts[0];
		int originPort = Integer.parseInt(originParts[1]);

		stateB = TERMINATED_B;
		
		int whiteListSize = whiteList.getWhiteList().size();
		
		
		//Comprobar si el usuario esta en la lista
		for(int i = 0; i < whiteListSize; i++)
		{
			if(getFromWhiteList(i).equals(okMessage.getToName().toLowerCase())) {
				originAddress = whiteList.getWhiteList().get(i).getUserAddress();
				originPort = whiteList.getWhiteList().get(i).getUserPort();
				transactionLayer.echoOK(okMessage, originAddress, originPort);
				//System.out.println(okMessage);
				return;
			}
		}
		
		transactionLayer.echoNotfound(NotFoundMessage(), originAddress, originPort);
		System.out.println(NotFoundMessage().toString());
	}
	
	// on BUSY HERE RECEIVED del llamado
	public void onBusyHereReceived(BusyHereMessage busyHereMessage) throws IOException {
		// Para mostrar el mensaje completo o solo la primera linea
		String[] splittedMessage = busyHereMessage.toStringMessage().split("\n", 2);
		String messageToPrint;
		messageToPrint = ((this.firstLine.equals("true")) ? splittedMessage[0]: busyHereMessage.toStringMessage());
		System.out.println(messageToPrint + "\n");
		//
		
		ArrayList<String> vias = busyHereMessage.getVias();
		String origin = vias.get(0);
		String[] originParts = origin.split(":");
		String originAddress = originParts[0];

		stateB = COMPLETED_B;
				
		int whiteListSize = whiteList.getWhiteList().size();
		
		//Comprobar si el usuario esta en la lista
		for(int i = 0; i < whiteListSize; i++)
		{
			if(getFromWhiteList(i).equals(busyHereMessage.getToName().toLowerCase())) {
				originAddress = whiteList.getWhiteList().get(i).getUserAddress();
				originPort = whiteList.getWhiteList().get(i).getUserPort();
				transactionLayer.echoBusyHere(busyHereMessage, originAddress, originPort);
				System.out.println(busyHereMessage);
				return;
			}
		}
		
		transactionLayer.echoNotfound(NotFoundMessage(), originAddress, originPort);
		System.out.println(NotFoundMessage().toStringMessage());
	}

	private String getFromWhiteList(int i) {
		String whiteListUser;
		whiteListUser = whiteList.getWhiteList().get(i).getUserURI().substring(0, whiteList.getWhiteList().get(i).getUserURI().indexOf("@"));
		return whiteListUser;
	}
	
	
	//** MENSAJES PARA ENVIAR **//
	
	
	// 200 OK message al llamante
	private OKMessage OKMessage(ProxyWhiteList whiteList) throws IOException {
		
		String callId = UUID.randomUUID().toString();
		
		int port = whiteList.getUserPort();
		String address = whiteList.getUserAddress();
		String name = whiteList.getUserName();
		
		OKMessage okMessage = new OKMessage();	
		
		okMessage.setVias(new ArrayList<String>
		(Arrays.asList(address + ":" + port)));
		
		okMessage.setToName(name);
		okMessage.setToUri("sip:"+name+"@SMA");
		okMessage.setFromName(name);
		okMessage.setFromUri("sip:"+name+"@SMA");
		okMessage.setCallId(callId);
		okMessage.setcSeqNumber("1");
		okMessage.setcSeqStr("INVITE");
		okMessage.setContact(address + ":" + port);
		
		// Para mostrar el mensaje completo o solo la primera linea
		String[] splittedMessage = okMessage.toStringMessage().split("\n", 2);
		String messageToPrint;
		messageToPrint = ((this.firstLine.equals("true")) ? splittedMessage[0]: okMessage.toStringMessage());
		System.out.println(messageToPrint + "\n");
		//

		
		return okMessage;
	}
	
	// 404 not found message
	private NotFoundMessage NotFoundMessage() throws IOException {
		
		int port = 0;
		String address = "";
		for(int i = 0; i < whiteList.getWhiteList().size(); i++)
		{
			if(getFromWhiteList(i).equals(userA)) {
				port = whiteList.getWhiteList().get(i).getUserPort();
				address = whiteList.getWhiteList().get(i).getUserAddress();
			}
		}
		
		String callId = UUID.randomUUID().toString();
		
		NotFoundMessage notFoundMessage = new NotFoundMessage();	
		
		notFoundMessage.setVias(new ArrayList<String>(Arrays.asList(address + ":" + port)));
		notFoundMessage.setToName(userB);
		notFoundMessage.setToUri("sip:"+userB+"@SMA");
		notFoundMessage.setFromName(userA);
		notFoundMessage.setFromUri("sip:"+userA+"@SMA");
		notFoundMessage.setCallId(callId);
		notFoundMessage.setcSeqNumber("1");
		notFoundMessage.setcSeqStr("INVITE");
		notFoundMessage.setContact(address + ":" + port);
		
		int whiteListSize = whiteList.getWhiteList().size();
		for(int i = 0; i < whiteListSize; i++)
		{
			if(getFromWhiteList(i).equals(notFoundMessage.getFromName().toLowerCase())) {
				// Para mostrar el mensaje completo o solo la primera linea
				String[] splittedMessage = notFoundMessage.toStringMessage().split("\n", 2);
				String messageToPrint;
				messageToPrint = ((this.firstLine.equals("true")) ? splittedMessage[0]: notFoundMessage.toStringMessage());
				System.out.println(messageToPrint + "\n");
				//
				
			}
		}
		
		return notFoundMessage;
	}
	
	// 100 trying message
	private TryingMessage TryingMessage() throws IOException {
		
		int port = 0;
		String address = "";
		for(int i = 0; i < whiteList.getWhiteList().size(); i++)
		{
			if(getFromWhiteList(i).equals(userA)) {
				port = whiteList.getWhiteList().get(i).getUserPort();
				address = whiteList.getWhiteList().get(i).getUserAddress();
			}
		}
		
		String callId = UUID.randomUUID().toString();
		
		TryingMessage tryingMessage = new TryingMessage();	
		
		tryingMessage.setVias(new ArrayList<String>(Arrays.asList(address + ":" + port)));
		tryingMessage.setToName(userB);
		tryingMessage.setToUri("sip:"+userB+"@SMA");
		tryingMessage.setFromName(userA);
		tryingMessage.setFromUri("sip:"+userA+"@SMA");
		tryingMessage.setCallId(callId);
		tryingMessage.setcSeqNumber("1");
		tryingMessage.setcSeqStr("INVITE");
		tryingMessage.setContentLength(0);
		
		int whiteListSize = whiteList.getWhiteList().size();
		for(int i = 0; i < whiteListSize; i++)
		{
			if(getFromWhiteList(i).equals(tryingMessage.getFromName().toLowerCase())) {
				// Para mostrar el mensaje completo o solo la primera linea
				String[] splittedMessage = tryingMessage.toStringMessage().split("\n", 2);
				String messageToPrint;
				messageToPrint = ((this.firstLine.equals("true")) ? splittedMessage[0]: tryingMessage.toStringMessage());
				System.out.println(messageToPrint + "\n");
				//
			}
		}

		return tryingMessage;
	}
	
	// 503 Service Unavailable
	private ServiceUnavailableMessage ServiceUnavailableMessage() throws IOException {
		
		int port = 0;
		String address = "";
		for(int i = 0; i < whiteList.getWhiteList().size(); i++)
		{
			if(getFromWhiteList(i).equals(userA)) {
				port = whiteList.getWhiteList().get(i).getUserPort();
				address = whiteList.getWhiteList().get(i).getUserAddress();
			}
		}
		
		String callId = UUID.randomUUID().toString();
		
		ServiceUnavailableMessage serviceUnavailableMessage = new ServiceUnavailableMessage();	
		
		serviceUnavailableMessage.setVias(new ArrayList<String>(Arrays.asList(address + ":" + port)));
		serviceUnavailableMessage.setToName(userB);
		serviceUnavailableMessage.setToUri("sip:"+userB+"@SMA");
		serviceUnavailableMessage.setFromName(userA);
		serviceUnavailableMessage.setFromUri("sip:"+userA+"@SMA");
		serviceUnavailableMessage.setCallId(callId);
		serviceUnavailableMessage.setcSeqNumber("1");
		serviceUnavailableMessage.setcSeqStr("INVITE");
		serviceUnavailableMessage.setContentLength(0);
		
		int whiteListSize = whiteList.getWhiteList().size();
		for(int i = 0; i < whiteListSize; i++)
		{
			if(getFromWhiteList(i).equals(serviceUnavailableMessage.getFromName().toLowerCase())) {
				// Para mostrar el mensaje completo o solo la primera linea
				String[] splittedMessage = serviceUnavailableMessage.toStringMessage().split("\n", 2);
				String messageToPrint;
				messageToPrint = ((this.firstLine.equals("true")) ? splittedMessage[0]: serviceUnavailableMessage.toStringMessage());
				System.out.println(messageToPrint + "\n");
				//
					
			}
		}

		return serviceUnavailableMessage;
	}

	public void startListening() {
		transactionLayer.startListening();
	}

}
