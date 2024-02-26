package org.processmining.plugins.workshop.Yaguang.WekaDiscriminationTree;


/** 
 * Interface to something that provides a short textual summary (as opposed
 * to toString() which is usually a fairly complete description) of itself.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.5 $
 */
public interface Summarizable {

  /**
   * Returns a string that summarizes the object.
   *
   * @return the object summarized as a string
   */
  String toSummaryString();
}








