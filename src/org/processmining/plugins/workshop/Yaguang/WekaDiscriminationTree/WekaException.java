package org.processmining.plugins.workshop.Yaguang.WekaDiscriminationTree;


/**
 * Class for Weka-specific exceptions.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public class WekaException extends Exception {

  /**
   * Creates a new WekaException with no message.
   *
   */
  public WekaException() {

    super();
  }

  /**
   * Creates a new WekaException.
   *
   * @param message the reason for raising an exception.
   */
  public WekaException(String message) {

    super(message);
  }
}
