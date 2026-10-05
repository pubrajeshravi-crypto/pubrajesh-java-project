package com.sms.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thrown when one or more form fields fail validation.
 *
 * <p>The {@link #getFieldErrors()} map contains field-name → error-message pairs
 * so the UI can render per-field inline error labels.
 */
public class ValidationException extends RuntimeException {

  /** Unmodifiable map of field name → human-readable error message. */
  private final Map<String, String> fieldErrors;

  /**
   * Constructs a {@code ValidationException} from a pre-built map of errors.
   *
   * @param fieldErrors field-name → error-message entries; must not be {@code null}
   */
  public ValidationException(Map<String, String> fieldErrors) {
    super("Validation failed: " + fieldErrors);
    this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
  }

  /**
   * Convenience constructor for a single-field error.
   *
   * @param field   field name
   * @param message human-readable error description
   */
  public ValidationException(String field, String message) {
    this(Map.of(field, message));
  }

  /**
   * @return unmodifiable map of field name → validation error message
   */
  public Map<String, String> getFieldErrors() {
    return fieldErrors;
  }
}
