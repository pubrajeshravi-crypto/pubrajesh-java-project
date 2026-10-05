package com.sms.ui;

import com.sms.util.Constants;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;

/**
 * Status bar displayed at the bottom of {@link MainFrame}.
 *
 * <p>Shows the result of the last user action in either success (dark green)
 * or error (dark red) style, along with a permanent row-count label on the right.
 */
public final class StatusBar extends JPanel {

  private final JLabel messageLabel;
  private final JLabel countLabel;

  /** Creates the status bar. */
  public StatusBar() {
    super(new BorderLayout());
    setBorder(new CompoundBorder(
        new MatteBorder(1, 0, 0, 0, new Color(0xD0D0D0)),
        new EmptyBorder(4, Constants.SPACING, 4, Constants.SPACING)));
    setBackground(new Color(0xF8F8F8));

    messageLabel = new JLabel(" ");
    messageLabel.setFont(messageLabel.getFont().deriveFont((float) Constants.FONT_SIZE_BODY));

    countLabel = new JLabel("0 students");
    countLabel.setFont(countLabel.getFont().deriveFont((float) Constants.FONT_SIZE_BODY));
    countLabel.setForeground(new Color(0x5C5C5C));

    add(messageLabel, BorderLayout.WEST);
    add(countLabel,   BorderLayout.EAST);
  }

  // --------------------------------------------------------------- public API

  /**
   * Displays a success message in dark green.
   *
   * @param message the message to show
   */
  public void setSuccess(String message) {
    messageLabel.setText(message);
    messageLabel.setForeground(Color.decode(Constants.COLOR_SUCCESS));
  }

  /**
   * Displays an error message in dark red.
   *
   * @param message the message to show
   */
  public void setError(String message) {
    messageLabel.setText(message);
    messageLabel.setForeground(Color.decode(Constants.COLOR_ERROR));
  }

  /**
   * Displays a neutral (dark grey) informational message.
   *
   * @param message the message to show
   */
  public void setInfo(String message) {
    messageLabel.setText(message);
    messageLabel.setForeground(new Color(0x1A1A1A));
  }

  /**
   * Updates the student-count indicator on the right side.
   *
   * @param count the number of students currently displayed in the table
   */
  public void setCount(int count) {
    countLabel.setText(count + (count == 1 ? " student" : " students"));
  }
}
