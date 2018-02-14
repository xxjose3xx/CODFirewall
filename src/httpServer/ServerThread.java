package httpServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;


public class ServerThread extends Thread{

	private Socket socketClient;
	
	public ServerThread(Socket socketClient) {
		this.socketClient = socketClient;
	}
	
	public String mime(String file) {
		
		String extension = file.split("\\.")[1];
		
		if(extension.equals("html"))
			return "text/html";
		
		if(extension.equals("css"))
			return "text/css";
		
		if(extension.equals("js"))
			return "text/javascript";
		
		if(extension.equals("png"))
			return "image/png";
		
		if(extension.equals("jpg") || extension.equals("jpeg"))
			return "image/jpeg";
		
		if(extension.equals("gif"))
			return "image/gif";
		
		if(extension.equals("bmp"))
			return "image/bmp";
		
		return "text/plain";
	}

	public void get(OutputStream outByte, String resource) throws IOException {
		
		File file = new File(((resource.equals("/")) ? "/index.html":resource).substring(1));
		
		if(file.exists()) {
			outByte.write(new String("HTTP/1.1 200 OK\r\n").getBytes());
		    outByte.write(new String("Server: servidorFirewall/1.0\r\n").getBytes());
		    outByte.write(new String("Connection: close\r\n").getBytes());
		    outByte.write(new String("Content-Type: " + mime(file.getName()) + "\r\n").getBytes());
		    outByte.write(new String("Content-Length: " + file.length() + "\r\n").getBytes());
		    outByte.write(new String("\r\n").getBytes());
			Files.copy(file.toPath(), outByte);
		} else
			error404(outByte);
	}
	
	public void post(OutputStream outByte, String resource, BufferedReader in) throws IOException {

		while (!(in.readLine()).isEmpty());
		char[] c = new char[100];
		in.read(c);
		
		String body[] = (new String(c)).split("=");
		boolean status = true;
		
		if(body.length == 2) {
			
			body[1] = body[1].trim();
			
			if(body[0].equals("add") && !MyHTTPServer.ips.contains(body[1])) {
				String aux[] = body[1].split("\\.");
				if(aux.length == 4 && Integer.parseInt(aux[0]) < 256 && Integer.parseInt(aux[1]) < 256 && Integer.parseInt(aux[2]) < 256 && Integer.parseInt(aux[3]) < 256)
					MyHTTPServer.ips.add(body[1]);
				else
					status = false;		
			} else if(body[0].equals("del") && MyHTTPServer.ips.contains(body[1]))
				MyHTTPServer.ips.remove(body[1]);
			
			if(status) {			
				try {
					String command = "netsh advfirewall firewall set rule name=@cod new remoteip=1.1.1.1";
					for(String ip : MyHTTPServer.ips)
						command += "," + ip;
					Runtime.getRuntime().exec(command);
					System.out.println("Comando ejecutado: " + command);
				} catch(IOException e) {
					e.printStackTrace();
					status = false;
				}	
			}			
		} else 
			status = false;

		outByte.write(new String("HTTP/1.1 200 OK\r\n").getBytes());
		outByte.write(new String("Server: servidorFirewall/1.0\r\n").getBytes());
		outByte.write(new String("Connection: close\r\n").getBytes());
	    outByte.write(new String("Content-Type: text/html\r\n").getBytes());
	    outByte.write(new String("Content-Length: 2\r\n").getBytes());
	    outByte.write(new String("\r\n").getBytes());
		
		if(status)
		    outByte.write(new String("Y").getBytes());//1+1
		else
			outByte.write(new String("N").getBytes());//1+1
	}
	
	public void error400(OutputStream outByte) throws IOException {
		
		outByte.write(new String("HTTP/1.1 400 Not Request\r\n").getBytes());
		outByte.write(new String("Server: servidorFirewall/1.0\r\n").getBytes());
		outByte.write(new String("Connection: close\r\n").getBytes());
		outByte.write(new String("Content-Type: text/html\r\n").getBytes());
		outByte.write(new String("Content-Length: 49\r\n").getBytes());
		outByte.write(new String("\r\n").getBytes());
		outByte.write(new String("<title>Error: 400</title>").getBytes());//25+1
		outByte.write(new String("<p>404 Not Request</p>").getBytes());//22+1
	}
	
	public void error404(OutputStream outByte) throws IOException {
		
		outByte.write(new String("HTTP/1.1 404 Not Found\r\n").getBytes());
		outByte.write(new String("Server: servidorFirewall/1.0\r\n").getBytes());
		outByte.write(new String("Connection: close\r\n").getBytes());
		outByte.write(new String("Content-Type: text/html\r\n").getBytes());
		outByte.write(new String("Content-Length: 47\r\n").getBytes());
		outByte.write(new String("\r\n").getBytes());
		outByte.write(new String("<title>Error: 404</title>").getBytes());//25+1
		outByte.write(new String("<p>404 Not Found</p>").getBytes());//20+1
	}
	
	public void error405(OutputStream outByte) throws IOException {

		outByte.write(new String("HTTP/1.1 405 Method Not Allowed\r\n").getBytes());
        outByte.write(new String("Server: servidorFirewall/1.0\r\n").getBytes());
        outByte.write(new String("Connection: close\r\n").getBytes());
        outByte.write(new String("Content-Type: text/html\r\n").getBytes());
        outByte.write(new String("Content-Length: 65\r\n").getBytes());
        outByte.write(new String("\r\n").getBytes());
        outByte.write(new String("<title>Error: 405</title>").getBytes());//25+1
        outByte.write(new String("<p>405 Method Not Allowed</p>").getBytes());//29+1	
	}
		
	public void run() {

        try(BufferedReader in = new BufferedReader(new InputStreamReader(socketClient.getInputStream()));
        		OutputStream outByte = socketClient.getOutputStream()) {

        	String requestLine[] = in.readLine().split(" ");
        	
        	if(requestLine.length != 3 || !requestLine[2].equals("HTTP/1.1"))
        		error400(outByte);
        	else if(requestLine[0].equals("GET"))
        		get(outByte, requestLine[1]);
        	else if(requestLine[0].equals("POST"))
            	post(outByte, requestLine[1], in);
        	else
        		error405(outByte);
        	
        } catch(IOException e) {
        	e.printStackTrace();
        }
        
        try {
			socketClient.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
}