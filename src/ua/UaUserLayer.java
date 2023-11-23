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
import mensajesSIP.RingingMessage;
import mensajesSIP.BusyHereMessage;
import mensajesSIP.ByeMessage;

public class UaUserLayer {
	
	private static final int REGISTERING = 1;
	private static final int TRYING = 2;
	private static final int WAITING = 3;
	private String Message = "";
	
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
		System.out.println("Received INVITE from " + inviteMessage.getFromName());
		
		userA = inviteMessage.getFromName();
		userB = inviteMessage.getToName();
		
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
		
		String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		String other = "";
		// Para mostrar el mensaje completo o solo la primera linea
		if(okMessage.getToName().equals(userURIstring))
		{
			other = "proxy";
		}
		else
		{
			other = okMessage.getToName();
		}
		String commInfo = okMessage.toStringMessage().substring(0,okMessage.toStringMessage().indexOf(" "))
				+ " " + other + " -> " + userURIstring;
		String[] splittedMessage = okMessage.toStringMessage().split("\n", 2);
		String messageToPrint;
		messageToPrint = ((this.firstLine.equals("true")) ? splittedMessage[0]: okMessage.toStringMessage());
		System.out.println(commInfo);
		System.out.println(messageToPrint + "\n");
		//
		
		this.Message = "OK";
		if(state == CALLING || state == PROCEEDING_A) {
			state = TERMINATED_A;
		}
		
		
		if(okMessage.getFromName().equalsIgnoreCase(userURIstring)) {
			
			prompt();
			
		}
		
