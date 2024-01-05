package ua;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.UUID;
import java.util.Timer;
import java.util.TimerTask;

import common.FindMyIPv4;
import mensajesSIP.InviteMessage;
import mensajesSIP.RegisterMessage;
import mensajesSIP.RequestTimeoutMessage;
import mensajesSIP.OKMessage;
import mensajesSIP.NotFoundMessage;
import mensajesSIP.SDPMessage;
import mensajesSIP.SIPMessage;
import mensajesSIP.ServiceUnavailableMessage;
import mensajesSIP.TryingMessage;
import mensajesSIP.RingingMessage;
import mensajesSIP.BusyHereMessage;
import mensajesSIP.ByeMessage;
import mensajesSIP.ACKMessage;

public class UaUserLayer {
	
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
	
	private int state = IDLE;
	private String Message = "";
	private String proxyName = "sip:proxy";
	private String userTo;
	private String userFrom;
	
	public static final ArrayList<Integer> RTPFLOWS = new ArrayList<Integer>(
			Arrays.asList(new Integer[] { 96, 97, 98 }));

	private UaTransactionLayer transactionLayer;

	private String myAddress = FindMyIPv4.findMyIPv4Address().getHostAddress();
	private int rtpPort;
	private int listenPort;
	private String userURI;
	private String userA;
	private String userB;
	private String firstLine;
	private String expiresTime;
	private int puertoB; // llamado
	private boolean isDisconnected = true;
	private boolean isACKReceived;
	private boolean isLooseRouting;
	private String looseRoutingString;
	private Process vitextClient = null;
	private Process vitextServer = null;

	public UaUserLayer(String userURI, int listenPort, String proxyAddress, int proxyPort, String firstLine, String expiresTime)
			throws SocketException, UnknownHostException {
		this.transactionLayer = new UaTransactionLayer(listenPort, proxyAddress, proxyPort, this);
		this.listenPort = listenPort;
		this.rtpPort = listenPort + 1;
		this.userURI = userURI;
		this.firstLine = firstLine;
		this.expiresTime = expiresTime;
	}

	public void onInviteReceived(InviteMessage inviteMessage) throws IOException {
		
		userA = inviteMessage.getFromName();
		userB = inviteMessage.getToName();
		
		String messageType = inviteMessage.toStringMessage();
		showArrowInMessage(proxyName, userB, messageType);
		
		// Si loose routing
		addRecordRoute(inviteMessage);
		
		ArrayList<String> vias = inviteMessage.getVias();
		String origin = vias.get(0);
		String[] originParts = origin.split(":");
		
		int originPort = Integer.parseInt(originParts[1]);
		puertoB = originPort;
		
		if(state == TERMINATED_A || state == TERMINATED_B)
		{
			commandBusy("");
			return;
		}
		
		state = PROCEEDING_B;
		System.out.println("Estado: PROCEEDING"+"\n");
		
		commandRinging("");
		System.out.println("Introduce an s to accept the call or deny with a n...");
			
		prompt();
		runVitextServer();
	}

	
	
	// OK MESSAGE RECEIVED
	public void onOKReceived(OKMessage okMessage) throws IOException {
		this.Message = "OK";
		
		userA = okMessage.getFromName();
		userB = okMessage.getToName();
		
		// RecordRoute (LLAMADO)
		addRecordRoute(okMessage);
		
		String userURIString = userURI.substring(0, userURI.indexOf("@"));
		String messageType = okMessage.toStringMessage();
		// Una vez establecido la llamada
		if (state == TERMINATED_B || state == TERMINATED_A) {
			messageType = okMessage.toStringMessage();
			if(this.isLooseRouting)
			{
				showArrowInMessage(proxyName, userURIString, messageType);
				System.out.println("Estado: IDLE"+"\n");	
			}
			else
			{
				showArrowInMessage(userB, userURIString, messageType);
				System.out.println("Estado: IDLE"+"\n");
			}
			
			state = IDLE;
		}
		else if(state == CALLING || state == PROCEEDING_A) {
			state = TERMINATED_A;
			
			messageType = okMessage.toStringMessage();
			showArrowInMessage(proxyName, userURIString, messageType);
			System.out.println("Estado: TERMINATED"+"\n");
			
			//Se establece la llamada y comienza a enviarse ACK
			if(this.isLooseRouting)
			{
				commandACK();
			}
			else // Si no hay loose routing va extremo a extremo
			{
				commandACK(this.listenPort, puertoB);
			}
		}
		else 
		{
			showArrowInMessage(proxyName, userURIString, messageType);
		}
	
		isDisconnected = false;
		
		prompt();
		
		//runVitextServer();
	}

	
	
