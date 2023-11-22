import ua.UaUserLayer;

public class UA {
	public static void main(String[] args) throws Exception {
		System.out.println("UA launching with args: " + String.join(", ", args));
		String userURI = args[0];
		int listenPort = Integer.parseInt(args[1]);
		String proxyAddress = args[2];
		int proxyPort = Integer.parseInt(args[3]);
		String firstLine = args[4];
		int expiresTime = Integer.parseInt(args[5]);
		
		UaUserLayer userLayer = new UaUserLayer(userURI, listenPort, proxyAddress, proxyPort, firstLine, expiresTime);
		
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
