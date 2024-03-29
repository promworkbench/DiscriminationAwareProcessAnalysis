package org.processmining.plugins.workshop.Yaguang.WekaDiscriminationTree;


import java.util.Enumeration;

/** 
 * Interface to something that can produce measures other than those
 * calculated by evaluation modules. 
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.7 $
 */
public interface AdditionalMeasureProducer {

  /**
   * Returns an enumeration of the measure names. Additional measures
   * must follow the naming convention of starting with "measure", eg.
   * double measureBlah()
   * @return an enumeration of the measure names
   */
  Enumeration enumerateMeasures();

  /**
   * Returns the value of the named measure
   * @param measureName the name of the measure to query for its value
   * @return the value of the named measure
   * @exception IllegalArgumentException if the named measure is not supported
   */
  double getMeasure(String measureName);
}
