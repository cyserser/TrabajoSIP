package proxy;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import mensajesSIP.InviteMessage;
import mensajesSIP.RegisterMessage;
import mensajesSIP.OKMessage;
import mensajesSIP.NotFoundMessage;
import mensajesSIP.SDPMessage;

public class ProxyUserLayer {
	private ProxyTransactionLayer transactionLayer;
	private ArrayList<String> validUsers;
	private String originAddress;
	private int originPort;

	public ProxyUserLayer(int listenPort) throws SocketException {
		this.transactionLayer = new ProxyTransactionLayer(listenPort, this);
		this.validUsers = new ArrayList<String>();
		this.validUsers.add("alice");
		this.validUsers.add("bob");
	}

	public void onInviteReceived(InviteMessage inviteMessage) throws IOException {
		System.out.println("Received INVITE from " + inviteMessage.getFromName());
		ArrayList<String> vias = inviteMessage.getVias();
		String origin = vias.get(0);
		String[] originParts = origin.split(":");
		String originAddress = originParts[0];
		int originPort = Integer.parseInt(originParts[1]);
		transactionLayer.echoInvite(inviteMessage, originAddress, originPort);
	}
	
	public void onRegisterReceived(RegisterMessage registerMessage) throws IOException {
		System.out.println("Received REGISTER from " + registerMessage.getFromName());
		System.out.println(registerMessage.getFromName());
		System.out.println(validUsers.get(0).toString());
		
		ArrayList<String> vias = registerMessage.getVias();
		String origin = vias.get(0);
		String[] originParts = origin.split(":");
		originAddress = originParts[0];
		originPort = Integer.parseInt(originParts[1]);
		
		//Comprobar si el usuario esta en la lista
		for(int i = 0; i < validUsers.size(); i++)
		{
			if(validUsers.get(i).equals(registerMessage.getFromName())) {
				
				transactionLayer.echoOK(OKMessage(), originAddress, originPort);
				return;
			}
			
		}
		
		transactionLayer.echoNotfound(NotFoundMessage(), originAddress, originPort);
		System.out.println("UNKNOWN USER");
	}
	
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

	public void startListening() {
		transactionLayer.startListening();
	}

}
