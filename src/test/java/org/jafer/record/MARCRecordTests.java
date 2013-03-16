package org.jafer.record;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.jafer.exception.JaferException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

public class MARCRecordTests {
  @Before
  public void setUp(){
	  
  }
  
  @After
  public void tearDown(){
	  
  }
  
  @Test
  public void testBytesToInt(){
	  byte [] buf = new byte[]{0x61, 0x31, 0x30, 0x32, 0x34, 0x61};
	  int actual = MARCRecord.getInt(buf, 1, 4);
	  String oldMethod = new String(buf,1,4);
	  int expected = Integer.parseInt(oldMethod);
	  assertEquals(expected, actual);
  }
  
  @Test
  public void testProcessFieldNoSubfields() throws IOException, JaferException{
	  Charset encoding = MARCRecord.MARC8_CHARSET;
	  byte [] bytes = "foo".getBytes(encoding);
	  ByteArrayInputStream byteIn = new ByteArrayInputStream(bytes);
	  Document document = mock(Document.class);
	  Node field = mock(Node.class);
	  Text textNode = mock(Text.class);
	  when(document.createTextNode(anyString())).thenReturn(textNode);
	  MARCRecord.processField(document, field, byteIn, encoding);
	  verify(document).createTextNode("\"foo\"");
  }
  
  @Test
  public void testProcessFieldWithSubfields() throws IOException, JaferException{
	  Charset encoding = MARCRecord.MARC8_CHARSET;
	  // "  \u001fa965hrportal"
	  byte [] bytes = new byte[]{0x20, 0x20, 0x1f, 0x61, 0x39, 0x36, 0x35, 0x68, 0x72, 0x70, 0x6f, 0x72, 0x74, 0x61, 0x6c};
	  Document document = mock(Document.class);
	  Element field = mock(Element.class);
	  Element subfield = mock(Element.class);
	  Text textNode = mock(Text.class);
	  when(document.createTextNode(anyString())).thenReturn(textNode);
	  when(document.createElementNS(MARCRecord.OAI_NAMESPACE, "subfield")).thenReturn(subfield);
	  MARCRecord.processField(document, field, bytes, encoding);
	  verify(document).createTextNode("965hrportal");
	  verify(subfield).setAttribute("label", "a");
  }

  @Test
  public void testProcessFieldWithMultiValueSubfields() throws IOException, JaferException{
	  Charset encoding = MARCRecord.MARC8_CHARSET;
	  // "  \u001fa965hrportal\u001fclatroprh569\u001fb965hrportal"
	  byte [] bytes = new byte[]{0x20, 0x20, 0x1f, 0x61, 0x39, 0x36, 0x35, 0x68, 0x72, 0x70, 0x6f, 0x72, 0x74, 0x61, 0x6c,
			                                 0x1f, 0x63, 0x6c, 0x61, 0x74, 0x72, 0x6f, 0x70, 0x72, 0x68, 0x35, 0x36, 0x39,
			                                 0x1f, 0x62, 0x39, 0x36, 0x35, 0x68, 0x72, 0x70, 0x6f, 0x72, 0x74, 0x61, 0x6c};
	  Document document = mock(Document.class);
	  Element field = mock(Element.class);
	  Element subfield1 = mock(Element.class);
	  Element subfield2 = mock(Element.class);
	  Element subfield3 = mock(Element.class);
	  Text textNode = mock(Text.class);
	  when(document.createTextNode(anyString())).thenReturn(textNode);
	  when(document.createElementNS(MARCRecord.OAI_NAMESPACE, "subfield")).thenReturn(subfield1).thenReturn(subfield2).thenReturn(subfield3);
	  MARCRecord.processField(document, field, bytes, encoding);
	  verify(document, times(2)).createTextNode("965hrportal");
	  verify(document).createTextNode("latroprh569");
	  verify(subfield1).setAttribute("label", "a");
	  verify(subfield2).setAttribute("label", "c");
	  verify(subfield3).setAttribute("label", "b");
  }

  @Test
  public void testProcessFieldWithMultiValueSubfieldsAndOffset() throws IOException, JaferException{
	  Charset encoding = MARCRecord.MARC8_CHARSET;
	  // "  \u001fa965hrportal\u001fclatroprh569"
	  byte [] bytes = new byte[]{0x23, 0x23, // filler
			                     0x20, 0x20, 
			                     0x1f, 0x61, 0x39, 0x36, 0x35, 0x68, 0x72, 0x70, 0x6f, 0x72, 0x74, 0x61, 0x6c,
			                     0x1f, 0x63, 0x6c, 0x61, 0x74, 0x72, 0x6f, 0x70, 0x72, 0x68, 0x35, 0x36, 0x39,
			                     0x23, 0x23}; // filler
	  Document document = mock(Document.class);
	  Element field = mock(Element.class);
	  Element subfield1 = mock(Element.class);
	  Element subfield2 = mock(Element.class);
	  Text textNode = mock(Text.class);
	  when(document.createTextNode(anyString())).thenReturn(textNode);
	  when(document.createElementNS(MARCRecord.OAI_NAMESPACE, "subfield")).thenReturn(subfield1).thenReturn(subfield2);
	  MARCRecord.processField(document, field, bytes, 2, bytes.length-4, encoding);
	  verify(document).createTextNode("965hrportal");
	  verify(document).createTextNode("latroprh569");
	  verify(subfield1).setAttribute("label", "a");
	  verify(subfield2).setAttribute("label", "c");
  }
  @Test
  public void testProcessFieldWithMultiValueSubfieldsAndBadLength() throws IOException, JaferException{
	  Charset encoding = MARCRecord.MARC8_CHARSET;
	  // "  \u001fa965hrportal\u001fclatroprh569"
	  byte [] bytes = new byte[]{0x23, 0x23, // filler
			                     0x20, 0x20, 
			                     0x1f, 0x61, 0x39, 0x36, 0x35, 0x68, 0x72, 0x70, 0x6f, 0x72, 0x74, 0x61, 0x6c,
			                     0x1f, 0x63, 0x6c, 0x61, 0x74, 0x72, 0x6f, 0x70, 0x72, 0x68, 0x35, 0x36, 0x39,
			                     };
	  Document document = mock(Document.class);
	  Element field = mock(Element.class);
	  Element subfield1 = mock(Element.class);
	  Element subfield2 = mock(Element.class);
	  Text textNode = mock(Text.class);
	  when(document.createTextNode(anyString())).thenReturn(textNode);
	  when(document.createElementNS(MARCRecord.OAI_NAMESPACE, "subfield")).thenReturn(subfield1).thenReturn(subfield2);
	  MARCRecord.processField(document, field, bytes, 2, bytes.length, encoding);
	  verify(document).createTextNode("965hrportal");
	  verify(document).createTextNode("latroprh569");
	  verify(subfield1).setAttribute("label", "a");
	  verify(subfield2).setAttribute("label", "c");
  }
}
