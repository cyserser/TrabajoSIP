import ua.UaUserLayer;

public class UA {
	public static void main(String[] args) throws Exception {
		System.out.println("UA launching with args: " + String.join(", ", args));
		String userURI = args[0];
		int listenPort = Integer.parseInt(args[1]);
		String proxyAddress = args[2];
		int proxyPort = Integer.parseInt(args[3]);

		UaUserLayer userLayer = new UaUserLayer(userURI, listenPort, proxyAddress, proxyPort);
		
		new Thread() {
			@Override
			public void run() {
				userLayer.startListeningNetwork();
			}
		}.start();

		userLayer.autoRegistering();
		userLayer.startListeningKeyboard();
	}
}
