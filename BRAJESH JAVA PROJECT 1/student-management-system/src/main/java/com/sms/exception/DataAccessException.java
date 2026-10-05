package com.sms.exception;

/**
 * Runtime wrapper around {@link java.sql.SQLException} and other persistence
 * errors.  The service layer catches this and the UI converts it to an error
 * dialog — no raw {@code SQLException} ever surfaces above the DAO layer.
 */
public class DataAccessException extends RuntimeException {

  /**
   * @param message human-readable description of the failure
   * @param cause   the underlying {@link java.sql.SQLException}
   */
  public DataAccessException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * @param message human-readable description of the failure
   */
  public DataAccessException(String message) {
    super(message);
  }
}
