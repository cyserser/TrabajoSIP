package proxy;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import mensajesSIP.InviteMessage;
import mensajesSIP.RegisterMessage;
import mensajesSIP.RingingMessage;
import mensajesSIP.OKMessage;
import mensajesSIP.NotFoundMessage;
import mensajesSIP.SDPMessage;
import mensajesSIP.SIPMessage;
import mensajesSIP.TryingMessage;
import mensajesSIP.RequestTimeoutMessage;
import mensajesSIP.BusyHereMessage;
import mensajesSIP.ServiceUnavailableMessage;
import mensajesSIP.ACKMessage;
//import mensajesSIP.ByeMessage;

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
	private String proxyName = "sip:proxy";
	private String proxyAddress;
	private int proxyPort;
	private boolean bIsConnected;
	//private boolean bOnACKReceivedA;
	//private boolean bOnACKReceivedB;
	private Timer timer;
	private Timer timerA;
	private Timer timerB;
	
	public ProxyUserLayer(int listenPort, String firstLine) throws SocketException {
		this.transactionLayer = new ProxyTransactionLayer(listenPort, this);
		this.whiteList = new ProxyWhiteListArray();
		this.firstLine = firstLine;
			
		/*if(timer == null)
		{
			timer = new Timer();
		}*/
	}
	
	// Setteamos las direccion y puerto del proxy
	public void setProxyData(String proxyAddress, int port) {
		this.proxyAddress = proxyAddress;
		this.proxyPort = port;
	}

	// RECIBO MENSAJE INVITE DEL LLAMANTE
	public void onInviteReceived(InviteMessage inviteMessage) throws IOException {
		//REDUCIMOS EL MAXFORWARDS
		inviteMessage.setMaxForwards(inviteMessage.getMaxForwards()-1);
		
		int whiteListSize = whiteList.getWhiteList().size();
		userA=inviteMessage.getFromName();
		userB=inviteMessage.getToName();
		
		String messageType = inviteMessage.toStringMessage();
		//showArrowInMessage(userA, proxyName, messageType);
		
		ArrayList<String> vias = inviteMessage.getVias();
		String origin = vias.get(0);
		String[] originParts = origin.split(":");
		String originAddress = originParts[0];
		int originPort = Integer.parseInt(originParts[1]);
		
		for(int i = 0; i < whiteListSize; i++)
		{
			if(inviteMessage.getFromName().equalsIgnoreCase(getFromWhiteList(i)))
			{
				// Si el usuario se desregistra con el campo expires...
				if(whiteList.getWhiteList().get(i).getIsRegistered() == false)
				{
					System.err.println("usuario no REGISTRADO");
					transactionLayer.echoNotfound(NotFoundMessage(false), originAddress, originPort);
					return;
				}
			}
		
		}
		
		// PROXY ENCAMINA EL INVITE A LLAMADO SI ESTAN EN TERMINATED
		if(stateA == TERMINATED_A && stateB == TERMINATED_B)
		{
			for(int i = 0; i < whiteListSize; i++)
			{
				if(getFromWhiteList(i).equalsIgnoreCase(inviteMessage.getToName())) {
					//System.out.println(whiteList.getWhiteList().get(i).getUserPort());
					String destinationAddress = whiteList.getWhiteList().get(i).getUserAddress();
					int destinationPort = whiteList.getWhiteList().get(i).getUserPort();
					
					// El usuario puede estar en la lista pero no estar registrado
					if(destinationPort!=0)
					{
						// Informar al LLAMANTE de que se esta intentando
						transactionLayer.echoTrying(TryingMessage(), originAddress,originPort);
						
						// INVITAMOS AL LLAMADO
						messageType = inviteMessage.toStringMessage();
						showArrowInMessage(proxyName, userB, messageType);
						transactionLayer.echoInvite(inviteMessage, destinationAddress, destinationPort);
						//bIsConnected = true;
						//System.out.println(inviteMessage.toStringMessage());
						
						return;
					}
				}
			}
		} 
		
		// Esperando a coger o no la llamada 
		else if(stateA == PROCEEDING_A || stateB == PROCEEDING_B)
		{
			transactionLayer.echoServiceUnavailable(ServiceUnavailableMessage(), originAddress,originPort);
			return;
		}

		//Comprobar si el llamante esta en la lista
		for(int i = 0; i < whiteListSize; i++)
		{
			if(getFromWhiteList(i).equalsIgnoreCase(inviteMessage.getFromName())) {
				stateA = CALLING;
				showArrowInMessage(userA, proxyName, messageType);
				System.out.println("Estado Llamante: CALLING");
				System.out.println("Estado llamado: IDLE"+"\n");
				//System.out.println(inviteMessage.toStringMessage());
			} 
			else
			{
				//PASA EXPIRES
				//MANDAR UN MENSAJE DE AUTOREGISTER PARA QUE EL UA SE VUELVE A REGISTRAR
			}
		}
		
		//Comprobar si el LLAMADO
		for(int i = 0; i < whiteListSize; i++)
		{
			if(getFromWhiteList(i).equalsIgnoreCase(inviteMessage.getToName())) {
				//System.out.println(whiteList.getWhiteList().get(i).getUserPort());
				String destinationAddress = whiteList.getWhiteList().get(i).getUserAddress();
				int destinationPort = whiteList.getWhiteList().get(i).getUserPort();
				
				// El usuario puede estar en la lista pero no estar registrado
				if(destinationPort!=0)
				{
					// INVITAMOS AL LLAMADO
					
					// añadimos las vias
					addViasMethod(inviteMessage);
					transactionLayer.echoInvite(inviteMessage, destinationAddress, destinationPort);
					
					messageType = inviteMessage.toStringMessage();
					showArrowInMessage(proxyName, userB, messageType);
					System.out.println("Estado llamante: CALLING");
					System.out.println("Estado Llamado: CALLING"+"\n");
					
					bIsConnected = true;
					//System.out.println(inviteMessage.toStringMessage());
					
					// Informar al LLAMANTE de que se esta intentando
					transactionLayer.echoTrying(TryingMessage(), originAddress,originPort);
					System.out.println("Estado Llamante: PROCEEDING");
					System.out.println("Estado llamado: CALLING"+"\n");
					//System.out.println(TryingMessage().toStringMessage());
					
					return;
				}
			}
		}
		
		// Si el llamado no esta conectado/registrado
		transactionLayer.echoNotfound(NotFoundMessage(true), originAddress, originPort);
		//System.out.println(NotFoundMessage(true).toStringMessage());
	}

	
	
	// Se recibe el mensaje de register 
	public void onRegisterReceived(RegisterMessage registerMessage) throws IOException {
		String messageType = registerMessage.toStringMessage();
		showArrowInMessage(registerMessage.getFromName(), proxyName, messageType);
		
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
				whiteList.getWhiteList().get(i).setIsRegistered(true);
				whiteList.getWhiteList().get(i).setUserAddress(originAddress);
				whiteList.getWhiteList().get(i).setUserPort(originPort);
				whiteList.getWhiteList().get(i).setUserName(registerMessage.getFromName());
				int temp = Integer.parseInt(registerMessage.getExpires());
				String userExpired = registerMessage.getFromName();
				expiresCounter(temp,userExpired);
				
				/*this.originAddress = originAddress;
				this.originPort = originPort;
				this.userURI = registerMessage.getFromUri();
				this.userName = registerMessage.getFromName();*/
		
				transactionLayer.echoOK(OKMessage(whiteList.getWhiteList().get(i)), originAddress, originPort);
				//System.out.println(OKMessage(whiteList.getWhiteList().get(i)).toStringMessage());
				return;
			}
		}
		
		transactionLayer.echoNotfound(NotFoundMessage(true), originAddress, originPort);
		//System.out.println(NotFoundMessage().toStringMessage());
		
	}
	
	// on Ringing received
	public void onRingingReceived(RingingMessage ringingMessage) throws IOException {
		String messageType = ringingMessage.toStringMessage();
		showArrowInMessage(ringingMessage.getToName(), proxyName, messageType);
		
		ArrayList<String> vias = ringingMessage.getVias();
		String origin = vias.get(0);
		String[] originParts = origin.split(":");
		String originAddress = originParts[0];
		int originPort = Integer.parseInt(originParts[1]);
	
		stateB = PROCEEDING_B;
		System.out.println("Estado llamaante: PROCEEDING");
		System.out.println("Estado llamado: PROCEEDING"+"\n");
		
		int whiteListSize = whiteList.getWhiteList().size();
		
		//Comprobar si el usuario esta en la lista
		for(int i = 0; i < whiteListSize; i++)
		{
			if(getFromWhiteList(i).equals(ringingMessage.getFromName().toLowerCase())) {
				String destinationAddress = whiteList.getWhiteList().get(i).getUserAddress();
				int destinationPort = whiteList.getWhiteList().get(i).getUserPort();
				// añadimos las vias
				addViasMethod(ringingMessage);
				showArrowInMessage(proxyName, ringingMessage.getFromName(), messageType);
				transactionLayer.echoRinging(ringingMessage, destinationAddress, destinationPort);
				//System.out.println(ringingMessage);
				return;
			}
		}
		transactionLayer.echoNotfound(NotFoundMessage(true), originAddress, originPort);
		//System.out.println(NotFoundMessage().toStringMessage());
	}
	
	// on OK received
	public void onOKReceived(OKMessage okMessage) throws IOException {
		String messageType = okMessage.toStringMessage();
		showArrowInMessage(okMessage.getToName(),proxyName, messageType);
		
		ArrayList<String> vias = okMessage.getVias();
		String origin = vias.get(0);
		String[] originParts = origin.split(":");
		String originAddress = originParts[0];
		int originPort = Integer.parseInt(originParts[1]);

		stateB = TERMINATED_B;
		System.out.println("Estado llamante: PROCEEDING");
		System.out.println("Estado llamado: TERMINATED"+"\n");
		
		int whiteListSize = whiteList.getWhiteList().size();
		
		
		//Comprobar si el usuario esta en la lista
		for(int i = 0; i < whiteListSize; i++)
		{
			if(getFromWhiteList(i).equals(okMessage.getFromName().toLowerCase())) {
				originAddress = whiteList.getWhiteList().get(i).getUserAddress();
				originPort = whiteList.getWhiteList().get(i).getUserPort();
				// añadimos las vias
				addViasMethod(okMessage);
				showArrowInMessage(proxyName,okMessage.getFromName(), messageType);
				transactionLayer.echoOK(okMessage, originAddress, originPort);
				stateA = TERMINATED_A;
				System.out.println("Estado llamante: TERMINATED");
				System.out.println("Estado llamado: TERMINATED"+"\n");
				//System.out.println(okMessage);
				return;
			}
		}
		
		transactionLayer.echoNotfound(NotFoundMessage(true), originAddress, originPort);
		//System.out.println(NotFoundMessage().toString());
	}
	
	// on Request Timeout
	public void onRequestTimeoutReceived(RequestTimeoutMessage timeoutMessage) throws IOException {
		String messageType = timeoutMessage.toStringMessage();
		showArrowInMessage(userB, proxyName, messageType);
		
		ArrayList<String> vias = timeoutMessage.getVias();
		String origin = vias.get(0);
		String[] originParts = origin.split(":");
		String originAddress = originParts[0];
		int originPort = Integer.parseInt(originParts[1]);
		
		stateB = COMPLETED_B;
				
		int whiteListSize = whiteList.getWhiteList().size();
		
		//Comprobar si el usuario esta en la lista
		for(int i = 0; i < whiteListSize; i++)
		{
			if(getFromWhiteList(i).equals(timeoutMessage.getFromName().toLowerCase())) {
				originAddress = whiteList.getWhiteList().get(i).getUserAddress();
				originPort = whiteList.getWhiteList().get(i).getUserPort();
				// añadimos las vias
				addViasMethod(timeoutMessage);
				messageType = timeoutMessage.toStringMessage();
				showArrowInMessage(proxyName,timeoutMessage.getFromName(), messageType);
				transactionLayer.echoTimeout(timeoutMessage, originAddress, originPort);
				return;
			}
		}
		
		transactionLayer.echoNotfound(NotFoundMessage(true), originAddress, originPort);
		System.out.println(NotFoundMessage(true).toStringMessage());
	}
	
	// on BUSY HERE RECEIVED del llamado
	public void onBusyHereReceived(BusyHereMessage busyHereMessage) throws IOException {
		String messageType = busyHereMessage.toStringMessage();
		showArrowInMessage(userB, proxyName, messageType);
		
		ArrayList<String> vias = busyHereMessage.getVias();
		String origin = vias.get(0);
		String[] originParts = origin.split(":");
		String originAddress = originParts[0];
		int originPort = Integer.parseInt(originParts[1]);
		
		stateB = COMPLETED_B;
		System.out.println("Estado llamante: PROCEEDING");
		System.out.println("Estado llamado: COMPLETED"+"\n");
				
		int whiteListSize = whiteList.getWhiteList().size();
		
		//Comprobar si el usuario esta en la lista
		for(int i = 0; i < whiteListSize; i++)
		{
			if(getFromWhiteList(i).equals(busyHereMessage.getFromName().toLowerCase())) {
				originAddress = whiteList.getWhiteList().get(i).getUserAddress();
				originPort = whiteList.getWhiteList().get(i).getUserPort();
				// añadimos las vias
				addViasMethod(busyHereMessage);
				messageType = busyHereMessage.toStringMessage();
				showArrowInMessage(proxyName, userA, messageType);
				transactionLayer.echoBusyHere(busyHereMessage, originAddress, originPort);
				System.out.println("Estado llamante: COMPLETED");
				System.out.println("Estado llamado: COMPLETED"+"\n");
				// Iniciamos el temporizador
				ACKTimer(busyHereMessage, originPort, originAddress);
				//System.out.println(busyHereMessage);
				return;
			}
		}
		
		transactionLayer.echoNotfound(NotFoundMessage(true), originAddress, originPort);
		System.out.println(NotFoundMessage(true).toStringMessage());
	}
	
	// on ACK received
	public void onACKReceived(ACKMessage ACKMessage) throws IOException {
		String messageType = ACKMessage.toStringMessage();
		showArrowInMessage(userA, proxyName, messageType);
		
		ArrayList<String> vias = ACKMessage.getVias();
		String origin = vias.get(0);
		String[] originParts = origin.split(":");
		String originAddress = originParts[0];
		int originPort = Integer.parseInt(originParts[1]);
		
		//stateB = COMPLETED_B;
		
		//bOnACKReceivedA = true;
		if(timer != null)
		{
			timer.cancel();
			timer.purge();
		}
	
		//ACKTimer(ACKMessage, originPort, originAddress);
				
		int whiteListSize = whiteList.getWhiteList().size();
		
		//Comprobar si el usuario esta en la lista
		for(int i = 0; i < whiteListSize; i++)
		{
			if(ACKMessage.getToName()!=null) {
				if(getFromWhiteList(i).equals(ACKMessage.getToName().toLowerCase())) {
					originAddress = whiteList.getWhiteList().get(i).getUserAddress();
					originPort = whiteList.getWhiteList().get(i).getUserPort();
					if(originPort!=0)
					{
						messageType = ACKMessage.toStringMessage();
						addViasMethod(ACKMessage);
						showArrowInMessage(proxyName, userB, messageType);
						/***
						 * Descomentar esta linea para probar el re
						 */
						transactionLayer.echoACK(ACKMessage, originAddress, originPort);
						return;
					}
				}	
			}
			
		}
		//transactionLayer.echoACK(ACKMessage, originAddress, originPort);
		//transactionLayer.echoNotfound(NotFoundMessage(), originAddress, originPort);
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
		
		String messageType = okMessage.toStringMessage();
		showArrowInMessage(proxyName, okMessage.getToName(), messageType);

		
		return okMessage;
	}
	
	// 404 not found message
	private NotFoundMessage NotFoundMessage(boolean bRegister) throws IOException {
		
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
		if(!bRegister)
		{
			notFoundMessage.setExpires("0");
		}
		
		//int whiteListSize = whiteList.getWhiteList().size();
		
		String messageType = notFoundMessage.toStringMessage();
		showArrowInMessage(proxyName, userA, messageType);
		
		// Iniciamos el temporizador
		ACKTimer(notFoundMessage, port, address);
		
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
		
		//int whiteListSize = whiteList.getWhiteList().size();
		
		String messageType = tryingMessage.toStringMessage();
		showArrowInMessage(proxyName, userA, messageType);

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
		
		String messageType = serviceUnavailableMessage.toStringMessage();
		showArrowInMessage(proxyName, userA, messageType);

		return serviceUnavailableMessage;
	}
	
	// TEMPORIZADORES
	private void ACKTimer(SIPMessage sipMessage, int destinationPort, String destinationAddress) {		
		int time = 2;
		int proxyPort = this.proxyPort;
		int otherPort = destinationPort;
		String otherAddress = destinationAddress;
		
		if(sipMessage instanceof BusyHereMessage)
		{
			BusyHereMessage busyHereMessage = (BusyHereMessage) sipMessage;
			/*Timer*/ timer = new Timer();
			TimerTask task = new TimerTask() {
				int counter=0;
			    @Override
			    public void run() {
			    	try {
			    		/*if(bOnACKReceivedA)
			    		{
			    			timer.cancel();
			    			timer.purge();
			    			bOnACKReceivedA = false;
			    			//return;
			    		}
			    		
			    		else*/
			    		if(counter != 0)
			    		{
			    			String messageType = busyHereMessage.toStringMessage();
			    			showArrowInMessage(proxyName, userA , messageType);
			    			transactionLayer.echoBusyHere(busyHereMessage, otherAddress, otherPort);
			    		}
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			       	counter=counter+time;
			    }
			};
			// Timer cada 200 ms
			timer.scheduleAtFixedRate(task, 0, time*1000);

		}
		else if(sipMessage instanceof RequestTimeoutMessage)
		{
			RequestTimeoutMessage timeoutMessage = (RequestTimeoutMessage) sipMessage;
			
		}
		else if(sipMessage instanceof NotFoundMessage)
		{
			NotFoundMessage notFoundMessage = (NotFoundMessage) sipMessage;
			/*Timer*/ timer = new Timer();
			TimerTask task = new TimerTask() {
				int counter=0;
			    @Override
			    public void run() {
			    	try {
			    		/*if(bOnACKReceivedA)
			    		{
			    			timer.cancel();
			    			timer.purge();
			    			bOnACKReceivedA = false;
			    			//return;
			    		}
			    		else*/ 
			    		if(counter != 0)
			    		{
			    			String messageType = notFoundMessage.toStringMessage();
			    			showArrowInMessage(proxyName, userA , messageType);
			    			transactionLayer.echoNotfound(notFoundMessage, otherAddress, otherPort);
			    		}
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			       	counter=counter+time;				 
			    }
			};
			// Timer cada 200 ms
			timer.scheduleAtFixedRate(task, 0, time*1000);	
			
		}
		else if(sipMessage instanceof ServiceUnavailableMessage)
		{
			ServiceUnavailableMessage notAvailable = (ServiceUnavailableMessage) sipMessage;
		
		}
		else
		{
			System.err.println("Error no se reconoce el mensaje...");
		}

	}
	
	// ACK
	private ACKMessage ACKMessage() throws IOException {
		
		System.out.println(userA);
		int port = 0;
		String address = "";
		for(int i = 0; i < whiteList.getWhiteList().size(); i++)
		{
			if(getFromWhiteList(i).equals(userB)) {
				port = whiteList.getWhiteList().get(i).getUserPort();
				address = whiteList.getWhiteList().get(i).getUserAddress();
			}
		}
		
		String callId = UUID.randomUUID().toString();
		
		ACKMessage ACKMessage = new ACKMessage();	
		
		ACKMessage.setVias(new ArrayList<String>(Arrays.asList(address + ":" + port)));
		ACKMessage.setToName(userB);
		ACKMessage.setToUri("sip:"+userB+"@SMA");
		ACKMessage.setFromName(userA);
		ACKMessage.setFromUri("sip:"+userA+"@SMA");
		ACKMessage.setCallId(callId);
		ACKMessage.setcSeqNumber("1");
		ACKMessage.setcSeqStr("INVITE");
		ACKMessage.setMaxForwards(70);
		ACKMessage.setContentLength(0);
		
		String messageType = ACKMessage.toStringMessage();
		showArrowInMessage(proxyName, userB, messageType);


		return ACKMessage;
	}

	public void startListening() {
		transactionLayer.startListening();
	}
	
	/// METODOS AUXILIARES
	
	private String getFromWhiteList(int i) {
		String whiteListUser;
		whiteListUser = whiteList.getWhiteList().get(i).getUserURI().substring(0, whiteList.getWhiteList().get(i).getUserURI().indexOf("@"));
		return whiteListUser;
	}
	
	private void showArrowInMessage(String from, String to, String messageType) { 
		String commInfo = messageType.substring(0,messageType.indexOf(" "))
				+ " " + from + " -> " + to;
		String[] splittedMessage = messageType.split("\n", 2);
		String messageToPrint;
		messageToPrint = ((this.firstLine.equals("true")) ? splittedMessage[0]: messageType);
		System.out.println(commInfo);
		System.out.println(messageToPrint + "\n");
	}
	
	private void expiresCounter(int expiresTime, String expiredUser) {
		Timer timer = new Timer();
		TimerTask task = new TimerTask() {
			int counter = expiresTime;
		    @Override
		    public void run() {
		    	counter = counter - 1;
		    	if(counter == 0)
		    	{
		    		//System.err.println("user expired!");
		    		int whiteListSize = whiteList.getWhiteList().size();
		    		// Des-registrar al ususario
		    		for(int i = 0; i < whiteListSize; i++)
		    		{
		    			if(getFromWhiteList(i).equals(expiredUser)) {
		    				whiteList.getWhiteList().get(i).setIsRegistered(false);
		    				//whiteList.getWhiteList().get(i).setUserPort(0);
		    				//whiteList.getWhiteList().get(i).setUserName(null);
		    				
		    				return;
		    			}
		    		}
		    	}
		    }
		};
		
		timer.scheduleAtFixedRate(task, 0, 1000);
	}
	
	// Metodo para añadir las vias generico para diferentes tipos de mensajes
	private void addViasMethod(SIPMessage sipMessage) {
		if(sipMessage!=null)
		{
			ArrayList<String> viaFinal = new ArrayList<>();
			String via1;
			String via2;
			
			if (sipMessage instanceof InviteMessage) {
				InviteMessage inviteMessage = (InviteMessage) sipMessage;
				
				via1 = String.join(", ", inviteMessage.getVias());
				via2 = this.proxyAddress+":"+this.proxyPort;
			
				viaFinal.add(via1);
				viaFinal.add(via2);
				inviteMessage.setVias(viaFinal);
			} 
			else if(sipMessage instanceof OKMessage)
			{
				OKMessage okMessage = (OKMessage) sipMessage;
				via1 = String.join(", ", okMessage.getVias());
				via2 = this.proxyAddress+":"+this.proxyPort;
			
				viaFinal.add(via1);
				viaFinal.add(via2);
				okMessage.setVias(viaFinal);
				
			}
			else if(sipMessage instanceof RingingMessage)
			{
				RingingMessage ringingMessage = (RingingMessage) sipMessage;
				via1 = String.join(", ", ringingMessage.getVias());
				via2 = this.proxyAddress+":"+this.proxyPort;
			
				viaFinal.add(via1);
				viaFinal.add(via2);
				ringingMessage.setVias(viaFinal);
				
			}
			else if(sipMessage instanceof BusyHereMessage)
			{
				BusyHereMessage busyHereMessage = (BusyHereMessage) sipMessage;
				via1 = String.join(", ", busyHereMessage.getVias());
				via2 = this.proxyAddress+":"+this.proxyPort;
			
				viaFinal.add(via1);
				viaFinal.add(via2);
				busyHereMessage.setVias(viaFinal);
			}
			else if(sipMessage instanceof RequestTimeoutMessage)
			{
				RequestTimeoutMessage timeoutMessage = (RequestTimeoutMessage) sipMessage;
				via1 = String.join(", ", timeoutMessage.getVias());
				via2 = this.proxyAddress+":"+this.proxyPort;
				viaFinal.add(via1);
				viaFinal.add(via2);
				timeoutMessage.setVias(viaFinal);
			}
			else if(sipMessage instanceof ACKMessage)
			{
				ACKMessage ACKMessage = (ACKMessage) sipMessage;
				via1 = String.join(", ", ACKMessage.getVias());
				via2 = this.proxyAddress+":"+this.proxyPort;
				viaFinal.add(via1);
				viaFinal.add(via2);
				ACKMessage.setVias(viaFinal);
			}
			else
			{
				System.err.println("Error no se reconoce el mensaje...");
			}
		}
	}

}