	// 404 NOT FOUND (usuario que no existe cuando lo introducimos por teclado)
	// O cuando se nos expira el campo expires...
	public void onNotFoundReceived(NotFoundMessage notFoundMessage) throws IOException {
		this.Message = "NOT FOUND";

		userA = notFoundMessage.getFromName();
		userB = notFoundMessage.getToName();
		
		// Para mostrar el mensaje completo o solo la primera linea
		String messageType = notFoundMessage.toStringMessage();
		showArrowInMessage(proxyName,userFrom, messageType);
		
		// Enviamos confirmacion
		commandACK();
		
		state = COMPLETED_A;
		System.out.println("Estado: COMPLETED"+"\n");
		
		runVitextServer();
	}
	
	// 100 TRYING
	public void onTryingReceived(TryingMessage tryingMessage) throws IOException {
		this.Message = "TRYING";
		// Para mostrar el mensaje completo o solo la primera linea
		String messageType = tryingMessage.toStringMessage();
		showArrowInMessage(proxyName, userFrom, messageType);
		System.out.println("Estado: PROCEEDING"+"\n");
		//
		
		state = PROCEEDING_A;
		String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		if(tryingMessage.getFromName().equalsIgnoreCase(userURIstring)) {
			prompt();
		}
		runVitextServer();
	}
	
	// 180 RINING
	public void onRingingReceived(RingingMessage ringingMessage) throws IOException {
		this.Message = "RINGING";
		
		userA = ringingMessage.getFromName();
		userB = ringingMessage.getToName();
		
		// Para mostrar el mensaje completo o solo la primera linea
		String messageType = ringingMessage.toStringMessage();
		showArrowInMessage(proxyName, userA, messageType);
		//
		ArrayList<String> vias = ringingMessage.getVias();
		String origin = vias.get(0);
		String[] originParts = origin.split(":");
		
		int originPort = Integer.parseInt(originParts[1]);
		
		puertoB = originPort;
				
		state = PROCEEDING_A;
	
		runVitextServer();
		
	}
	
	// 406 Request Timeout
	public void onRequestTimeoutReceived(RequestTimeoutMessage timeoutMessage) throws IOException {
		String messageType = timeoutMessage.toStringMessage();
		showArrowInMessage(proxyName, userFrom, messageType);
		
		state = COMPLETED_A;
		System.out.println("Estado: COMPLETED"+"\n");
		
		// Enviamos un ACK al mensaje de error
		commandACK();
		
		runVitextServer();
	}
	
	// 486 Busy 
	public void onBusyHereReceived(BusyHereMessage busyHereMessage) throws IOException {
		this.Message = "BUSY";
		String messageType = busyHereMessage.toStringMessage();
		showArrowInMessage(proxyName, userFrom, messageType);
		
		state = COMPLETED_A;
		System.out.println("Estado: COMPLETED"+"\n");
		
		// Enviamos un ACK al mensaje de error
		commandACK();
		
		runVitextServer();
	}
	
	// 503 Service unavailable
	public void onServiceUnavailableReceived(ServiceUnavailableMessage serviceUnavailableMessage) throws IOException {
		this.Message = "BUSY";
		
		String messageType = serviceUnavailableMessage.toStringMessage();
		showArrowInMessage(proxyName, userTo, messageType);
		
		state = COMPLETED_A;
		System.out.println("Estado: COMPLETED"+"\n");
		
		String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		if(serviceUnavailableMessage.getFromName().equalsIgnoreCase(userURIstring)) {
			prompt();
		}
		
		// Enviamos confirmacion
		commandACK();
				
		runVitextServer();
	}
	
