/**
 * JAFER Toolkit Poject.
 * Copyright (C) 2002, JAFER Toolkit Project, Oxford University.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.jafer.util.xml;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.jafer.exception.JaferException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Node;

/**
 * <p>Provides methods for serializing xml to a file, stream or writer.
 * Can also transform the xml prior to serialization using transformOutput methods.
 * NB use OutputStreams instead of Writers to preserve the required output character encoding</p>
 * @author Antony Corfield; Matthew Dovey; Colin Tatham
 * @version 1.0
 */
public class XMLSerializer {

  private static Logger logger = LoggerFactory.getLogger("org.jafer.util");
  
  private static TransformerFactory tFactory = TransformerFactory.newInstance();

  public static void out(Node node, boolean omitXMLDeclaration, OutputStream stream) throws JaferException {

    Transformer transformer = getTransformer(getDefaultProperties("xml", omitXMLDeclaration));
    XMLTransformer.transform(node, transformer, stream);
  }

  public static void out(Node node, boolean omitXMLDeclaration, Writer writer) throws JaferException {

    Transformer transformer = getTransformer(getDefaultProperties("xml", omitXMLDeclaration));
    XMLTransformer.transform(node, transformer, writer);
  }

  public static void out(Node node, boolean omitXMLDeclaration, String filePath) throws JaferException {

    out(node, omitXMLDeclaration, getFileOutputStream(filePath));
  }

  public static void out(Node node, String method, OutputStream stream) throws JaferException {

    Transformer transformer = getTransformer(getDefaultProperties(method));
    XMLTransformer.transform(node, transformer, stream);
  }

  public static void out(Node node, String method, Writer writer) throws JaferException {

    Transformer transformer = getTransformer(getDefaultProperties(method));
    XMLTransformer.transform(node, transformer, writer);
  }

  public static void out(Node node, String method, String filePath) throws JaferException {

    out(node, method, getFileOutputStream(filePath));
  }

  public static void transformOutput(Node sourceNode, URL stylesheet, OutputStream stream) throws JaferException {

    Transformer transformer = getTransformer(stylesheet);
    XMLTransformer.transform(sourceNode, transformer, stream);
  }

  public static void transformOutput(Node sourceNode, URL stylesheet, Writer writer) throws JaferException {

    Transformer transformer = getTransformer(stylesheet);
    XMLTransformer.transform(sourceNode, transformer, writer);
  }

  public static void transformOutput(Node sourceNode, URL stylesheet, String filePath) throws JaferException {

    transformOutput(sourceNode, stylesheet, getFileOutputStream(filePath));
  }

  public static void transformOutput(Node sourceNode, URL stylesheet, Map<String,?> parameters, OutputStream stream) throws JaferException {

    Transformer transformer = getTransformer(stylesheet, parameters);
    XMLTransformer.transform(sourceNode, transformer, stream);
  }

  public static void transformOutput(Node sourceNode, URL stylesheet, Map<String,?> parameters, Writer writer) throws JaferException {

    Transformer transformer = getTransformer(stylesheet, parameters);
    XMLTransformer.transform(sourceNode, transformer, writer);
  }

  public static void transformOutput(Node sourceNode, URL stylesheet, Map<String,?> parameters, String filePath) throws JaferException {

    transformOutput(sourceNode, stylesheet, parameters, getFileOutputStream(filePath));
  }

  private static HashMap<String,String> MEDIA_TYPES = getMediaTypeMap();

  private static HashMap<String,String> getMediaTypeMap() {
      HashMap<String,String> result = new HashMap<String,String>();
      result.put("xml", "text/xml");
      result.put("html", "text/html");
      result.put("xhtml", "text/xml");
      result.put("text", "text/plain");
      return result;
  }

  public static Properties getDefaultProperties(String method) throws JaferException {

    method = method.toLowerCase();

    if (method.equals("xml") || method.equals("html") || method.equals("xhtml") || method.equals("text")) {
//      return OutputProperties.getDefaultMethodProperties(method);
      Properties props = new Properties();
      props.put(OutputKeys.METHOD, method);
      props.put(OutputKeys.VERSION, "1.0");
      props.put(OutputKeys.ENCODING, "UTF-8");
      props.put(OutputKeys.INDENT, "no");
      props.put(OutputKeys.OMIT_XML_DECLARATION, "no");
      props.put(OutputKeys.STANDALONE, "no");
      props.put(OutputKeys.MEDIA_TYPE, MEDIA_TYPES.get(method));
      return props;
    } else {
      throw new JaferException("Method supplied must be \"xml\", \"html\", \"xhtml\", or \"text\"");
    }
  }

  private static Properties getDefaultProperties(String method, boolean omitXMLDeclaration) throws JaferException {

    Properties properties = getDefaultProperties(method);
    if (omitXMLDeclaration)
      properties.setProperty("omit-xml-declaration", "yes");
    return properties;
  }

  private static Transformer getTransformer() throws JaferException {
    return getTransformer((InputStream)null);
  }

  private static Transformer getTransformer(URL stylesheet) throws JaferException {

    try {
      return getTransformer(stylesheet.openStream());
    }
    catch (IOException e) {
      logger.error("XMLSerializer (Error in transformation: {})", stylesheet, e);
      throw new JaferException(e.getMessage(), e);
	}
  }

  private static Transformer getTransformer(InputStream stylesheet) throws JaferException {

	    try {
	    	Transformer transformer;
	    	synchronized(tFactory) {
	    		transformer = (stylesheet == null) ? tFactory.newTransformer() : tFactory.newTransformer(new StreamSource(stylesheet));
	    	}
	    	transformer.setErrorListener(new org.jafer.util.xml.ErrorListener());
	    	return transformer;
	    }
	    catch (TransformerConfigurationException e) {
	        logger.error("XMLSerializer (Error in transformation: {})", stylesheet, e);
	        throw new JaferException(e.getMessage(), e);
	    }
	  }

  private static Transformer getTransformer(URL stylesheet, Map<String, ?> parameters) throws JaferException {

    Transformer transformer = getTransformer(stylesheet);
    Iterator<String> keys = parameters.keySet().iterator();
    while (keys.hasNext()) {
      String param = keys.next();
      transformer.setParameter(param, parameters.get(param));
    }
    return transformer;
  }

  private static Transformer getTransformer(Properties properties) throws JaferException {

      Transformer transformer = getTransformer();
      transformer.setOutputProperties(properties);
      return transformer;
  }

  private static FileOutputStream getFileOutputStream(String filePath) throws JaferException {

    try {
      return new FileOutputStream(filePath);
    }
    catch (IOException e) {
        logger.error("XMLSerializer (Error in transformation: {})", filePath, e);
        throw new JaferException(e.getMessage(), e);
    }
  }
}