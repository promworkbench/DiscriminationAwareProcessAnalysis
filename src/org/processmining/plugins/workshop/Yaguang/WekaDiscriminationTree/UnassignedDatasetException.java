package org.processmining.plugins.workshop.Yaguang.WekaDiscriminationTree;



/**
 * Exception that is raised when trying to use something that has no
 * reference to a dataset, when one is required.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public class UnassignedDatasetException extends RuntimeException {

  /**
   * Creates a new UnassignedDatasetException with no message.
   *
   */
  public UnassignedDatasetException() {

    super();
  }

  /**
   * Creates a new UnassignedDatasetException.
   *
   * @param message the reason for raising an exception.
   */
  public UnassignedDatasetException(String message) {

    super(message);
  }
}
