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
import org.jafer.exception.JaferException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;
import java.util.Iterator;
//Imported TraX classes
import javax.xml.transform.Result;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.Templates;
// Imported DOM classes
import org.w3c.dom.Node;
// Imported Logger classes
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Provides methods for transforming xml using a templates object or by specifying a file, or URL for the styleSheet.
 * NB use OutputStreams instead of Writers to preserve the required output character encoding</p>
 * @author Antony Corfield; Matthew Dovey; Colin Tatham
 * @version 1.0
 */
public class XMLTransformer {

  private static Logger logger = LoggerFactory.getLogger(XMLTransformer.class);
  private static TransformerFactory tFactory = TransformerFactory.newInstance();

  static {
	  if(!(tFactory.getFeature(DOMSource.FEATURE) &&
			  tFactory.getFeature(DOMResult.FEATURE))) {
		  logger.error("DOM node processing not supported - cannot continue!");
		  System.exit(-1);
	  }
  }

  public static Node transform(Node sourceNode, Transformer transformer) throws JaferException {

    logger.trace("entering public static Node transform(Node sourceNode, Transformer transformer)");

    try {
    	DOMResult domResult = new DOMResult(sourceNode.cloneNode(false));
    	transform(sourceNode, transformer, domResult);
    	return domResult.getNode().getFirstChild();
    } finally {
      logger.trace("exiting public static Node transform(Node sourceNode, Transformer transformer)");
    }
  }

  public static void transform(Node sourceNode, Transformer transformer, OutputStream stream) throws JaferException {

    logger.trace("entering public static void transform(Node sourceNode, Transformer transformer, OutputStream stream");

    try {
        transform(sourceNode, transformer, new StreamResult(stream));
    } finally {
      logger.trace("exiting public static void transform(Node sourceNode, Transformer transformer, OutputStream stream");
    }
  }

  public static void transform(Node sourceNode, Transformer transformer, Writer writer) throws JaferException {

    logger.trace("entering public static void transform(Node sourceNode, Transformer transformer, Writer writer)");

    try {
      transform(sourceNode, transformer, new StreamResult(writer));
    } finally {
      logger.trace("exiting public static void transform(Node sourceNode, Transformer transformer, Writer writer)");
    }
  }
  
  private static void transform(Node sourceNode, Transformer transformer, Result streamResult) throws JaferException {
	    logger.trace("entering public static void transform(Node sourceNode, Transformer transformer, StreamResult streamResult)");

	    try {
	      DOMSource domSource = new DOMSource(sourceNode);
	      transformer.transform(domSource, streamResult);
	    } catch (TransformerException e) {
	      logger.error("XMLTransformer; cannot transform node.", e);
	      throw new JaferException(e.getMessage(), e);
	    } catch (IllegalArgumentException e) {// eg. node is null
		      logger.error("XMLTransformer; cannot transform node.", e);
		      throw new JaferException(e.getMessage(), e);
	    } catch (NullPointerException e) {// eg. node is null
		      logger.error("XMLTransformer; cannot transform node.", e);
		      throw new JaferException(e.getMessage(), e);
	    } finally {
	      logger.trace("exiting public static void transform(Node sourceNode, Transformer transformer, StreamResult streamResult)");
	    }
  }
  
  public static void transform(Node sourceNode, OutputStream out) {
	  try {
		  Transformer trans = null;
		  synchronized(tFactory){
			  trans = tFactory.newTransformer();
		  }
		  trans.transform(new DOMSource(sourceNode), new StreamResult(out));
	  } catch (TransformerException e) {
		e.printStackTrace();
	}
  }


  public static Node transform(Node sourceNode, Templates template) throws JaferException {

    logger.trace("entering public static Node transform(Node sourceNode, Templates template)");

    try {// Create Transformer object from thread safe templates object
      return transform(sourceNode, template.newTransformer());
    } catch (TransformerConfigurationException e) {
      logger.error("XMLTransformer; cannot create transformer object from template. ", e);
      throw new JaferException(e.getMessage(), e);
    } finally {
      logger.trace("exiting public static Node transform(Node sourceNode, Templates template)");
    }
  }

