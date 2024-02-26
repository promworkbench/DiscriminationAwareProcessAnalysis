package org.processmining.plugins.workshop.Yaguang.WekaDiscriminationTree;



import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Abstract class gives default implementation of setSource 
 * methods. All other methods must be overridden.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 1.6.2.2 $
 */
public abstract class AbstractLoader implements Loader {
  
  /** The retrieval modes */
  protected static final int NONE = 0;
  protected static final int BATCH = 1;
  protected static final int INCREMENTAL = 2;

  /** The current retrieval mode */
  protected int m_retrieval;

  /**
   * Sets the retrieval mode.
   *
   * @param mode the retrieval mode
   */
  protected void setRetrieval(int mode) {

    m_retrieval = mode;
  }

  /**
   * Gets the retrieval mode.
   *
   * @return the retrieval mode
   */
  protected int getRetrieval() {

    return m_retrieval;
  }

  /**
   * Default implementation throws an IOException.
   *
   * @param file the File
   * @exception IOException always
   */
  public void setSource(File file) throws IOException {

    throw new IOException("Setting File as source not supported");
  }

  /**
   * Default implementation sets retrieval mode to NONE
   *
   * @exception never.
   */
  public void reset() throws Exception {
    m_retrieval = NONE;
  }
  
  /**
   * Default implementation throws an IOException.
   *
   * @param input the input stream
   * @exception IOException always
   */
  public void setSource(InputStream input) throws IOException {

    throw new IOException("Setting InputStream as source not supported");
  }
  
  /*
   * To be overridden.
   */
  public abstract Instances getStructure() throws IOException;

  /*
   * To be overridden.
   */
  public abstract Instances getDataSet() throws IOException;

  /*
   * To be overridden.
   */
  public abstract Instance getNextInstance(Instances structure) throws IOException;
}
