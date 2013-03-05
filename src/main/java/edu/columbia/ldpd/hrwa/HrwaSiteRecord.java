package edu.columbia.ldpd.hrwa;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class HrwaSiteRecord {
	
	HashMap<String, String> singleValuedFields = new HashMap<String, String>();
	HashMap<String, ArrayList<String>> multiValuedFields = new HashMap<String, ArrayList<String>>();
	
	HashSet<String> singleValuedFieldNames = new HashSet<String>();
	HashSet<String> multiValuedFieldNames = new HashSet<String>();

	public HrwaSiteRecord(File solrXmlDocFile) {
		
		singleValuedFieldNames.add("bib_key");
		singleValuedFieldNames.add("title");
		singleValuedFieldNames.add("marc_005_last_modified");
		singleValuedFieldNames.add("organization_type");
		singleValuedFieldNames.add("organization_based_in");
		singleValuedFieldNames.add("summary");
		singleValuedFieldNames.add("crawl_date_start");
		singleValuedFieldNames.add("crawl_date_end");
		
		multiValuedFieldNames.add("alternate_title");
		multiValuedFieldNames.add("creator_name");
		multiValuedFieldNames.add("geographic_focus");
		multiValuedFieldNames.add("subject");
		multiValuedFieldNames.add("language");
		multiValuedFieldNames.add("original_urls");
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		Document doc;
		try {
			
			dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(solrXmlDocFile);
			doc.getDocumentElement().normalize();
			
			NodeList fieldElements = doc.getElementsByTagName("field");
			int numFieldElements = fieldElements.getLength();
			
			Element element;
			String fieldName;
			String fieldValue;
			for(int i = 0; i < numFieldElements; i++) {
				element = (Element)fieldElements.item(i);
				fieldName = element.getAttribute("name");
				fieldValue = element.getTextContent();
				
				if(singleValuedFieldNames.contains(fieldName)) {
					
					if(singleValuedFields.containsKey(fieldName)) {
						HrwaManager.writeToLog("Error: Expected field (" + fieldName + ") to be single valued, but multiple values were supplied by the following solr XML file: " + solrXmlDocFile.getPath(), true, HrwaManager.LOG_TYPE_ERROR);
						System.exit(HrwaManager.EXIT_CODE_ERROR);
					} else {
						singleValuedFields.put(fieldName, fieldValue);
					}
					
				}
				else if(multiValuedFieldNames.contains(fieldName)) {
					
					if(multiValuedFields.containsKey(fieldName)) {
						multiValuedFields.get(fieldName).add(fieldValue);
					} else {
						ArrayList<String> newArrayList = new ArrayList<String>();
						newArrayList.add(fieldValue);
						multiValuedFields.put(fieldName, newArrayList);
					}
					
				}
				
			}
			
		} catch (SAXException e) {
			e.printStackTrace();
			HrwaManager.writeToLog("Error encountered while parsing Solr XML file: " + solrXmlDocFile.getPath(), true, HrwaManager.LOG_TYPE_ERROR);
			System.exit(HrwaManager.EXIT_CODE_ERROR);
		} catch (IOException e) {
			e.printStackTrace();
			HrwaManager.writeToLog("Error encountered while trying to read Solr XML file: " + solrXmlDocFile.getPath(), true, HrwaManager.LOG_TYPE_ERROR);
			System.exit(HrwaManager.EXIT_CODE_ERROR);
		} catch (ParserConfigurationException e) {
			HrwaManager.writeToLog("Error encountered while trying to configure the parser for reading a Solr XML file: " + solrXmlDocFile.getPath(), true, HrwaManager.LOG_TYPE_ERROR);
			e.printStackTrace();
		}
		
	}
	
	public String getSingleValuedFieldValue(String singleValuedFieldName) {
		return this.singleValuedFields.get(singleValuedFieldName);
	}
	
	public ArrayList<String> getMultiValuedFieldValue(String multiValuedFieldName) {
		return this.multiValuedFields.get(multiValuedFieldName);
	}
	
}
