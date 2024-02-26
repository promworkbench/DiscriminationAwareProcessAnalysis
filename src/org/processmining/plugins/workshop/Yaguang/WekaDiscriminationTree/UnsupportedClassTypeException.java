package org.processmining.plugins.workshop.Yaguang.WekaDiscriminationTree;


/**
 * Exception that is raised by an object that is unable to process the
 * class type of the data it has been passed.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public class UnsupportedClassTypeException extends WekaException {

  /**
   * Creates a new UnsupportedClassTypeException with no message.
   *
   */
  public UnsupportedClassTypeException() {

    super();
  }

  /**
   * Creates a new UnsupportedClassTypeException.
   *
   * @param message the reason for raising an exception.
   */
  public UnsupportedClassTypeException(String message) {

    super(message);
  }
}
