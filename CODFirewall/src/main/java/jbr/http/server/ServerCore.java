package jbr.http.server;

import java.net.ServerSocket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerCore {

  private static final int PORT = 8181;

  private static final Logger LOGGER;

  static {
    System.setProperty("java.util.logging.SimpleFormatter.format",
        "[%1$tF %1$tT] [%4$-7s] %5$s %n");
    LOGGER = Logger.getLogger(ServerCore.class.getName());
  }

  private static boolean keepActive = true;

  public static void main(String[] args) {
    try (ServerSocket socketServidor = new ServerSocket(PORT)) {
      LOGGER.log(Level.INFO, "> httpServer running on port: " + PORT);
      while (keepActive) {
        new ServerThread(socketServidor.accept()).start();
      }
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "ServerCore main exception", e);
    }
  }

  public static void shutdown() {
    keepActive = false;
  }
}
