import proxy.ProxyUserLayer;

public class Proxy {
	public static void main(String[] args) throws Exception {
		System.out.println("Proxy launching with args: " + String.join(", ", args));
		int listenPort = Integer.parseInt(args[0]);
		String firstLine = args[1];
		String looseRouting = args[2];
		ProxyUserLayer userLayer = new ProxyUserLayer(listenPort, firstLine, looseRouting);
		userLayer.startListening();
	}
}