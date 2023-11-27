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
import mensajesSIP.ServiceUnavailableMessage;
import mensajesSIP.TryingMessage;
import proxy.ProxyWhiteListArray;
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
	private int puertoA; // llamante
	private int puertoB; // llamado

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
		
		ArrayList<String> vias = inviteMessage.getVias();
		String origin = vias.get(0);
		String[] originParts = origin.split(":");
		
		int originPort = Integer.parseInt(originParts[1]);
		
		puertoB = originPort;
		
		System.out.println("El puerto del A es: " + listenPort);
		System.out.println("El puerto del B es: " + puertoB);
		
		//System.out.println(inviteMessage.toStringMessage());

		if(state == TERMINATED_A)
		{
			commandBusy("");
			return;
		}
		
		state = PROCEEDING_B;
		runVitextServer();
		
		
		/*String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		if(inviteMessage.getToName().equalsIgnoreCase(userURIstring)) {	
		}*/
	
		commandRinging("");
		System.out.println("Introduce an s to accept the call or deny with a n...");
			
		prompt();
		
	}
	
	// OK MESSAGE RECEIVED
	public void onOKReceived(OKMessage okMessage) throws IOException {
		this.Message = "OK";
		
		String userURIString = userURI.substring(0, userURI.indexOf("@"));
		//String other = "";
		// Para mostrar el mensaje completo o solo la primera linea
		/*if(okMessage.getToName().equals(userURIString))
		{
			other = proxyName;
		}
		else
		{
			other = okMessage.getToName();
		}*/
		//
		
		//
		
		if(state == CALLING || state == PROCEEDING_A) {
			state = TERMINATED_A;
			
			String messageType = okMessage.toStringMessage();
			showArrowInMessage(proxyName, userURIString, messageType);
			
			//Se establece la llamada y comienza a enviarse ACK
			commandACK(this.listenPort, puertoB);
			System.out.println("Type BYE to finish");
			//ACKTimer();
			//puertoB = okMessage.getVias();
			
		}
		else if (state == TERMINATED_A) {
			String messageType = okMessage.toStringMessage();
			showArrowInMessage(userB, userURIString, messageType);
		}
		
		if(okMessage.getFromName().equalsIgnoreCase(userURIString)) {
			prompt();
		}
		
		
		runVitextServer();
	}

	
	
	// 404 NOT FOUND (usuario que no existe cuando lo introducimos por teclado)
	// O cuando se nos expira el campo expires...
	public void onNotFoundReceived(NotFoundMessage notFoundMessage) throws IOException {
		this.Message = "NOT FOUND";
		String algo = notFoundMessage.getExpires();
		System.out.println(algo);
		userA = notFoundMessage.getFromName();
		userB = notFoundMessage.getToName();
		
		// Para mostrar el mensaje completo o solo la primera linea
		String messageType = notFoundMessage.toStringMessage();
		showArrowInMessage(proxyName,userFrom, messageType);
		//
		
		commandACK();
		
		// si usuario ha expirado... nos volvemos a registrar
		/*if(notFoundMessage.getExpires().equals("0"))
		{
			autoRegistering();
			return;
		}*/

		state = COMPLETED_A;
		
		/*String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		if(notFoundMessage.getFromName().equalsIgnoreCase(userURIstring)) {
			prompt();
		}*/
	
		runVitextServer();
	}
	
	// 100 TRYING
	public void onTryingReceived(TryingMessage tryingMessage) throws IOException {
		this.Message = "TRYING";
		// Para mostrar el mensaje completo o solo la primera linea
		String messageType = tryingMessage.toStringMessage();
		showArrowInMessage(proxyName, userFrom, messageType);
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
		String originAddress = originParts[0];
		
		int originPort = Integer.parseInt(originParts[1]);
		
		puertoB = originPort;
		
		System.out.println("El puerto del A es: " + listenPort);
		System.out.println("El puerto del B es: " + puertoB);
				
		state = PROCEEDING_A;
		
		/*String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		if(ringingMessage.getFromName().equalsIgnoreCase(userURIstring)) {
			//prompt();
		}*/
		
		runVitextServer();
		
	}
	
	// 406 Request Timeout
	public void onRequestTimeoutReceived(RequestTimeoutMessage timeoutMessage) throws IOException {
		String messageType = timeoutMessage.toStringMessage();
		showArrowInMessage(proxyName, userFrom, messageType);
		
		// Enviamos un ACK al mensaje de error
		commandACK();
		
		state = COMPLETED_A;
		
	
		runVitextServer();
	}
	
	// 486 Busy 
	public void onBusyHereReceived(BusyHereMessage busyHereMessage) throws IOException {
		this.Message = "BUSY";
		String messageType = busyHereMessage.toStringMessage();
		showArrowInMessage(proxyName, userFrom, messageType);
		//
		
		// Enviamos un ACK al mensaje de error
		//commandACK();
		
		state = COMPLETED_A;
		
		/*String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		if(busyHereMessage.getFromName().equalsIgnoreCase(userURIstring)) {
			//prompt();
		}*/
		
		runVitextServer();
	}
	
	// 503 receving
	public void onServiceUnavailableReceived(ServiceUnavailableMessage serviceUnavailableMessage) throws IOException {
		this.Message = "BUSY";
		
		String messageType = serviceUnavailableMessage.toStringMessage();
		showArrowInMessage(proxyName, userTo, messageType);
		
		state = COMPLETED_A;
		
		String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		if(serviceUnavailableMessage.getFromName().equalsIgnoreCase(userURIstring)) {
			prompt();
		}
		
		runVitextServer();
	}
	
	// ACK
	public void onACKReceived(ACKMessage ACKMessage) throws IOException {
		String messageType = ACKMessage.toStringMessage();
		showArrowInMessage(proxyName, ACKMessage.getToName(), messageType);
		
		/*ACKMessage.setVias(new ArrayList<String>(Arrays.asList(this.myAddress + ":" 
				+ this.listenPort + ":" + puertoB)));
		
		ArrayList<String> vias = ACKMessage.getVias();
		String destination = vias.get(0);
		String[] destinationParts = destination.split(":");

		int destiantionPort = Integer.parseInt(destinationParts[2]);*/
		
		/*String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		if(ACKMessage.getFromName().equalsIgnoreCase(userURIstring)) {
			prompt();
		}*/
		if(state == TERMINATED_B || state == TERMINATED_A)
		{
			messageType = ACKMessage.toStringMessage();
			showArrowInMessage(ACKMessage.getFromName(), ACKMessage.getToName(), messageType);
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
		String originAddress = originParts[0];
		
		int originPort = Integer.parseInt(originParts[1]);
	
		String messageType = byeMessage.toStringMessage();
		
		//OKMessage okMessage = new OKMessage();
		
		if(this.listenPort == originPort)
		{
			state = TERMINATED_A;
			showArrowInMessage(byeMessage.getToName(), byeMessage.getFromName(), messageType);
			commandOK("");
		}
		else
		{
			state = TERMINATED_A;
			showArrowInMessage(byeMessage.getFromName(), byeMessage.getToName(), messageType);
			commandOK("");
		}
	
		/*String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		if(byeMessage.getFromName().equalsIgnoreCase(userURIstring)) {
			prompt();
		}*/
		
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

	private void ourTimer() {
		Timer timer = new Timer();
		int time = 2;
		TimerTask task = new TimerTask() {
			//int counter=0;
		    @Override
		    public void run() {
		        // Coloca la acción que deseas ejecutar aquí
		        try {
		        	if (Message.length()==0) {
		        		commandRegister("", state);
		        		//if(counter>=2) {
		            		//System.out.println("El valor del temporizador es: " + counter +"s");
		            	//}
						//counter=counter+time;
		        	}
		        	
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		        /*if(counter>10) {
		        	try {
		        		System.out.println(commandTimeout("").toStringMessage());
		        		timer.cancel();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		        }*/
		    }
		};

		// Programar el Timer para que se ejecute cada 2 segundos
		timer.scheduleAtFixedRate(task, 0, time*1000);
	}
	
	private void ringingTimer() {
		Timer timer = new Timer();
		System.out.println("Ringing... ");
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
		       	else if(counter>10) {
		        	try {
		        		//System.out.println(commandTimeout("").toStringMessage());
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

		// Programar el Timer para que se ejecute cada 2 segundos
		timer.scheduleAtFixedRate(task, 0, time*1000);
	}
	// TODO: jacer el PUT OTIEMR OTRA VEZ
	private void ACKTimer() {
		Timer timer = new Timer();
		System.out.println("ACK... ");
		int time = 2;
		int myPort = this.listenPort;
		int otherPort = puertoB;
		TimerTask task = new TimerTask() {
			int counter=0;
		    @Override
		    public void run() {
		        // Coloca la acción que deseas ejecutar aquí
		    	try {
					commandACK(myPort,otherPort);
					System.out.println("Type BYE to finish");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		        if (Message.equals("BYE")) {
		       		timer.cancel();
		       	}
		       	else {
		       		
		       		counter=counter+time;
		       	}
		    }
		};

		// Programar el Timer para que se ejecute cada 2 segundos
		timer.scheduleAtFixedRate(task, 0, time*1000);
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
	}
	
	private void promptTerminatedB() {
		System.out.println(this.userURI);
		System.out.println("Conexión establecida B");
	}
	

	private void command(String line) throws IOException {
		if (line.startsWith("INVITE")) {
			commandInvite(line, state);
		} 
		else if (line.startsWith("REGISTER")) {
			commandRegister(line, state);
			
		} 
		else if(line.startsWith("s"))
		{
			this.Message = "s";
			state = TERMINATED_B;
			commandOK("");
		}
		
		else if(line.startsWith("n"))
		{
			this.Message = "n";
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
			System.out.println("Bad command");
		}
	}

	private void commandInvite(String line, int state) throws IOException {
		stopVitextServer();
		stopVitextClient();
		
		//System.out.println("Inviting...");
		
		runVitextClient();
		String callId = UUID.randomUUID().toString();
		
		// El nombre del llamado
		String nameToSend = line.substring(line.indexOf(" ")).trim();
		userTo = nameToSend;
		
		// El nombre del llamante
		String userURIString = userURI.substring(0, userURI.indexOf("@"));
		userFrom = userURIString;
		//System.out.println("Sending to:" + nameToSend);
		
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
		
		String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		if(inviteMessage.getFromName().equalsIgnoreCase(userURIstring)) {
			//System.out.print(inviteMessage.toStringMessage());
			//prompt();
		}
		
		String messageType = inviteMessage.toStringMessage();
		showArrowInMessage(userURIString, proxyName, messageType);
		
		this.state = CALLING;
		
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
		
		/*String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		if(registerMessage.getFromName().equalsIgnoreCase(userURIstring)) {
			
			prompt();
		}*/
		// Para mostrar el mensaje completo o solo la primera linea
		String messageType = registerMessage.toStringMessage();
		showArrowInMessage(userURIString, proxyName, messageType);
		//
		
		transactionLayer.callRegister(registerMessage);
	
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
		timeout.setcSeqStr("REGISTER");
		timeout.setContentLength(timeout.toStringMessage().getBytes().length);
		
		
		/*String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		if(timeout.getToName().equalsIgnoreCase(userURIstring)) {
			System.out.print(timeout.toStringMessage());
			prompt();
		}*/
		
		String messageType = timeout.toStringMessage();
		showArrowInMessage(userB, proxyName, messageType);
		
		transactionLayer.callTimeout(timeout);
		//return timeout;
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
		ringingMessage.setcSeqStr("REGISTER");
		ringingMessage.setContentLength(ringingMessage.toStringMessage().getBytes().length);
		
		/*String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		if(ringingMessage.getToName().equalsIgnoreCase(userURIstring)) {
			//System.out.print(ringingMessage.toStringMessage());
			//prompt();
		}*/
		
		String messageType = ringingMessage.toStringMessage();
		showArrowInMessage(userB, proxyName, messageType);
		
		prompt();
	
		transactionLayer.callRinging(ringingMessage);
		ringingTimer();
		
	}
	
	// 200 OK message (lo envia el llamado)
	private void commandOK(String line) throws IOException {
		
		String callId = UUID.randomUUID().toString();
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
		
		/*if(state == TERMINATED_A) {
			userB = userA;
			userA = userURIstring;
		}*/
		
		/*String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		if(okMessage.getToName().equalsIgnoreCase(userURIstring)) {
			//System.out.print(okMessage.toStringMessage());
			
		}*/
		
		
		
		prompt();
		
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
		
	}
	
	// 486 BUSY
	private void commandBusy(String line) throws IOException {
		
		String callId = UUID.randomUUID().toString();
		
		BusyHereMessage busyHereMessage = new BusyHereMessage();	
		
		busyHereMessage.setVias(new ArrayList<String>(Arrays.asList(this.myAddress + ":" + this.listenPort)));
		busyHereMessage.setToName(userB);
		busyHereMessage.setToUri("sip:"+userB+"@SMA");
		busyHereMessage.setFromName(userA);
		busyHereMessage.setFromUri("sip:"+userA+"@SMA");
		busyHereMessage.setCallId(callId);
		busyHereMessage.setcSeqNumber("1");
		busyHereMessage.setcSeqStr("INVITE");
		
		/*String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		if(busyHereMessage.getToName().equalsIgnoreCase(userURIstring)) {
			//System.out.print(busyHereMessage.toStringMessage());
			if(state!=IDLE) {
				//prompt();
			}
			
		}*/
		
		String messageType = busyHereMessage.toStringMessage();
		showArrowInMessage(userB, proxyName, messageType);
		
		prompt();
		
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
		ACKMessage.setcSeqStr("INVITE");
		ACKMessage.setDestination("sip:"+userA+"@SMA");
		
		/*String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		if(ACKMessage.getToName().equalsIgnoreCase(userURIstring)) {
			System.out.print(ACKMessage.toStringMessage());
			if(state!=IDLE) {
				prompt();
			}
			
		}*/
		
		String messageType = ACKMessage.toStringMessage();
		showArrowInMessage(userA, userB, messageType);
		
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
		ACKMessage.setcSeqStr("INVITE");
		ACKMessage.setDestination("sip:"+userA+"@SMA");
		
		String messageType = ACKMessage.toStringMessage();
		showArrowInMessage(userA, proxyName, messageType);
		transactionLayer.callACK(ACKMessage);
	}
	
	// BYE BYE
	private void commandBye(String line) throws IOException {
		
		String callId = UUID.randomUUID().toString();
		
		ByeMessage byeMessage = new ByeMessage();	
		
		String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		
		//System.out.println(userURIstring);
		//System.out.println(userA);
		//System.out.println(userB);
		if(userURIstring.equals(userB)) {
			userB = userA;
			userA = userURIstring;
			//System.out.println(userA);
			//System.out.println(userB);
		}
		
		
		/*if(userURIstring == ) {
			userA = "alice";
			userB = "bob";
		}
		else if (listenPort == 9100) {
			userA = "bob";
			userB = "alice";
		}*/
		//String tempUserA = "";
		//String tempUserB = "";
		
		// Si 9100 == 
		/*System.out.println("Puerto listen: " + this.listenPort);
		System.out.println("PuertoB : " + puertoB);
		System.out.println("User A : " + userA);
		System.out.println("User B : " + userB);*/
		
		//tempUserA = userA; 
		
		//userA = userB;
		
		//userB = tempUserA;
		//System.out.println("User A : " + userA);
		//System.out.println("User B : " + userB);
		
		/*if(this.listenPort == puertoB)
		{
	
		}*/
		/*else
		{
			tempUserA = userA;
			userA = userB;
			userB = tempUserA;
		}*/
		
		byeMessage.setDestination("sip:"+userB+"@SMA");
		byeMessage.setVias(new ArrayList<String>(Arrays.asList(this.myAddress + ":" + this.listenPort)));
		byeMessage.setToName(userB);
		byeMessage.setToUri("sip:"+userB+"@SMA");
		byeMessage.setFromName(userA);
		byeMessage.setFromUri("sip:"+userA+"@SMA");
		byeMessage.setCallId(callId);
		byeMessage.setcSeqNumber("1");
		byeMessage.setcSeqStr("INVITE");
		
		String messageType = byeMessage.toStringMessage();
		showArrowInMessage(userA, userB, messageType);
		
		transactionLayer.callBye(byeMessage, this.myAddress, puertoB);
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
	private void showArrowInMessage(String from, String to, String messageType) { 
		String commInfo = messageType.substring(0,messageType.indexOf(" "))
				+ " " + from + " -> " + to;
		String[] splittedMessage = messageType.split("\n", 2);
		String messageToPrint;
		messageToPrint = ((this.firstLine.equals("true")) ? splittedMessage[0]: messageType);
		System.out.println(commInfo);
		System.out.println(messageToPrint + "\n");
	}

}
