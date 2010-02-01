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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Properties;

import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyException;
import org.python.core.PyFile;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.core.io.StreamIO;

public class JavaPp {
   private PyObject func;
   private PyDictionary env;
   private PyObject prefix;
   
   public JavaPp(String prefix, Hashtable<Object, Object> env) {
      Properties props = new Properties();
      props.setProperty("python.home", "javapp.jar");
      props.setProperty("python.cachedir.skip", "true");
      
      PySystemState.initialize(System.getProperties(), props);
      
	PySystemState sys = new PySystemState();
	
	PyObject importer = sys.getBuiltins().__getitem__(Py.newString("__import__"));
      PyObject module = importer.__call__(Py.newString("javapp"));
      func = module.__getattr__("process");
	
      Hashtable<PyObject, PyObject> table = new Hashtable<PyObject, PyObject>();
      PyObject pkey, pval;
      String val;
      
      // jython requires dict's to contain only PyObjects in key and value
      for (Object key : env.keySet()) {
         pkey = Py.java2py(key);
         val = env.get(key).toString();
         
         try {
            // first attempt to parse number as integer
            pval = Py.newInteger(Integer.parseInt(val));
         } catch (NumberFormatException ie) {
            try {
               // if not integer, try double
               pval = Py.newFloat(Double.parseDouble(val));
            } catch (NumberFormatException de) {
               // if neither integer or double, use string
               pval = Py.newString(val);
            }
         }
         
         table.put(pkey, pval);
      }
      
      this.env = new PyDictionary(table);
      this.prefix = Py.newString(prefix);
   }
   
   public void process(File input, File output) throws IOException, JavaPpException {
      InputStream in = null;
      OutputStream out = null;
      
      try {
         in = new FileInputStream(input);
         out = new FileOutputStream(output);
         
         // Plex will fail if input file is not in universal newline mode
         PyFile infile = new PyFile(new StreamIO(in, false), input.getName(), "rU", -1);
         
         func.__call__(new PyObject[] {
               infile, new PyFile(out), infile.name, env, prefix
         });
      } catch (PyException e) {
         throw new JavaPpException(e);
      } finally {
         if ( in != null ) in.close();
         if ( out != null ) out.close();
      }
   }
}
