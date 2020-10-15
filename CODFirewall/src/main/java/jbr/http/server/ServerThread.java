package jbr.http.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;
import jbr.firewall.service.FirewallService;

public class ServerThread extends Thread {

  private static final Logger LOGGER;

  static {
    System.setProperty("java.util.logging.SimpleFormatter.format",
        "[%1$tF %1$tT] [%4$-7s] %5$s %n");
    LOGGER = Logger.getLogger(ServerThread.class.getName());
  }

  private static final byte[] STATUS_200 = "HTTP/1.1 200 OK\r\n".getBytes();

  private static final byte[] STATUS_400 = "HTTP/1.1 400 Not Request\r\n".getBytes();

  private static final byte[] STATUS_404 = "HTTP/1.1 404 Not Found\r\n".getBytes();

  private static final byte[] STATUS_405 = "HTTP/1.1 405 Method Not Allowed\r\n".getBytes();

  private static final byte[] SERVER_INFO = "Server: servidorFirewall/1.0\r\n".getBytes();

  private static final byte[] CONNECTION_CLOSE = "Connection: close\r\n".getBytes();

  private static final byte[] LINE_FEED = "\r\n".getBytes();

  private static final String CONTENT_TYPE = "Content-Type: %s\r\n";

  private static final String CONTENT_LENGTH = "Content-Length: %d\r\n";

  private Socket socketClient;

  private FirewallService firewallService;

  public ServerThread(Socket socketClient) {
    this.socketClient = socketClient;
    this.firewallService = FirewallService.getInstance();
  }

  @Override
  public void run() {
    try (
        BufferedReader in =
            new BufferedReader(new InputStreamReader(socketClient.getInputStream()));
        OutputStream outByte = socketClient.getOutputStream()) {

      String request = in.readLine();
      String[] params = request.split(" ");
      LOGGER.log(Level.INFO, ">> {0}", request);

      if (params.length != 3 || !params[2].equals("HTTP/1.1")) {
        error400(outByte);
      } else if (params[0].equals("GET")) {
        get(outByte, params[1]);
      } else if (params[0].equals("POST")) {
        post(outByte, in, params[1]);
      } else if (params[0].equals("PUT")) {
        put(outByte, params[1]);
      } else {
        error405(outByte);
      }

    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "ServerCore exception", e);
    }
  }

  private ContentType mime(String file) {

    String extension = file.split("\\.")[1];

    if (extension.equals("html"))
      return ContentType.HTML;

    if (extension.equals("css"))
      return ContentType.CSS;

    if (extension.equals("js"))
      return ContentType.JAVASCRIPT;

    if (extension.equals("png"))
      return ContentType.PNG;

    if (extension.equals("jpg") || extension.equals("jpeg"))
      return ContentType.JPEG;

    if (extension.equals("gif"))
      return ContentType.GIF;

    if (extension.equals("bmp"))
      return ContentType.BMP;

    return ContentType.PLAIN;
  }

  public void get(OutputStream outByte, String resource) throws IOException {

    switch (resource) {
      case "/":
        resource = "index.html";
        break;
      case "/exit":
        firewallService.removeRules();
        shutdownPage(outByte);
        ServerCore.shutdown();
        return;
      default:
        resource = resource.substring(1);
    }
    System.out.println("resource: " + resource);
    InputStream file = getClass().getClassLoader().getResourceAsStream(resource);

    if (null != file) {
      byte[] fileContent = new byte[10 * 1024 * 1024];
      int fileLength = file.read(fileContent);
      outByte.write(STATUS_200);
      outByte.write(SERVER_INFO);
      outByte.write(CONNECTION_CLOSE);
      outByte.write(String.format(CONTENT_TYPE, mime(resource)).getBytes());
      outByte.write(String.format(CONTENT_LENGTH, fileLength).getBytes());
      outByte.write(LINE_FEED);
      outByte.write(fileContent, 0, fileLength);
    } else {
      System.out.print("resource not found: " + resource);
      error404(outByte);
    }
  }

  public void put(OutputStream outByte, String resource) throws IOException {

    if ("/removeRules".equals(resource)) {
      firewallService.removeRules();
      ok200(outByte, true);
    } else {
      error400(outByte);
    }
  }

  public void post(OutputStream outByte, BufferedReader in, String resource) throws IOException {

    while (!in.readLine().isEmpty());
    char[] c = new char[100];
    JSONObject jsonObject = new JSONObject(new String(c, 0, in.read(c)));
    boolean status;

    switch (resource) {
      case "/block":
        status = firewallService.addIp(jsonObject.getString("ip"));
        break;
      case "/allow":
        status = firewallService.removeIp(jsonObject.getString("ip"));
        break;
      default:
        status = false;
    }

    ok200(outByte, status);
  }

  private void ok200(OutputStream outByte, boolean status) throws IOException {

    JSONObject jsonObject = new JSONObject();
    jsonObject.put("success", status);
    jsonObject.put("ips", firewallService.ipsToArray());
    String response = jsonObject.toString();

    outByte.write(STATUS_200);
    outByte.write(SERVER_INFO);
    outByte.write(CONNECTION_CLOSE);
    outByte.write(String.format(CONTENT_TYPE, ContentType.JSON).getBytes());
    outByte.write(String.format(CONTENT_LENGTH, response.length()).getBytes());
    outByte.write(LINE_FEED);
    outByte.write(response.getBytes());
  }

  private void error400(OutputStream outByte) throws IOException {
    outByte.write(STATUS_400);
    outByte.write(SERVER_INFO);
    outByte.write(CONNECTION_CLOSE);
    outByte.write(String.format(CONTENT_TYPE, ContentType.HTML).getBytes());
    outByte.write(String.format(CONTENT_LENGTH, 49).getBytes());
    outByte.write(LINE_FEED);
    outByte.write("<title>Error: 400</title>".getBytes());
    outByte.write("<p>404 Not Request</p>".getBytes());
  }

  private void error404(OutputStream outByte) throws IOException {
    outByte.write(STATUS_404);
    outByte.write(SERVER_INFO);
    outByte.write(CONNECTION_CLOSE);
    outByte.write(String.format(CONTENT_TYPE, ContentType.HTML).getBytes());
    outByte.write(String.format(CONTENT_LENGTH, 47).getBytes());
    outByte.write(LINE_FEED);
    outByte.write("<title>Error: 404</title>".getBytes());
    outByte.write("<p>404 Not Found</p>".getBytes());
  }

  private void error405(OutputStream outByte) throws IOException {
    outByte.write(STATUS_405);
    outByte.write(SERVER_INFO);
    outByte.write(CONNECTION_CLOSE);
    outByte.write(String.format(CONTENT_TYPE, ContentType.HTML).getBytes());
    outByte.write(String.format(CONTENT_LENGTH, 56).getBytes());
    outByte.write(LINE_FEED);
    outByte.write("<title>Error: 405</title>".getBytes());
    outByte.write("<p>405 Method Not Allowed</p>".getBytes());
  }

  private void shutdownPage(OutputStream outByte) throws IOException {
    outByte.write(STATUS_200);
    outByte.write(SERVER_INFO);
    outByte.write(CONNECTION_CLOSE);
    outByte.write(String.format(CONTENT_TYPE, ContentType.HTML).getBytes());
    outByte.write(String.format(CONTENT_LENGTH, 56).getBytes());
    outByte.write(LINE_FEED);
    outByte.write("<title>Shut down</title>".getBytes());
    outByte.write("<p>Server is shutting down</p>".getBytes());
  }
}