  public static Node transform(Node sourceNode, InputStream stream) throws JaferException {
      logger.trace("entering public static Node transform(Node sourceNode, InputStream stream)");
      try {
        return transform(sourceNode, new StreamSource(stream));
      } finally {
        logger.trace("exiting public static Node transform(Node sourceNode, InputStream stream)");
      }
  }
  
  public static Node transform(Map<String, String> paramMap, Node sourceNode, StreamSource stream) throws JaferException {

      logger.trace("entering public static Node transform(Map paramMap, Node sourceNode, StreamSource stream)");

      Transformer transformer = null;
      try {
        synchronized (tFactory) {
        	transformer = tFactory.newTransformer(stream);
        }
        Iterator<String> keys = paramMap.keySet().iterator();
        while (keys.hasNext()) {
          String param = keys.next();
          String value = paramMap.get(param);
          transformer.setParameter(param, value);
        }
        return transform(sourceNode, transformer);
      } catch (TransformerConfigurationException e) {
        logger.error("XMLTransformer; cannot create transformer object from stylesheet input stream. ", e);
        throw new JaferException(e.getMessage(), e);
      } finally {
        logger.trace("exiting public static Node transform(Map paramMap, Node sourceNode, StreamSource stream)");
      }
  }

  
  public static Node transform(Map<String, String> paramMap, Node sourceNode, InputStream stream) throws JaferException {

      logger.trace("entering public static Node transform(Map paramMap, Node sourceNode, InputStream stream)");

      try {
        return transform(paramMap, sourceNode, new StreamSource(stream));
      } finally {
        logger.trace("exiting public static Node transform(Map paramMap, Node sourceNode, InputStream stream)");
      }
  }
  
  public static Node transform(Node sourceNode, String path) throws JaferException {
	  logger.trace("entering public static Node transform(Node sourceNode, String path)");
	  try {
		  return transform(sourceNode, new StreamSource(path));
	  } finally {
	      logger.trace("exiting public static Node transform(Node sourceNode, String path)");
	  }
  }
  
  public static Node transform(Node sourceNode, StreamSource transformSource) throws JaferException {

    logger.trace("entering public static Node transform(Node sourceNode, StreamSource transformSource)");

    Transformer transformer = null;
    try {
      synchronized (tFactory) {
    	  transformer = tFactory.newTransformer(transformSource);
      }
      return transform(sourceNode, transformer);
    } catch (TransformerConfigurationException e) {
      String message = "XMLTransformer; cannot create transformer object from styleSheet " + transformSource.getSystemId() + ". " + e.toString();
      logger.error(message);
      throw new JaferException(message, e);
    } catch (NullPointerException e) {
      String message = "XMLTransformer; cannot transform node, NULL template. " + e.toString();
      logger.error(message);
      throw new JaferException(message, e);
    } finally {
      logger.trace("exiting public static Node transform(Node sourceNode, StreamSource transformSource)");
    }
  }

  public static Node transform(Map<String, String> paramMap, Node sourceNode, String path) throws JaferException {

    logger.trace("entering public static Node transform(Map paramMap, Node sourceNode, String path)");

    try {
      return transform(paramMap, sourceNode, new StreamSource(path));
    } finally {
      logger.trace("exiting public static Node transform(Map paramMap, Node sourceNode, String path)");
    }
  }

  public static Node transform(Node sourceNode, URL resource) throws JaferException {

    logger.trace("entering public static Node transform(Node sourceNode, URL resource)");

    try {
      return transform(sourceNode, resource.openStream());
    } catch (NullPointerException e) {
      String message = "XMLTransformer; cannot transform node, NULL resource. " + e.toString();
      logger.error(message);
      throw new JaferException(message, e);
    } catch (IOException e) {
        String message = "XMLTransformer; cannot transform node, NULL resource. " + e.toString();
        logger.error(message);
        throw new JaferException(message, e);
	} finally {
      logger.trace("exiting public static Node transform(Node sourceNode, URL resource)");
    }
  }