		runVitextServer();
		
	}
	
	// 404 NOT FOUND (usuario que no existe cuando lo introducimos por teclado)
	public void onNotFoundReceived(NotFoundMessage notFoundMessage) throws IOException {
		// Para mostrar el mensaje completo o solo la primera linea
		String[] splittedMessage = notFoundMessage.toStringMessage().split("\n", 2);
		String messageToPrint;
		messageToPrint = ((this.firstLine.equals("true")) ? splittedMessage[0]: notFoundMessage.toStringMessage());
		System.out.println(messageToPrint + "\n");
		//
		
		this.Message = "NOT FOUND";
		state = COMPLETED_A;
		String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		if(notFoundMessage.getFromName().equalsIgnoreCase(userURIstring)) {
			prompt();
		}
		runVitextServer();
	}
	
	// 100 TRYING
	public void onTryingReceived(TryingMessage tryingMessage) throws IOException {
		// Para mostrar el mensaje completo o solo la primera linea
		String[] splittedMessage = tryingMessage.toStringMessage().split("\n", 2);
		String messageToPrint;
		messageToPrint = ((this.firstLine.equals("true")) ? splittedMessage[0]: tryingMessage.toStringMessage());
		System.out.println(messageToPrint + "\n");
		//
		this.Message = "TRYING";
		state = PROCEEDING_A;
		String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		if(tryingMessage.getFromName().equalsIgnoreCase(userURIstring)) {
			prompt();
		}
		runVitextServer();
	}
	
	// 180 RINING
	public void onRingingReceived(RingingMessage ringingMessage) throws IOException {
		// Para mostrar el mensaje completo o solo la primera linea
		String[] splittedMessage = ringingMessage.toStringMessage().split("\n", 2);
		String messageToPrint;
		messageToPrint = ((this.firstLine.equals("true")) ? splittedMessage[0]: ringingMessage.toStringMessage());
		System.out.println(messageToPrint + "\n");
		//
		this.Message = "RINGING";
		state = PROCEEDING_A;
		
		String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		if(ringingMessage.getFromName().equalsIgnoreCase(userURIstring)) {
			prompt();
		}
		runVitextServer();
		//ringingTimer();
	}
	
	public void onBusyHereReceived(BusyHereMessage busyHereMessage) throws IOException {
		// Para mostrar el mensaje completo o solo la primera linea
		String[] splittedMessage = busyHereMessage.toStringMessage().split("\n", 2);
		String messageToPrint;
		messageToPrint = ((this.firstLine.equals("true")) ? splittedMessage[0]: busyHereMessage.toStringMessage());
		System.out.println(messageToPrint + "\n");
		//
		this.Message = "BUSY";
		state = COMPLETED_A;
		
		String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		if(busyHereMessage.getFromName().equalsIgnoreCase(userURIstring)) {
			prompt();
		}
		
		runVitextServer();
	}
	
	// 503 receving
	public void onServiceUnavailableReceived(ServiceUnavailableMessage serviceUnavailableMessage) throws IOException {
		// Para mostrar el mensaje completo o solo la primera linea
		String[] splittedMessage = serviceUnavailableMessage.toStringMessage().split("\n", 2);
		String messageToPrint;
		messageToPrint = ((this.firstLine.equals("true")) ? splittedMessage[0]: serviceUnavailableMessage.toStringMessage());
		System.out.println(messageToPrint + "\n");
		//
		this.Message = "BUSY";
		state = COMPLETED_A;
		
		String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		if(serviceUnavailableMessage.getFromName().equalsIgnoreCase(userURIstring)) {
			prompt();
		}
		
		runVitextServer();
	}
	
	// BYE
	public void onByeReceived(ByeMessage byeMessage) throws IOException {
		// Para mostrar el mensaje completo o solo la primera linea
		String[] splittedMessage = byeMessage.toStringMessage().split("\n", 2);
		String messageToPrint;
		messageToPrint = ((this.firstLine.equals("true")) ? splittedMessage[0]: byeMessage.toStringMessage());
		System.out.println(messageToPrint + "\n");
		//
		this.Message = "BUSY";
		state = COMPLETED_A;
		
		String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		if(byeMessage.getFromName().equalsIgnoreCase(userURIstring)) {
			prompt();
		}
		
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
		try {
			commandRegister("", state);
			
			//ourTimer();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void ourTimer() {
		Timer timer = new Timer();
		int time = 2;
		TimerTask task = new TimerTask() {
			int counter=0;
		    @Override
		    public void run() {
		        // Coloca la acción que deseas ejecutar aquí
		        try {
		        	if (Message.length()==0) {
		        		if(counter>=2) {
		            		commandRegister("", state);
		            		System.out.println("El valor del temporizador es: " + counter +"s");
		            	}
						counter=counter+time;
		        	}
		        	
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		        if(counter>10) {
		        	try {
		        		System.out.println(commandTimeout("").toStringMessage());
		        		timer.cancel();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		        }
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
		        // Coloca la acción que deseas ejecutar aquí
		        if (Message.equals("OK") || Message.equals("BUSY")) {
		       		timer.cancel();
		       	}
		       	else {
		       		
		       		counter=counter+time;
		       	}
		        if(counter>10) {
		        	try {
		        		System.out.println(commandTimeout("").toStringMessage());
		        		timer.cancel();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
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
			if(line.length()==1)
			{
				commandOK("");
			}
		}
		
		else if(line.startsWith("n"))
		{
			if(line.length()==1)
			{
				commandBusy("");
			}
		}
		else {
			System.out.println("Bad command");
		}
	}

	private void commandInvite(String line, int state) throws IOException {
		stopVitextServer();
		stopVitextClient();
		
		this.state = CALLING;
		
		System.out.println("Inviting...");
		
		
		runVitextClient();
		String callId = UUID.randomUUID().toString();
		
		// El nombre del llamado
		String nameToSend = line.substring(line.indexOf(" ")).trim();
		
		// El nombre del llamante
		String userURIString = userURI.substring(0, userURI.indexOf("@"));
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
			System.out.print(inviteMessage.toStringMessage());
			prompt();
		}
		
		transactionLayer.call(inviteMessage);
	}
	
	private void commandRegister(String line, int state) throws IOException {
		stopVitextServer();
		stopVitextClient();
		
		runVitextClient();

		String callId = UUID.randomUUID().toString();
		String userURIString = userURI.substring(0, userURI.indexOf("@"));
		
		RegisterMessage registerMessage = new RegisterMessage();
	
		registerMessage.setDestination("sip:proxy");
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
		String commInfo = registerMessage.toStringMessage().substring(0,registerMessage.toStringMessage().indexOf(" "))
				+ " " + userURIString + " -> " + registerMessage.getDestination().toString();
		String[] splittedMessage = registerMessage.toStringMessage().split("\n", 2);
		String messageToPrint;
		messageToPrint = ((this.firstLine.equals("true")) ? splittedMessage[0]: registerMessage.toStringMessage());
		System.out.println(commInfo);
		System.out.println(messageToPrint + "\n");
		//
		
		transactionLayer.callRegister(registerMessage);
	
	}
	
	private RequestTimeoutMessage commandTimeout(String line) throws IOException {
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
		
		
		String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		if(timeout.getToName().equalsIgnoreCase(userURIstring)) {
			System.out.print(timeout.toStringMessage());
			prompt();
		}
		
		return timeout;
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
		
		String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		if(ringingMessage.getToName().equalsIgnoreCase(userURIstring)) {
			//System.out.print(ringingMessage.toStringMessage());
			//prompt();
		}
		
		transactionLayer.callRinging(ringingMessage);
		
	}
	
	// 200 OK message (lo envia el llamado)
	private void commandOK(String line) throws IOException {
		
		String callId = UUID.randomUUID().toString();
		
		state = TERMINATED_B;
		
		OKMessage okMessage = new OKMessage();	
		
		okMessage.setVias(new ArrayList<String>(Arrays.asList(this.myAddress + ":" + this.listenPort)));
		okMessage.setToName(userB);
		okMessage.setToUri("sip:"+userB+"@SMA");
		okMessage.setFromName(userA);
		okMessage.setFromUri("sip:"+userA+"@SMA");
		okMessage.setCallId(callId);
		okMessage.setcSeqNumber("1");
		okMessage.setcSeqStr("INVITE");
		okMessage.setContact(this.myAddress + ":" + this.listenPort);
		
		String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		if(okMessage.getToName().equalsIgnoreCase(userURIstring)) {
			System.out.print(okMessage.toStringMessage());
			prompt();
		}
		
		transactionLayer.callOK(okMessage);
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
		
		String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		if(busyHereMessage.getToName().equalsIgnoreCase(userURIstring)) {
			System.out.print(busyHereMessage.toStringMessage());
			if(state!=IDLE) {
				prompt();
			}
			
		}
		
		transactionLayer.callBusyHere(busyHereMessage);
	}
	
	// BYE BYE
	private void commandBye(String line) throws IOException {
		
		String callId = UUID.randomUUID().toString();
		
		ByeMessage byeMessage = new ByeMessage();	
		
		byeMessage.setVias(new ArrayList<String>(Arrays.asList(this.myAddress + ":" + this.listenPort)));
		byeMessage.setToName(userB);
		byeMessage.setToUri("sip:"+userB+"@SMA");
		byeMessage.setFromName(userA);
		byeMessage.setFromUri("sip:"+userA+"@SMA");
		byeMessage.setCallId(callId);
		byeMessage.setcSeqNumber("1");
		byeMessage.setcSeqStr("INVITE");
		
		String userURIstring = userURI.substring(0, userURI.indexOf("@"));
		if(byeMessage.getToName().equalsIgnoreCase(userURIstring)) {
			System.out.print(byeMessage.toStringMessage());
			if(state!=IDLE) {
				prompt();
			}
			
		}
		
		transactionLayer.callBye(byeMessage);
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

}
