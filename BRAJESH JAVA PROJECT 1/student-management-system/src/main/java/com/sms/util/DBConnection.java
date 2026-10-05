package com.sms.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton that manages the HikariCP connection pool.
 *
 * <p>Configuration is loaded from {@code config.properties} on the classpath.
 * Call {@link #getConnection()} to borrow a connection; always close it in a
 * try-with-resources block so it is returned to the pool automatically.
 *
 * <p>Call {@link #shutdown()} once — typically from a JVM shutdown hook — to
 * drain the pool cleanly.
 */
public final class DBConnection {

  private static final Logger LOG = LoggerFactory.getLogger(DBConnection.class);
  private static final String CONFIG_FILE = "config.properties";

  private static volatile HikariDataSource dataSource;

  private DBConnection() {}

  /**
   * Returns the singleton {@link DataSource}, initialising it on first call.
   *
   * @return the HikariCP {@link DataSource}
   * @throws IllegalStateException if {@code config.properties} cannot be found or parsed
   */
  public static DataSource getDataSource() {
    if (dataSource == null) {
      synchronized (DBConnection.class) {
        if (dataSource == null) {
          dataSource = buildPool();
        }
      }
    }
    return dataSource;
  }

  /**
   * Borrows a connection from the pool.
   *
   * @return an open {@link Connection}; must be closed by the caller
   * @throws SQLException if no connection can be obtained
   */
  public static Connection getConnection() throws SQLException {
    return getDataSource().getConnection();
  }

  /**
   * Closes the connection pool. Call once on application exit.
   */
  public static void shutdown() {
    if (dataSource != null && !dataSource.isClosed()) {
      LOG.info("Shutting down HikariCP connection pool.");
      dataSource.close();
    }
  }

  // ----------------------------------------------------------------- private

  private static HikariDataSource buildPool() {
    Properties props = loadProperties();

    HikariConfig cfg = new HikariConfig();
    cfg.setJdbcUrl(props.getProperty("db.url"));
    cfg.setUsername(props.getProperty("db.username"));
    cfg.setPassword(props.getProperty("db.password"));
    cfg.setMaximumPoolSize(Integer.parseInt(
        props.getProperty("db.pool.maximumPoolSize", "10")));
    cfg.setMinimumIdle(Integer.parseInt(
        props.getProperty("db.pool.minimumIdle", "2")));
    cfg.setConnectionTimeout(Long.parseLong(
        props.getProperty("db.pool.connectionTimeout", "30000")));
    cfg.setIdleTimeout(Long.parseLong(
        props.getProperty("db.pool.idleTimeout", "600000")));
    cfg.setMaxLifetime(Long.parseLong(
        props.getProperty("db.pool.maxLifetime", "1800000")));
    cfg.setPoolName("SMS-Pool");

    LOG.info("Initialising HikariCP pool — URL: {}", cfg.getJdbcUrl());
    return new HikariDataSource(cfg);
  }

  private static Properties loadProperties() {
    Properties props = new Properties();
    try (InputStream in =
             DBConnection.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
      if (in == null) {
        throw new IllegalStateException(
            CONFIG_FILE + " not found on classpath. "
            + "Copy config.properties.example to src/main/resources/config.properties "
            + "and fill in your credentials.");
      }
      props.load(in);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load " + CONFIG_FILE, e);
    }
    return props;
  }
}
