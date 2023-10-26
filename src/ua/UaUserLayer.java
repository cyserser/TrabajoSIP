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

public class UaUserLayer {
	private static final int IDLE = 0;
	private static final int REGISTERING = 1;
	private int state = REGISTERING;
	private String Message = "";
	private int counter=0;

	public static final ArrayList<Integer> RTPFLOWS = new ArrayList<Integer>(
			Arrays.asList(new Integer[] { 96, 97, 98 }));

	private UaTransactionLayer transactionLayer;

	private String myAddress = FindMyIPv4.findMyIPv4Address().getHostAddress();
	private int rtpPort;
	private int listenPort;
	private String userURI;

	private Process vitextClient = null;
	private Process vitextServer = null;

	public UaUserLayer(String userURI, int listenPort, String proxyAddress, int proxyPort)
			throws SocketException, UnknownHostException {
		this.transactionLayer = new UaTransactionLayer(listenPort, proxyAddress, proxyPort, this);
		this.listenPort = listenPort;
		this.rtpPort = listenPort + 1;
		this.userURI = userURI;
	}

	public void onInviteReceived(InviteMessage inviteMessage) throws IOException {
		System.out.println("Received INVITE from " + inviteMessage.getFromName());
		runVitextServer();
	}
	
	public void onRegisterReceived(RegisterMessage registerMessage) throws IOException {
		System.out.println("Received REGISTER from " + registerMessage.getFromName());
		//state = IDLE;
		runVitextServer();
	}
	public void onOKReceived(OKMessage okMessage) throws IOException {
		System.out.println(okMessage.toStringMessage());
		this.Message = "OK";
		//state = IDLE;
		runVitextServer();
	}
	public void onNotFoundReceived(NotFoundMessage notFoundMessage) throws IOException {
		System.out.println(notFoundMessage.toStringMessage());
		this.Message = "NOT FOUND";
		//state = IDLE;
		runVitextServer();
	}

	public void startListeningNetwork() {
		transactionLayer.startListeningNetwork();
	}

	public void startListeningKeyboard() {
		
		try (Scanner scanner = new Scanner(System.in)) {
			while (true) {
				
				prompt();
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
			commandRegister("");
			Timer timer = new Timer();
			
			int time = 2;
	        TimerTask task = new TimerTask() {
	            @Override
	            public void run() {
	                // Coloca la acción que deseas ejecutar aquí
	                try {
	                	if (Message.length()==0) {
	                		if(counter>=2) {
		                		commandRegister("");
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
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void prompt() {
		System.out.println("");
		switch (state) {
		case IDLE:
			promptIdle();
			break;
		case REGISTERING:
			promptRegistering();
			break;
		default:
			throw new IllegalStateException("Unexpected state: " + state);
		}
		System.out.print("> ");
	}

	private void promptIdle() {
		System.out.println("INVITE xxx");
	}
	
	private void promptRegistering() {
		System.out.println("REGISTER xxx");
	}

	private void command(String line) throws IOException {
		if (line.startsWith("INVITE")) {
			commandInvite(line);
		} 
		if (line.startsWith("REGISTER")) {
			commandRegister(line);
		} 
		else {
			System.out.println("Bad command");
		}
	}

	private void commandInvite(String line) throws IOException {
		stopVitextServer();
		stopVitextClient();
		
		System.out.println("Inviting...");

		runVitextClient();

		String callId = UUID.randomUUID().toString();

		SDPMessage sdpMessage = new SDPMessage();
		sdpMessage.setIp(this.myAddress);
		sdpMessage.setPort(this.rtpPort);
		sdpMessage.setOptions(RTPFLOWS);

		InviteMessage inviteMessage = new InviteMessage();
		inviteMessage.setDestination("sip:bob@SMA");
		inviteMessage.setVias(new ArrayList<String>(Arrays.asList(this.myAddress + ":" + this.listenPort)));
		inviteMessage.setMaxForwards(70);
		inviteMessage.setToName("Bob");
		inviteMessage.setToUri("sip:bob@SMA");
		inviteMessage.setFromName("Alice");
		inviteMessage.setFromUri("sip:alice@SMA");
		inviteMessage.setCallId(callId);
		inviteMessage.setcSeqNumber("1");
		inviteMessage.setcSeqStr("INVITE");
		inviteMessage.setContact(myAddress + ":" + listenPort);
		inviteMessage.setContentType("application/sdp");
		inviteMessage.setContentLength(sdpMessage.toStringMessage().getBytes().length);
		inviteMessage.setSdp(sdpMessage);

		transactionLayer.call(inviteMessage);
	}
	
	private void commandRegister(String line) throws IOException {
		stopVitextServer();
		stopVitextClient();
		
		System.out.println("Registering...");

		runVitextClient();

		String callId = UUID.randomUUID().toString();
		String userURIString = userURI.substring(0, userURI.indexOf("@"));
		
		RegisterMessage registerMessage = new RegisterMessage();
	
		
		registerMessage.setDestination("sip:bob@SMA");
		registerMessage.setVias(new ArrayList<String>(Arrays.asList(this.myAddress + ":" + this.listenPort)));
		registerMessage.setMaxForwards(70);
		registerMessage.setToName("Bob");
		registerMessage.setToUri("sip:bob@SMA");
		registerMessage.setFromName(userURIString);
		registerMessage.setFromUri("sip:"+userURI);
		registerMessage.setCallId(callId);
		registerMessage.setcSeqNumber("1");
		registerMessage.setcSeqStr("REGISTER");
		registerMessage.setContact(myAddress + ":" + listenPort);
		registerMessage.setAuthorization("Authorization");
		registerMessage.setExpires("2");
		registerMessage.setContentLength(registerMessage.toStringMessage().getBytes().length);

		
		transactionLayer.callR(registerMessage);
	
	}
	
	private RequestTimeoutMessage commandTimeout(String line) throws IOException {
		stopVitextServer();
		stopVitextClient();
		
		System.out.println("Registering...");

		runVitextClient();

		String callId = UUID.randomUUID().toString();
		String userURIString = userURI.substring(0, userURI.indexOf("@"));
		
		RequestTimeoutMessage timeout = new RequestTimeoutMessage();
	
		timeout.setVias(new ArrayList<String>(Arrays.asList(this.myAddress + ":" + this.listenPort)));
		timeout.setToName("Bob");
		timeout.setToUri("sip:bob@SMA");
		timeout.setFromName(userURIString);
		timeout.setFromUri("sip:"+userURI);
		timeout.setCallId(callId);
		timeout.setcSeqNumber("1");
		timeout.setcSeqStr("REGISTER");
		timeout.setContentLength(timeout.toStringMessage().getBytes().length);
		
		return timeout;
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
