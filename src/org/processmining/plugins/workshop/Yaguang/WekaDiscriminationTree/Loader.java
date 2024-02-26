package org.processmining.plugins.workshop.Yaguang.WekaDiscriminationTree;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

/** 
 * Interface to something that can load Instances from an input source in some
 * format.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.6.2.2 $
 */
public interface Loader extends Serializable {

  /**
   * Resets the Loader object ready to begin loading.
   * If there is an existing source, implementations should
   * attempt to reset in such a fashion as to be able to
   * load from the beginning of the source.
   * @exception if Loader can't be reset for some reason.
   */
  void reset() throws Exception;

  /*@ public model instance boolean model_structureDetermined
    @   initially: model_structureDetermined == false;
    @*/

  /*@ public model instance boolean model_sourceSupplied
    @   initially: model_sourceSupplied == false;
    @*/

  /**
   * Resets the Loader object and sets the source of the data set to be 
   * the supplied File object.
   *
   * @param file the File
   * @exception IOException if an error occurs
   * support loading from a File.
   *
   * <pre><jml>
   *    public_normal_behavior
   *      requires: file != null
   *                && (* file exists *);
   *      modifiable: model_sourceSupplied, model_structureDetermined;
   *      ensures: model_sourceSupplied == true 
   *               && model_structureDetermined == false;
   *  also
   *    public_exceptional_behavior
   *      requires: file == null
   *                || (* file does not exist *);
   *    signals: (IOException);
   * </jml></pre>
   */
  void setSource(File file) throws IOException;

  /**
   * Resets the Loader object and sets the source of the data set to be 
   * the supplied InputStream.
   *
   * @param input the source InputStream
   * @exception IOException if this Loader doesn't
   * support loading from a File.
   */
  void setSource(InputStream input) throws IOException;

  /**
   * Determines and returns (if possible) the structure (internally the 
   * header) of the data set as an empty set of instances.
   *
   * @return the structure of the data set as an empty set of Instances
   * @exception IOException if there is no source or parsing fails
   *
   * <pre><jml>
   *    public_normal_behavior
   *      requires: model_sourceSupplied == true
   *                && model_structureDetermined == false
   *                && (* successful parse *);
   *      modifiable: model_structureDetermined;
   *      ensures: \result != null
   *               && \result.numInstances() == 0
   *               && model_structureDetermined == true;
   *  also
   *    public_exceptional_behavior
   *      requires: model_sourceSupplied == false
   *                || (* unsuccessful parse *);
   *      signals: (IOException);
   * </jml></pre>
   */
  Instances getStructure() throws IOException;

  /**
   * Return the full data set. If the structure hasn't yet been determined
   * by a call to getStructure then the method should do so before processing
   * the rest of the data set.
   *
   * @return the full data set as an Instances object
   * @exception IOException if there is an error during parsing or if 
   * getNextInstance has been called on this source (either incremental
   * or batch loading can be used, not both).
   *
   * <pre><jml>
   *    public_normal_behavior
   *      requires: model_sourceSupplied == true
   *                && (* successful parse *);
   *      modifiable: model_structureDetermined;
   *      ensures: \result != null
   *               && \result.numInstances() >= 0
   *               && model_structureDetermined == true;
   *  also
   *    public_exceptional_behavior
   *      requires: model_sourceSupplied == false
   *                || (* unsuccessful parse *);
   *      signals: (IOException);
   * </jml></pre>
   */
  Instances getDataSet() throws IOException;

  /**
   * Read the data set incrementally---get the next instance in the data 
   * set or returns null if there are no
   * more instances to get. If the structure hasn't yet been 
   * determined by a call to getStructure then method should do so before
   * returning the next instance in the data set.
   *
   * If it is not possible to read the data set incrementally (ie. in cases
   * where the data set structure cannot be fully established before all
   * instances have been seen) then an exception should be thrown.
   *
   * @param structure the dataset header information, will get updated in 
   * case of string or relational attributes
   * @return the next instance in the data set as an Instance object or null
   * if there are no more instances to be read
   * @exception IOException if there is an error during parsing or if
   * getDataSet has been called on this source (either incremental
   * or batch loading can be used, not both).
   */
  Instance getNextInstance(Instances structure) throws IOException;
}




