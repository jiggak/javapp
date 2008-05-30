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

public class JavaPpTask extends Task {
   private String prefix = "#";
   
   private boolean inheritAll = true;
   
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
         
         // srcfile is set, verify destfile is set too
         if ( destFile == null ) {
            throw new BuildException("destfile required when srcfile is given");
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
      
      if ( srcFile != null ) {
         try {
            pp.process(srcFile, destFile);
         } catch (IOException e) {
            throw new BuildException(e);
         }
      } else {
         for (FileSet fs : resources) {
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            String[] includedFiles = ds.getIncludedFiles();
         }
      }
   }
}
