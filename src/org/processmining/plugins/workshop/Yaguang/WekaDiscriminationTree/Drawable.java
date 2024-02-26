package org.processmining.plugins.workshop.Yaguang.WekaDiscriminationTree;

/** 
 * Interface to something that can be drawn as a graph.
 *
 * @author Ashraf M. Kibriya(amk14@cs.waikato.ac.nz), Eibe Frank(eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.8 $
 */
public interface Drawable {

  int NOT_DRAWABLE = 0, TREE = 1, BayesNet = 2;

  /**
   * Returns the type of graph representing
   * the object.
   *
   * @return the type of graph representing the object
   */
  int graphType();

  /**
   * Returns a string that describes a graph representing
   * the object. The string should be in XMLBIF ver.
   * 0.3 format if the graph is a BayesNet, otherwise
   * it should be in dotty format.
   *
   * @return the graph described by a string
   * @exception Exception if the graph can't be computed
   */
  String graph() throws Exception;
}