	// ACK
	public void onACKReceived(ACKMessage ACKMessage) throws IOException {
		String messageType = ACKMessage.toStringMessage();
		showArrowInMessage(proxyName, ACKMessage.getToName(), messageType);
		
		isACKReceived = true;
		
		if(state == TERMINATED_B || state == TERMINATED_A)
		{
			messageType = ACKMessage.toStringMessage();
			if(!this.isLooseRouting)
			{
				showArrowInMessage(ACKMessage.getFromName(), ACKMessage.getToName(), messageType);
			}
			System.out.println("Type BYE to finish");
		}
		
		runVitextServer();
	}
	
	// BYE
	public void onByeReceived(ByeMessage byeMessage) throws IOException {
		this.Message = "BYE";
		
		ArrayList<String> vias = byeMessage.getVias();
		String origin = vias.get(0);
		String[] originParts = origin.split(":");
		
		int originPort = Integer.parseInt(originParts[1]);
	
		String messageType = byeMessage.toStringMessage();
		
			
		if(this.listenPort == originPort)
		{
			state = TERMINATED_A;
			if(this.isLooseRouting)
			{
				showArrowInMessage(proxyName, byeMessage.getFromName(), messageType);
			}
			else
			{
				showArrowInMessage(byeMessage.getToName(), byeMessage.getFromName(), messageType);
			}
			commandOK("");
			System.out.println("Estado: IDLE"+"\n");
		}
		else
		{
			state = TERMINATED_A;
			if(this.isLooseRouting)
			{
				showArrowInMessage(proxyName, byeMessage.getToName(), messageType);
			}
			else
			{
				showArrowInMessage(byeMessage.getFromName(), byeMessage.getToName(), messageType);
			}
			
			commandOK("");
			System.out.println("Estado: IDLE"+"\n");
		}
		state = IDLE;
		promptIdle();
	
		runVitextServer();
	}
	
	
	public void startListeningNetwork() {
		transactionLayer.startListeningNetwork();
	}

