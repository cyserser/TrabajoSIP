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
		transactionLayer.echoInvite(inviteMessage, originAddress, originPort);
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

	private String getFromWhiteList(int i) {
		String whiteListUser;
		whiteListUser = whiteList.getWhiteList().get(i).getUserURI().substring(0, whiteList.getWhiteList().get(i).getUserURI().indexOf("@"));
		return whiteListUser;
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
