package httpServer;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class MyHTTPServer {
	
	static int conexiones = 0;
	static ArrayList<String> ips = new ArrayList<String>();;
	
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		
		try {
			
			String puerto = "8181";
			ServerSocket socketServidor = new ServerSocket(Integer.parseInt(puerto));
			System.out.println("> httpServer running on port: " + puerto);
			
			for(;;) {
				Socket socketCliente = socketServidor.accept();
				Thread t = new ServerThread(socketCliente);
				t.start();	
			}

		} catch(Exception e) {
			System.err.println("Error: " + e.toString());
		}
	}	
}