	public void startListeningKeyboard() {
		
		try (Scanner scanner = new Scanner(System.in)) {
			while (true) {
				
				//prompt();
				String line = scanner.nextLine();
				if (!line.isEmpty()) {
					command(line);
				}
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}
	//REGISTRO AUTOMÁTICO AL INICIAR SIN CREDENCIALES
	public void autoRegistering() {
		ourTimer();
	}


	private void prompt() {
		switch (state) {
		case IDLE:
			promptIdle();
			break;
		case PROCEEDING_A:
			promptProceedingA();
			break;
		case PROCEEDING_B:
			promptProceedingB();
			break;
		case CALLING:
			promptCalling();
			break;
		case COMPLETED_A:
			promptCompletedA();
			break;
		case COMPLETED_B:
			promptCompletedB();
			break;
		case TERMINATED_A:
			promptTerminatedA();
			break;
		case TERMINATED_B:
			promptTerminatedB();
			break;
		default:
			throw new IllegalStateException("Unexpected state: " + state);
		}
		System.out.print("> ");
	}

	private void promptIdle() {
		System.out.println(this.userURI);
		System.out.println("INVITE xxx");
	}
	
	private void promptProceedingA() {
		System.out.println(this.userURI);
		System.out.println("Proceeding...");
	}
	
	private void promptProceedingB() {
		//System.out.println(this.userURI);
	}
	private void promptCalling() {
		System.out.println(this.userURI);
		System.out.println("Calling...");
	}
	private void promptCompletedA() {
		System.out.println(this.userURI);
		System.out.println("Llamada fallida A...");
	}
	
	private void promptCompletedB() {
		System.out.println(this.userURI);
		System.out.println("Llamada fallida B...");
	}
	
	private void promptTerminatedA() {
		System.out.println(this.userURI);
		System.out.println("Conexión establecida A");
		System.out.println("Type BYE to finish");
	}
	
	private void promptTerminatedB() {
		System.out.println(this.userURI);
		System.out.println("Conexión establecida B");
		System.out.println("Type BYE to finish");
	}
	

	private void command(String line) throws IOException {
		if (line.startsWith("INVITE")) {
			commandInvite(line, state);
		} 
		else if (line.startsWith("REGISTER")) {
			commandRegister(line, state);
			
		} 
		else if(line.equals("s"))
		{
			this.Message = "s";
			state = TERMINATED_B;
			System.out.println("Estado: TERMINATED"+"\n");
			commandOK("");
			prompt();
			//state = IDLE;
		}
		
		else if(line.equals("n"))
		{
			this.Message = "n";
			state = COMPLETED_B;
			System.out.println("Estado: COMPLETED"+"\n");
			commandBusy("");
		}
		else if(line.equalsIgnoreCase("BYE"))
		{
			if(line.length()==3)
			{
				commandBye("");
			}
		}
		else {
			System.out.println("Bad command"+"\n");
		}
	}

	private void commandInvite(String line, int state) throws IOException {
		stopVitextServer();
		stopVitextClient();
		
		// Si enviamos un invite sin estar registrado nos registramos y salimos
		if(isDisconnected) {
			autoRegistering();
			return;
		}
	
		runVitextClient();
		String callId = UUID.randomUUID().toString();
		
		// El nombre del llamado
		String nameToSend = line.substring(line.indexOf(" ")).trim();
		userTo = nameToSend;
		
		// El nombre del llamante
		String userURIString = userURI.substring(0, userURI.indexOf("@"));
		userFrom = userURIString;
		//System.out.println("Sending to:" + nameToSend);
		
		if(userTo.equals(userURIString))
		{
			System.err.println("Error: no puedes llamar al mismo usuario");
			promptIdle();
			return;
		}
		
		SDPMessage sdpMessage = new SDPMessage();
		sdpMessage.setIp(this.myAddress);
		sdpMessage.setPort(this.rtpPort);
		sdpMessage.setOptions(RTPFLOWS);

		InviteMessage inviteMessage = new InviteMessage();
		inviteMessage.setDestination("sip:"+nameToSend+"@SMA");
		inviteMessage.setVias(new ArrayList<String>(Arrays.asList(this.myAddress + ":" + this.listenPort)));
		inviteMessage.setMaxForwards(70);
		inviteMessage.setToName(nameToSend);
		inviteMessage.setToUri("sip:"+nameToSend+"@SMA");
		inviteMessage.setFromName(userURIString);
		inviteMessage.setFromUri("sip:"+userURIString+"@SMA");
		inviteMessage.setCallId(callId);
		inviteMessage.setcSeqNumber("1");
		inviteMessage.setcSeqStr("INVITE");
		inviteMessage.setContact(myAddress + ":" + listenPort);
		inviteMessage.setContentType("application/sdp");
		inviteMessage.setContentLength(sdpMessage.toStringMessage().getBytes().length);
		inviteMessage.setSdp(sdpMessage);
		
		String messageType = inviteMessage.toStringMessage();
		showArrowInMessage(userURIString, proxyName, messageType);
		this.state = CALLING;
		System.out.println("Estado: CALLING");
		transactionLayer.call(inviteMessage);
	}
	
	private void commandRegister(String line, int state) throws IOException {
		stopVitextServer();
		stopVitextClient();
		
		runVitextClient();

		String callId = UUID.randomUUID().toString();
		String userURIString = userURI.substring(0, userURI.indexOf("@"));
		
		RegisterMessage registerMessage = new RegisterMessage();
	
		registerMessage.setDestination(proxyName);
		registerMessage.setVias(new ArrayList<String>(Arrays.asList(this.myAddress + ":" + this.listenPort)));
		registerMessage.setMaxForwards(70);
		registerMessage.setToName(userURIString);
		registerMessage.setToUri("sip:"+userURI);
		registerMessage.setFromName(userURIString);
		registerMessage.setFromUri("sip:"+userURI);
		registerMessage.setCallId(callId);
		registerMessage.setcSeqNumber("1");
		registerMessage.setcSeqStr("REGISTER");
		registerMessage.setContact(myAddress + ":" + listenPort);
		registerMessage.setAuthorization("Authorization");
		registerMessage.setExpires(expiresTime);
		registerMessage.setContentLength(registerMessage.toStringMessage().getBytes().length);
		
		// Para mostrar el mensaje completo o solo la primera linea
		String messageType = registerMessage.toStringMessage();
		showArrowInMessage(userURIString, proxyName, messageType);
		//
		
		transactionLayer.callRegister(registerMessage);
	
		int tiempo = Integer.parseInt(expiresTime);
		expiresCounter(tiempo,registerMessage.getFromName());
		
	
	}
	
	private void commandTimeout(String line) throws IOException {
		stopVitextServer();
		stopVitextClient();
		
		runVitextClient();

		String callId = UUID.randomUUID().toString();
		
		RequestTimeoutMessage timeout = new RequestTimeoutMessage();
	
		timeout.setVias(new ArrayList<String>(Arrays.asList(this.myAddress + ":" + this.listenPort)));
		timeout.setToName(userB);
		timeout.setToUri("sip:"+userB+"@SMA");
		timeout.setFromName(userA);
		timeout.setFromUri("sip:"+userA+"@SMA");
		timeout.setCallId(callId);
		timeout.setcSeqNumber("1");
		timeout.setcSeqStr("TIMEOUT");
		timeout.setContentLength(timeout.toStringMessage().getBytes().length);
		
		String messageType = timeout.toStringMessage();
		showArrowInMessage(userB, proxyName, messageType);
		
		ACKTimer2(timeout);
		
		transactionLayer.callTimeout(timeout);
	}
	
	private void commandRinging(String line) throws IOException {
		stopVitextServer();
		stopVitextClient();
		
		runVitextClient();

		String callId = UUID.randomUUID().toString();
		
		RingingMessage ringingMessage = new RingingMessage();
	
		ringingMessage.setVias(new ArrayList<String>(Arrays.asList(this.myAddress + ":" + this.listenPort)));
		ringingMessage.setToName(userB);
		ringingMessage.setToUri("sip:"+userB+"@SMA");
		ringingMessage.setFromName(userA);
		ringingMessage.setFromUri("sip:"+userA+"@SMA");
		ringingMessage.setCallId(callId);
		ringingMessage.setcSeqNumber("1");
		ringingMessage.setcSeqStr("INVITE");
		ringingMessage.setContentLength(ringingMessage.toStringMessage().getBytes().length);
		
		String messageType = ringingMessage.toStringMessage();
		showArrowInMessage(userB, proxyName, messageType);
		
		prompt();
	
		transactionLayer.callRinging(ringingMessage);
		ringingTimer();
		
	}
	
	// 200 OK message (lo envia el llamado)
	private void commandOK(String line) throws IOException {
		
		String callId = UUID.randomUUID().toString();
		
		// Si enviamos un invite sin estar registrado nos registramos y salimos
		if(isDisconnected) {
			autoRegistering();
			return;
		}
		
		String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		OKMessage okMessage = new OKMessage();	
		
		if(state == TERMINATED_A && userURIstring.equals(userA)) {
			userA = userB;
			userB = userURIstring;
		}
		
		okMessage.setVias(new ArrayList<String>(Arrays.asList(this.myAddress + ":" + this.listenPort)));
		okMessage.setToName(userB);
		okMessage.setToUri("sip:"+userB+"@SMA");
		okMessage.setFromName(userA);
		okMessage.setFromUri("sip:"+userA+"@SMA");
		okMessage.setCallId(callId);
		okMessage.setcSeqNumber("1");
		okMessage.setcSeqStr("INVITE");
		okMessage.setContact(this.myAddress + ":" + this.listenPort);
		if(this.isLooseRouting)
		{
			String lr = looseRoutingString;
			okMessage.setRecordRoute(lr);
			
			String messageType = okMessage.toStringMessage();
			showArrowInMessage(userB, proxyName, messageType);
			transactionLayer.callOK(okMessage);
		}
		else
		{
			if(state == TERMINATED_A) {
			String messageType = okMessage.toStringMessage();
			showArrowInMessage(userB, userA, messageType);
			transactionLayer.callOK(okMessage, this.myAddress, puertoB);
			
			}
			else if (state == TERMINATED_B) {
				String messageType = okMessage.toStringMessage();
				showArrowInMessage(userB, proxyName, messageType);
				transactionLayer.callOK(okMessage,"",0);
			}
			//String messageType = okMessage.toStringMessage();
			//showArrowInMessage(userB, userA, messageType);
			//transactionLayer.callOK(okMessage, this.myAddress, puertoB);
		}
		
		prompt();
		
		
	}
	
	// 486 BUSY
	private void commandBusy(String line) throws IOException {
		
		String callId = UUID.randomUUID().toString();
		
		// Si enviamos un invite sin estar registrado nos registramos y salimos
		if(isDisconnected) {
			autoRegistering();
			return;
		}
		
		BusyHereMessage busyHereMessage = new BusyHereMessage();	
		
		busyHereMessage.setVias(new ArrayList<String>(Arrays.asList(this.myAddress + ":" + this.listenPort)));
		busyHereMessage.setToName(userB);
		busyHereMessage.setToUri("sip:"+userB+"@SMA");
		busyHereMessage.setFromName(userA);
		busyHereMessage.setFromUri("sip:"+userA+"@SMA");
		busyHereMessage.setCallId(callId);
		busyHereMessage.setcSeqNumber("1");
		busyHereMessage.setcSeqStr("INVITE");
		
		String messageType = busyHereMessage.toStringMessage();
		showArrowInMessage(userB, proxyName, messageType);
		
		prompt();
		
		ACKTimer2(busyHereMessage);
		
		transactionLayer.callBusyHere(busyHereMessage);
	}
	
	// ACK
	private void commandACK(int puertoA, int puertoB) throws IOException {
		
		String callId = UUID.randomUUID().toString();
		
		ACKMessage ACKMessage = new ACKMessage();	
		
		ACKMessage.setVias(new ArrayList<String>(Arrays.asList(this.myAddress + ":" + this.listenPort)));
		ACKMessage.setToName(userB);
		ACKMessage.setToUri("sip:"+userB+"@SMA");
		ACKMessage.setFromName(userA);
		ACKMessage.setFromUri("sip:"+userA+"@SMA");
		ACKMessage.setCallId(callId);
		ACKMessage.setcSeqNumber("1");
		ACKMessage.setcSeqStr("ACK");
		ACKMessage.setDestination("sip:"+userA+"@SMA");
		if(this.isLooseRouting && this.Message.equals("OK"))
		{
			ACKMessage.setRoute(looseRoutingString);
		}
		String messageType = ACKMessage.toStringMessage();
		showArrowInMessage(userA, userB, messageType);
		
		//SI NO HAY LOOSE ROUTING
		transactionLayer.callACK(ACKMessage, this.myAddress, puertoB);
	}
	
	// ACK PARA MENSAJES DE ERROR
	private void commandACK() throws IOException {
		
		String callId = UUID.randomUUID().toString();
		
		ACKMessage ACKMessage = new ACKMessage();	
		
		ACKMessage.setVias(new ArrayList<String>(Arrays.asList(this.myAddress + ":" + this.listenPort)));
		ACKMessage.setToName(userB);
		ACKMessage.setToUri("sip:"+userB+"@SMA");
		ACKMessage.setFromName(userA);
		ACKMessage.setFromUri("sip:"+userA+"@SMA");
		ACKMessage.setCallId(callId);
		ACKMessage.setcSeqNumber("1");
		ACKMessage.setcSeqStr("ACK");
		ACKMessage.setDestination("sip:"+userA+"@SMA");
		if(this.isLooseRouting && this.Message.equals("OK"))
		{
			ACKMessage.setRoute(looseRoutingString);
		}
		
		String messageType = ACKMessage.toStringMessage();
		showArrowInMessage(userA, proxyName, messageType);
		transactionLayer.callACK(ACKMessage);
		//state = IDLE;
		System.out.println("Estado: IDLE");
	}
	
	// BYE BYE
	private void commandBye(String line) throws IOException {
		
		String callId = UUID.randomUUID().toString();
	
		ByeMessage byeMessage = new ByeMessage();	
		
		String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		
		
		if(userURIstring.equals(userB)) {
			userB = userA;
			userA = userURIstring;
		}
		
		byeMessage.setDestination("sip:"+userB+"@SMA");
		byeMessage.setVias(new ArrayList<String>(Arrays.asList(this.myAddress + ":" + this.listenPort)));
		byeMessage.setToName(userB);
		byeMessage.setToUri("sip:"+userB+"@SMA");
		byeMessage.setFromName(userA);
		byeMessage.setFromUri("sip:"+userA+"@SMA");
		byeMessage.setCallId(callId);
		byeMessage.setcSeqNumber("1");
		byeMessage.setcSeqStr("BYE");
		if(this.isLooseRouting)
		{
			byeMessage.setRoute(looseRoutingString);
			String messageType = byeMessage.toStringMessage();
			showArrowInMessage(userA, proxyName, messageType);
			transactionLayer.callBye(byeMessage);
		}
		else
		{
			String messageType = byeMessage.toStringMessage();
			showArrowInMessage(userA, userB, messageType);
			transactionLayer.callBye(byeMessage, this.myAddress, puertoB);
		}
	}
	
	// Temporizadores
	private void ourTimer() {
		Timer timer = new Timer();
		int time = 2;
		TimerTask task = new TimerTask() {
			//int counter=0;
		    @Override
		    public void run() {
		        // Coloca la acción que deseas ejecutar aquí
		        try {
		        
		        	if (isDisconnected) {
		        		commandRegister("", state);
		        	}
		        	else {
		        		timer.cancel();
		        	}
		        	
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    }
		};

		timer.scheduleAtFixedRate(task, 0, time*1000);
	}
	
	private void ringingTimer() {
		Timer timer = new Timer();
		int time = 2;
		TimerTask task = new TimerTask() {
			int counter=0;
		    @Override
		    public void run() {
		    	//System.err.println(Message.toString());
		        if (Message.equals("s") || Message.equals("n")) {
		        	//System.err.println("RINGING TIMER BUG");
		       		timer.cancel();
		       	}
		       	else if(counter>1000) {
		        	try {
		        		commandTimeout("");
		        		timer.cancel();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		        } else {
		       		counter=counter+time;
		       	}
		    }
		};

		timer.scheduleAtFixedRate(task, 0, time*1000);
	}
	
	private void ACKTimer2(SIPMessage sipMessage) {
		
		if(sipMessage instanceof BusyHereMessage) {
			
			BusyHereMessage busyHereMessage = (BusyHereMessage) sipMessage;
			Timer timer = new Timer();
			int time = 2;
			TimerTask task = new TimerTask() {
				int counter=0;
			    @Override
			    public void run() {
			    	try {
			    		// Si recibimos el ACK cancelamos el timer
			    		if(isACKReceived) {
			    			timer.cancel();
			    		}
			    		else if(isACKReceived == false && counter != 0)
			    		{
			    			String messageType = busyHereMessage.toStringMessage();
			    			showArrowInMessage(proxyName, userA , messageType);
			    			transactionLayer.callBusyHere(busyHereMessage);
			    		}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			       	counter=counter+time;
			    }
			};

			timer.scheduleAtFixedRate(task, 0, time*100);
		}
		
		else if(sipMessage instanceof RequestTimeoutMessage) {
			RequestTimeoutMessage timeoutMessage = (RequestTimeoutMessage) sipMessage;
			Timer timer = new Timer();
			int time = 2;
			TimerTask task = new TimerTask() {
				int counter=0;
			    @Override
			    public void run() {
			        
			    	try {
			    		if(isACKReceived) {
			    			timer.cancel();
			    		}
			    		else if(isACKReceived == false && counter != 0)
			    		{
			    			String messageType = timeoutMessage.toStringMessage();
			    			showArrowInMessage(proxyName, userA , messageType);
			    			transactionLayer.callTimeout(timeoutMessage);
			    		}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			       	counter=counter+time;
			    }
			};

			timer.scheduleAtFixedRate(task, 0, time*1000);
		}	
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
					isDisconnected = true;
					System.out.println("user expired!");
					
					// Nos registramos de nuevo
					/*try {
						//commandRegister("", state);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}*/
					timer.cancel();	
	    		}
	    	}
		};
		timer.scheduleAtFixedRate(task, 0, 1000);
	}

	private void runVitextClient() throws IOException {
		//vitextClient = Runtime.getRuntime().exec("xterm -e vitext/vitextclient -p 5000 239.1.2.3");
	}

	private void stopVitextClient() {
		if (vitextClient != null) {
			vitextClient.destroy();
		}
	}

	private void runVitextServer() throws IOException {
		//vitextServer = Runtime.getRuntime().exec("xterm -iconic -e vitext/vitextserver -r 10 -p 5000 vitext/1.vtx 239.1.2.3");
	}

	private void stopVitextServer() {
		if (vitextServer != null) {
			vitextServer.destroy();
		}
	}
	
	
	/// METODOS AUXILIARES
	/**
	 * Muestra con una flecha el significado del mensaje, pero tambien muestra el mensaje
	 * completo si esta a true el parametro firstline.
	 * @param from
	 * @param to
	 * @param messageType
	 */
	private void showArrowInMessage(String from, String to, String messageType) { 
		String commInfo = messageType.substring(0,messageType.indexOf(" "))
				+ " " + from + " -> " + to;
		String[] splittedMessage = messageType.split("\n", 2);
		String messageToPrint;
		messageToPrint = ((this.firstLine.equals("false")) ? splittedMessage[0]: messageType);
		System.out.println(commInfo);
		System.out.println(messageToPrint + "\n");
	}
	
	/**
	 * Extrae del mensaje invite las vias (cuando UA llamado recibe esto, entonces conten-
	 * tra todos todas las vías por las que ha pasado...)
	 * @param inviteMessage
	 */
	private void addRecordRoute(SIPMessage sipMessage) {
		
		if (sipMessage instanceof InviteMessage) {
			InviteMessage inviteMessage = (InviteMessage) sipMessage;
			
			if(inviteMessage.getRecordRoute()!=null)
			{
				// Solo de los UAs
				looseRoutingString = inviteMessage.getVias().get(0);
				this.isLooseRouting = true;
			}
			
		} 
		else if(sipMessage instanceof OKMessage)
		{
			OKMessage okMessage = (OKMessage) sipMessage;
			if(okMessage.getRecordRoute()!=null)
			{
				looseRoutingString = okMessage.getVias().get(0);
				this.isLooseRouting = true;
			}
		}
		/*else if(sipMessage instanceof ACKMessage)
		{
			ACKMessage ackMessage = (ACKMessage) sipMessage;
			if(ackMessage.getRoute()!=null)
			{
				// Solo de los UAs
				looseRoutingString = ackMessage.getVias().get(0);
				this.isLooseRouting = true;
			}
		}
		else if(sipMessage instanceof ByeMessage)
		{
			ByeMessage byeMessage = (ByeMessage) sipMessage;
			if(byeMessage.getRoute()!=null)
			{
				// Solo de los UAs
				looseRoutingString = byeMessage.getVias().get(0);
				this.isLooseRouting = true;
			}
		}*/
		
	}

}
