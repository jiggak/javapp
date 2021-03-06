/*
 * Copyright 2008 Josh Kropf
 * 
 * This file is part of javapp.
 * 
 * javapp is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * javapp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with javapp; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package ca.slashdev.javapp;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Property;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.FileUtils;

public class JavaPpTask extends Task {
   private String prefix = "#";
   
   private boolean inheritAll = true;
   
   private boolean verbose = false;
   
   private File destDir;
   
   private File destFile;
   
   private File srcFile;
   
   private Vector<FileSet> resources = new Vector<FileSet>();
   
   private Vector<Property> properties = new Vector<Property>();
   
   public String getPrefix() {
      return prefix;
   }
   
   public void setPrefix(String prefix) {
      this.prefix = prefix;
   }
   
   public boolean isInheritAll() {
      return inheritAll;
   }
   
   public void setInheritAll(boolean inheritAll) {
      this.inheritAll = inheritAll;
   }
   
   public boolean isVerbose() {
      return verbose;
   }
   
   public void setVerbose(boolean verbose) {
      this.verbose = verbose;
   }
   
   public void setDestDir(File destDir) {
      this.destDir = destDir;
   }
   
   public File getDestDir() {
      return destDir;
   }
   
   public void setDestFile(File destFile) {
      this.destFile = destFile;
   }
   
   public File getDestFile() {
      return destFile;
   }
   
   public File getSrcFile() {
      return srcFile;
   }
   
   public void setSrcFile(File srcFile) {
      this.srcFile = srcFile;
   }
   
   public void addFileSet(FileSet fileSet) {
      resources.add(fileSet);
   }
   
   public void addProperty(Property prop) {
      properties.add(prop);
   }
   
   private void validate() throws BuildException {
      if ( resources.size() == 0 ) {
         if ( srcFile == null ) {
            throw new BuildException("srcfile or resource collection must be given");
         }
         
         // srcfile is set, verify dest file or dest dir is set
         if ( destFile == null && destDir == null ) {
            throw new BuildException("destfile required when srcfile is given");
         }
         
         if ( destDir != null ) {
            destFile = new File(destDir, srcFile.getName());
         }
      } else {
         if ( srcFile != null ) {
            srcFile = null;
            log("srcfile ignored since resource collection is given", Project.MSG_WARN);
         }
         
         if ( destFile != null ) {
            destFile = null;
            log("destfile ignored since resource collection is given", Project.MSG_WARN);
         }
         
         // destination directory required when resource collection is given
         if ( destDir == null ) {
            throw new BuildException("destdir required when resource collection is given");
         }
      }
   }
   
   @Override
   public void execute() throws BuildException {
      validate();
      
      Hashtable env;
      if ( inheritAll ) {
         env = getProject().getProperties();
      } else {
         env = new Hashtable();
      }
      
      for (Property prop : properties) {
         env.put(prop.getName(), prop.toString());
      }
      
      JavaPp pp = new JavaPp(prefix, env);
      
      if ( destDir != null && !destDir.exists() ) {
         if ( ! destDir.mkdirs() ) {
            throw new BuildException("unable to create destination directory");
         }
      }
      
      if ( srcFile != null ) {
         File parent = destFile.getParentFile();
         if ( !parent.exists() ) {
            if ( !parent.mkdirs() ) {
               throw new BuildException("unable to create parent directory of destination file");
            }
         }
         
         if ( verbose )
            log(String.format("processing %s", srcFile.getName()));
         
         try {
            pp.process(srcFile, destFile);
         } catch (JavaPpException e) {
            destFile.delete();
            throw new BuildException(e.getMessage());
         } catch (IOException e) {
            throw new BuildException(e);
         }
      } else {
         for (FileSet fs : resources) {
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            File baseDir = ds.getBasedir(), src, dest = null, parent;
            
            for (String file : ds.getIncludedFiles()) {
               
               try {
                  src = new File(baseDir, file);
                  dest = new File(destDir, file);
                  parent = dest.getParentFile();
                  
                  if ( !parent.exists() ) {
                     if ( !parent.mkdirs() ) {
                        throw new BuildException("unable to create parent directory of destination file");
                     }
                  }
                  
                  if ( verbose )
                     log(String.format("processing %s", file));
                  
                  if ( dest.exists() ) {
                     if ( FileUtils.getFileUtils().isUpToDate(src, dest) ) {
                        if ( verbose )
                           log("destination file is uptodate, processing skipped");
                        
                        continue;
                     }
                  }
                  
                  pp.process(src, dest);
               } catch (JavaPpException e) {
                  if ( dest != null )
                     dest.delete();
                  
                  throw new BuildException(e.getMessage());
               } catch (IOException e) {
                  throw new BuildException(e);
               }
            }
         }
      }
   }
}
