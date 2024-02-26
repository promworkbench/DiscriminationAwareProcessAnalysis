package org.processmining.plugins.workshop.Yaguang.WekaDiscriminationTree;




/**
 * Exception that is raised by an object that is unable to process some of the
 * attribute types it has been passed.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public class UnsupportedAttributeTypeException extends WekaException {

  /**
   * Creates a new UnsupportedAttributeTypeException with no message.
   *
   */
  public UnsupportedAttributeTypeException() {

    super();
  }

  /**
   * Creates a new UnsupportedAttributeTypeException.
   *
   * @param message the reason for raising an exception.
   */
  public UnsupportedAttributeTypeException(String message) {

    super(message);
  }
}