  public static Node transform(Map<String, String> paramMap, Node sourceNode, URL resource) throws JaferException {

    logger.trace("entering public static Node transform(Map paramMap, Node sourceNode, URL resource)");

    try {
      return transform(paramMap, sourceNode, resource.openStream());
    } catch (IOException e) {
        String message = "XMLTransformer; cannot transform node, unreadable URL resource. " + e.toString();
        logger.error(message);
        throw new JaferException(message, e);
	} finally {
      logger.trace("exiting public static Node transform(Map paramMap, Node sourceNode, URL resource)");
    }
  }

  public static Node transform(Node sourceNode, File file) throws JaferException {

    logger.trace("entering public static Node transform(Node sourceNode, File file)");

    try {
      return transform(sourceNode, new FileInputStream(file));
    } catch (IOException e) {
        logger.error("XMLTransformer; cannot transform node, problem reading file. ", e);
        throw new JaferException(e.getMessage());
    } finally {
      logger.trace("exiting public static Node transform(Node sourceNode, File file)");
    }
  }

  public static Node transform(Map<String, String> paramMap, Node sourceNode, File file) throws JaferException {

    logger.trace("entering public static Node transform(Map paramMap, Node sourceNode, File file)");

    try {
    	if (file == null) {
    		logger.error("XMLTransformer; cannot transform node, NULL file.");
    		throw new JaferException("XMLTransformer; cannot transform node, NULL file.");
    	}
        return transform(paramMap, sourceNode, new FileInputStream(file));
    }catch (IOException e) {
        logger.error("XMLTransformer; cannot transform node, problem reading file. ", e);
        throw new JaferException(e.getMessage(), e);
    } finally {
      logger.trace("exiting public static Node transform(Map paramMap, Node sourceNode, File file)");
    }
  }

  public static Templates createTemplate(InputStream stream) throws JaferException {

      logger.trace("entering public static Templates createTemplate(InputStream stream)");

      try {
        return createTemplate(new StreamSource(stream));
      } finally {
        logger.trace("exiting public static Templates createTemplate(InputStream stream)");
      }
    }
  
  public static Templates createTemplate(StreamSource stream) throws JaferException {

      logger.trace("entering public static Templates createTemplate(StreamSource stream)");

      try {// Create a templates object, which is the processed, thread-safe representation of the stylesheet - NB. namespace?
        synchronized (tFactory) {
        	return tFactory.newTemplates(stream);
        }
      } catch (TransformerConfigurationException e) {
        logger.error("XMLTransformer; cannot create template using stylesheet from input stream. ", e);
        throw new JaferException(e.getMessage(), e);
      } finally {
        logger.trace("exiting public static Templates createTemplate(StreamSource stream)");
      }
    }

  public static Templates createTemplate(String path) throws JaferException {

	  logger.trace("entering public static Templates createTemplate(String path)");

	  try {
		  return createTemplate(new StreamSource(path));
	  } finally {
		  logger.trace("exiting public static Templates createTemplate(String path)");
	  }
  }

  public static Templates createTemplate(URL resource) throws JaferException {

	  logger.trace("entering public static Templates createTemplate(URL resource)");
	  try {
		  if (resource != null) {
			  return createTemplate(resource.openStream());
		  } else {
			  throw new JaferException("Resource necessary for creating XML transformer template not found");
		  }
	  } catch (IOException e) {
		  throw new JaferException("Could not read template content from " + resource.toExternalForm(), e);
	  } finally {
		  logger.trace("exiting public static Templates createTemplate(URL resource)");
	  }
  }

  public static Templates createTemplate(File file) throws JaferException {

	  logger.trace("entering public static Templates createTemplate(File file)");
	  try {
		  if (file != null) {
			  return createTemplate(new FileInputStream(file));
		  } else {
			  throw new JaferException("Resource necessary for creating XML transformer template not found");
		  }
	  } catch (FileNotFoundException e) {
		  throw new JaferException("Error reading XML transformer template file", e);
	  } finally {
		  logger.trace("exiting public static Templates createTemplate(File file)");
	  }
  }
}