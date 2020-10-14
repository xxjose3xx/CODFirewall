package jbr.http.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ServerCore {

  private static final int PORT = 8181;

  private static final Logger LOGGER;

  static {
    System.setProperty("java.util.logging.SimpleFormatter.format",
        "[%1$tF %1$tT] [%4$-7s] %5$s %n");
    LOGGER = Logger.getLogger(ServerCore.class.getName());
  }

  private static List<String> ips = new ArrayList<>();

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

  public static boolean containsIp(String ip) {
    return ips.contains(ip);
  }

  public static void addIp(String ip) {
    ips.add(ip);
    updateFirewallRule(false);
  }

  public static void removeIp(String ip) {
    ips.remove(ip);
    updateFirewallRule(ips.isEmpty());
  }

  public static void shutdown() {
    updateFirewallRule(true);
    keepActive = false;
  }

  private static void updateFirewallRule(boolean deleteOnly) {
    try {
      String command = "netsh advfirewall firewall delete rule name=@cod";
      Runtime.getRuntime().exec(command);
      LOGGER.log(Level.INFO, "> Command executed: {0}", command);

      if (!deleteOnly) {
        command = "netsh advfirewall firewall add rule name=@cod dir=in action=block remoteip=";
        command += ips.stream().collect(Collectors.joining(","));
        Runtime.getRuntime().exec(command);
        LOGGER.log(Level.INFO, "> Command executed: {0}", command);
      }
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "ServerCore updateFirewallRule exception", e);
    }
  }
}
