package http.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

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

  public ServerThread(Socket socketClient) {
    this.socketClient = socketClient;
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
        post(outByte, in);
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

    resource = (resource.equals("/") ? "/index.html" : resource).substring(1);
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
      error404(outByte);
    }
  }

  public void post(OutputStream outByte, BufferedReader in) throws IOException {

    while (!in.readLine().isEmpty());
    char[] c = new char[100];

    String aux = new String(c, 0, in.read(c));
    String[] body = aux.split("=");
    String status = "Y";

    if (body.length == 2) {
      if (body[0].equals("add") && !ServerCore.containsIp(body[1]) && validateIp(body[1])) {
        ServerCore.addIp(body[1]);
      } else if (body[0].equals("del") && ServerCore.containsIp(body[1])) {
        ServerCore.removeIp(body[1]);
      } else {
        status = "N";
      }
      outByte.write(STATUS_200);
      outByte.write(SERVER_INFO);
      outByte.write(CONNECTION_CLOSE);
      outByte.write(String.format(CONTENT_TYPE, ContentType.HTML).getBytes());
      outByte.write(String.format(CONTENT_LENGTH, 2).getBytes());
      outByte.write(LINE_FEED);
      outByte.write(status.getBytes());
    }
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
    outByte.write(String.format(CONTENT_LENGTH, 65).getBytes());
    outByte.write(LINE_FEED);
    outByte.write("<title>Error: 405</title>".getBytes());
    outByte.write("<p>405 Method Not Allowed</p>".getBytes());
  }

  private boolean validateIp(String ip) {
    String[] aux = ip.split("\\.");
    return aux.length == 4 && Integer.parseInt(aux[0]) < 256 && Integer.parseInt(aux[1]) < 256
        && Integer.parseInt(aux[2]) < 256 && Integer.parseInt(aux[3]) < 256;
  }
}
