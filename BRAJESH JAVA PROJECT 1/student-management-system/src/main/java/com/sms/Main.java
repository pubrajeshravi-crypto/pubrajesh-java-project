package com.sms;

import com.formdev.flatlaf.FlatLightLaf;
import com.sms.dao.StudentDAOImpl;
import com.sms.service.StudentService;
import com.sms.ui.MainFrame;
import com.sms.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sms.web.WebServer;

import javax.swing.*;
import java.awt.Desktop;
import java.net.URI;

/**
 * Application entry point.
 *
 * <p>Bootstraps FlatLightLaf, wires the service layer, starts the embedded
 * {@link WebServer} on port 8080, and launches {@link MainFrame} on the Event
 * Dispatch Thread. Registers a JVM shutdown hook to drain HikariCP and stop the server cleanly.
 */
public final class Main {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);
  private static final int DEFAULT_WEB_PORT = 8080;

  private Main() {}

  /**
   * @param args command-line arguments (supports "--web-only" for headless mode)
   */
  public static void main(String[] args) {
    boolean webOnly = false;
    for (String arg : args) {
      if ("--web-only".equalsIgnoreCase(arg) || "--web".equalsIgnoreCase(arg)) {
        webOnly = true;
      }
    }

    StudentService service = new StudentService(new StudentDAOImpl());

    // 1. Start Embedded Web Server
    WebServer webServer = new WebServer(service, DEFAULT_WEB_PORT);
    try {
      webServer.start();
      System.out.println("==================================================================");
      System.out.println("  Student Management System Web Interface running at:");
      System.out.println("  --> http://localhost:" + DEFAULT_WEB_PORT + "/ <--");
      System.out.println("==================================================================");
    } catch (Exception ex) {
      LOG.error("Failed to start WebServer on port {}", DEFAULT_WEB_PORT, ex);
    }

    // 2. Register Shutdown Hook
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      LOG.info("Shutdown hook: closing WebServer & DB pool.");
      try {
        webServer.stop();
      } catch (Exception ignored) {}
      DBConnection.shutdown();
    }, "sms-shutdown"));

    // 3. Launch Desktop Swing UI if not running in web-only mode
    if (!webOnly) {
      // Install FlatLaf before any Swing component is created
      try {
        FlatLightLaf.setup();
        UIManager.put("Button.arc",               6);
        UIManager.put("Component.arc",            4);
        UIManager.put("TextComponent.arc",        4);
        UIManager.put("ScrollBar.width",          10);
        UIManager.put("TableHeader.background",   UIManager.getColor("Panel.background"));
      } catch (Throwable t) {
        LOG.warn("FlatLaf could not be installed, falling back to default L&F", t);
      }

      SwingUtilities.invokeLater(() -> {
        try {
          MainFrame frame = new MainFrame(service);
          frame.setVisible(true);
        } catch (Exception ex) {
          LOG.error("Failed to launch MainFrame", ex);
          JOptionPane.showMessageDialog(null,
              "Failed to start desktop application:\n" + ex.getMessage()
              + "\n\nWeb Interface is still running at http://localhost:" + DEFAULT_WEB_PORT + "/",
              "Startup Warning", JOptionPane.WARNING_MESSAGE);
        }
      });
    } else {
      LOG.info("Running in web-only mode. Press Ctrl+C to stop.");
    }
  }
}
