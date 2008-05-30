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
import java.util.Hashtable;
import java.util.Properties;

import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyFile;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

public class JavaPp {
   private PythonInterpreter py;
   
   public JavaPp(String prefix, Hashtable<Object, Object> env) {
      Properties props = new Properties();
      props.setProperty("python.home", "javapp.jar");
      props.setProperty("python.cachedir.skip", "true");
      
      PythonInterpreter.initialize(System.getProperties(), props, new String[] {""});
      
      py = new PythonInterpreter();
      py.exec("from javapp import process");
      
      py.set("prefix", prefix);
      
      // jython requires dict's to contain only PyObjects in key and value
      Hashtable<PyObject, PyObject> table = new Hashtable<PyObject, PyObject>();
      for (Object key : env.keySet()) {
         table.put(Py.java2py(key), Py.java2py(env.get(key)));
      }
      
      py.set("env", new PyDictionary(table));
   }
   
   public void process(File input, File output) throws IOException {
      FileInputStream in = null;
      FileOutputStream out = null;
      
      try {
         in = new FileInputStream(input);
         out = new FileOutputStream(output);
         
         py.set("infile", new PyFile(in));
         py.set("outfile", new PyFile(out));
         
         py.exec("process(infile, outfile, env)");
      } finally {
         if ( in != null ) in.close();
         if ( out != null ) out.close();
      }
   }
}
