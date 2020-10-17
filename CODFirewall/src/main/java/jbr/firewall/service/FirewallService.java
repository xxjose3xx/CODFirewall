package jbr.firewall.service;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jbr.http.server.ServerCore;

public class FirewallService {

  private static final Logger LOGGER;

  static {
    System.setProperty("java.util.logging.SimpleFormatter.format",
        "[%1$tF %1$tT] [%4$-7s] %5$s %n");
    LOGGER = Logger.getLogger(ServerCore.class.getName());
  }

  private static FirewallService firewallService = null;

  private static Set<String> ips = new HashSet<>();

  private FirewallService() {}

  public static FirewallService getInstance() {
    if (null != firewallService) {
      return firewallService;
    } else {
      return new FirewallService();
    }
  }

  public boolean addIp(String ip) {
    boolean rtn = false;

    if (validateIp(ip)) {
      ips.add(ip);
      updateRules(false);
      rtn = true;
    }

    return rtn;
  }

  public boolean removeIp(String ip) {
    boolean rtn = false;

    if (validateIp(ip)) {
      ips.remove(ip);
      updateRules(ips.isEmpty());
      rtn = true;
    }

    return rtn;
  }

  public void removeRules() {
    updateRules(true);
  }

  public String[] ipsToArray() {
    return ips.toArray(new String[ips.size()]);
  }

  private boolean validateIp(String ip) {
    String[] aux = ip.split("\\.");
    return aux.length == 4 && Integer.parseInt(aux[0]) < 256 && Integer.parseInt(aux[1]) < 256
        && Integer.parseInt(aux[2]) < 256 && Integer.parseInt(aux[3]) < 256;
  }

  private void updateRules(boolean deleteOnly) {
    try {
      Runtime.getRuntime().exec("netsh advfirewall firewall delete rule name=@codIn");
      Runtime.getRuntime().exec("netsh advfirewall firewall delete rule name=@codOut");

      if (!deleteOnly) {
        String ipList = ips.stream().collect(Collectors.joining(","));
        Runtime.getRuntime()
            .exec("netsh advfirewall firewall add rule name=@codIn dir=in action=block remoteip="
                + ipList);
        Runtime.getRuntime()
            .exec("netsh advfirewall firewall add rule name=@codOut dir=out action=block remoteip="
                + ipList);
        LOGGER.log(Level.INFO, "> Blocked ip list: {0}", ipList);
      } else {
        LOGGER.log(Level.INFO, "> Rules deleted");
      }
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "ServerCore updateFirewallRule exception", e);
    }
  }
}
