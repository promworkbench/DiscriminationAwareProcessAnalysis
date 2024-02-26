package org.processmining.plugins.workshop.Yaguang.WekaDiscriminationTree;



import java.io.File;
import java.io.IOException;

/**
 * Interface to a loader/saver that loads/saves from a file source.
 *
 * @author Mark Hall
 * @version $Revision: 1.2 $
 */
public interface FileSourcedConverter {

  /**
   * Get the file extension used for this type of file
   *
   * @return the file extension
   */
  String getFileExtension();

  /**
   * Get a one line description of the type of file
   *
   * @return a description of the file type
   */
  String getFileDescription();

  /**
   * Set the file to load from/ to save in
   *
   * @param file the file to load from
   * @exception IOException if an error occurs
   */
   void setFile(File file) throws IOException;

  /**
   * Return the current source file/ destination file
   *
   * @return a <code>File</code> value
   */
  File retrieveFile();

}
