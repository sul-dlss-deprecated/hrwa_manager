/*
 * $Source$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 1998, Hoylen Sue.  All Rights Reserved.
 * <h.sue@ieee.org>
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  Refer to
 * the supplied license for more details.
 *
 * Generated by Zebulun ASN1tojava: 1998-09-08 03:15:12 UTC
 */

//----------------------------------------------------------------

package z3950.DiagFormat;
import asn1.*;
import z3950.v3.AttributeList;
import z3950.v3.DatabaseName;
import z3950.v3.DefaultDiagFormat;
import z3950.v3.InternationalString;
import z3950.v3.SortElement;
import z3950.v3.Specification;
import z3950.v3.Term;

//================================================================
/**
 * Class for representing a <code>DiagFormat_attribute</code> from <code>DiagnosticFormatDiag1</code>
 *
 * <pre>
 * DiagFormat_attribute ::=
 * SEQUENCE {
 *   id [1] IMPLICIT OBJECT IDENTIFIER
 *   type [2] IMPLICIT INTEGER OPTIONAL
 *   value [3] IMPLICIT INTEGER OPTIONAL
 *   term [4] EXPLICIT Term OPTIONAL
 * }
 * </pre>
 *
 * @version	$Release$ $Date$
 */

//----------------------------------------------------------------

public final class DiagFormat_attribute extends ASN1Any
{

  public final static String VERSION = "Copyright (C) Hoylen Sue, 1998. 199809080315Z";

//----------------------------------------------------------------
/**
 * Default constructor for a DiagFormat_attribute.
 */

public
DiagFormat_attribute()
{
}

//----------------------------------------------------------------
/**
 * Constructor for a DiagFormat_attribute from a BER encoding.
 * <p>
 *
 * @param ber the BER encoding.
 * @param check_tag will check tag if true, use false
 *         if the BER has been implicitly tagged. You should
 *         usually be passing true.
 * @exception	ASN1Exception if the BER encoding is bad.
 */

public
DiagFormat_attribute(BEREncoding ber, boolean check_tag)
       throws ASN1Exception
{
  super(ber, check_tag);
}

//----------------------------------------------------------------
/**
 * Initializing object from a BER encoding.
 * This method is for internal use only. You should use
 * the constructor that takes a BEREncoding.
 *
 * @param ber the BER to decode.
 * @param check_tag if the tag should be checked.
 * @exception ASN1Exception if the BER encoding is bad.
 */

public void
ber_decode(BEREncoding ber, boolean check_tag)
       throws ASN1Exception
{
  // DiagFormat_attribute should be encoded by a constructed BER

  BERConstructed ber_cons;
  try {
    ber_cons = (BERConstructed) ber;
  } catch (ClassCastException e) {
    throw new ASN1EncodingException
      ("Zebulun DiagFormat_attribute: bad BER form\n");
  }

  // Prepare to decode the components

  int num_parts = ber_cons.number_components();
  int part = 0;
  BEREncoding p;
  BERConstructed tagged;

  // Decoding: id [1] IMPLICIT OBJECT IDENTIFIER

  if (num_parts <= part) {
    // End of record, but still more elements to get
    throw new ASN1Exception("Zebulun DiagFormat_attribute: incomplete");
  }
  p = ber_cons.elementAt(part);

  if (p.tag_get() != 1 ||
      p.tag_type_get() != BEREncoding.CONTEXT_SPECIFIC_TAG)
    throw new ASN1EncodingException
      ("Zebulun DiagFormat_attribute: bad tag in s_id\n");

  s_id = new ASN1ObjectIdentifier(p, false);
  part++;

  // Remaining elements are optional, set variables
  // to null (not present) so can return at end of BER

  s_type = null;
  s_value = null;
  s_term = null;

  // Decoding: type [2] IMPLICIT INTEGER OPTIONAL

  if (num_parts <= part) {
    return; // no more data, but ok (rest is optional)
  }
  p = ber_cons.elementAt(part);

  if (p.tag_get() == 2 &&
      p.tag_type_get() == BEREncoding.CONTEXT_SPECIFIC_TAG) {
    s_type = new ASN1Integer(p, false);
    part++;
  }

  // Decoding: value [3] IMPLICIT INTEGER OPTIONAL

  if (num_parts <= part) {
    return; // no more data, but ok (rest is optional)
  }
  p = ber_cons.elementAt(part);

  if (p.tag_get() == 3 &&
      p.tag_type_get() == BEREncoding.CONTEXT_SPECIFIC_TAG) {
    s_value = new ASN1Integer(p, false);
    part++;
  }

  // Decoding: term [4] EXPLICIT Term OPTIONAL

  if (num_parts <= part) {
    return; // no more data, but ok (rest is optional)
  }
  p = ber_cons.elementAt(part);

  if (p.tag_get() == 4 &&
      p.tag_type_get() == BEREncoding.CONTEXT_SPECIFIC_TAG) {
    try {
      tagged = (BERConstructed) p;
    } catch (ClassCastException e) {
      throw new ASN1EncodingException
        ("Zebulun DiagFormat_attribute: bad BER encoding: s_term tag bad\n");
    }
    if (tagged.number_components() != 1) {
      throw new ASN1EncodingException
        ("Zebulun DiagFormat_attribute: bad BER encoding: s_term tag bad\n");
    }

    s_term = new Term(tagged.elementAt(0), true);
    part++;
  }

  // Should not be any more parts

  if (part < num_parts) {
    throw new ASN1Exception("Zebulun DiagFormat_attribute: bad BER: extra data " + part + "/" + num_parts + " processed");
  }
}

//----------------------------------------------------------------
/**
 * Returns a BER encoding of the DiagFormat_attribute.
 *
 * @exception	ASN1Exception Invalid or cannot be encoded.
 * @return	The BER encoding.
 */

public BEREncoding
ber_encode()
       throws ASN1Exception
{
  return ber_encode(BEREncoding.UNIVERSAL_TAG, ASN1Sequence.TAG);
}

//----------------------------------------------------------------
/**
 * Returns a BER encoding of DiagFormat_attribute, implicitly tagged.
 *
 * @param tag_type	The type of the implicit tag.
 * @param tag	The implicit tag.
 * @return	The BER encoding of the object.
 * @exception	ASN1Exception When invalid or cannot be encoded.
 * @see asn1.BEREncoding#UNIVERSAL_TAG
 * @see asn1.BEREncoding#APPLICATION_TAG
 * @see asn1.BEREncoding#CONTEXT_SPECIFIC_TAG
 * @see asn1.BEREncoding#PRIVATE_TAG
 */

public BEREncoding
ber_encode(int tag_type, int tag)
       throws ASN1Exception
{
  // Calculate the number of fields in the encoding

  int num_fields = 1; // number of mandatories
  if (s_type != null)
    num_fields++;
  if (s_value != null)
    num_fields++;
  if (s_term != null)
    num_fields++;

  // Encode it

  BEREncoding fields[] = new BEREncoding[num_fields];
  int x = 0;
  BEREncoding enc[];

  // Encoding s_id: OBJECT IDENTIFIER 

  fields[x++] = s_id.ber_encode(BEREncoding.CONTEXT_SPECIFIC_TAG, 1);

  // Encoding s_type: INTEGER OPTIONAL

  if (s_type != null) {
    fields[x++] = s_type.ber_encode(BEREncoding.CONTEXT_SPECIFIC_TAG, 2);
  }

  // Encoding s_value: INTEGER OPTIONAL

  if (s_value != null) {
    fields[x++] = s_value.ber_encode(BEREncoding.CONTEXT_SPECIFIC_TAG, 3);
  }

  // Encoding s_term: Term OPTIONAL

  if (s_term != null) {
    enc = new BEREncoding[1];
    enc[0] = s_term.ber_encode();
    fields[x++] = new BERConstructed(BEREncoding.CONTEXT_SPECIFIC_TAG, 4, enc);
  }

  return new BERConstructed(tag_type, tag, fields);
}

//----------------------------------------------------------------
/**
 * Returns a new String object containing a text representing
 * of the DiagFormat_attribute. 
 */

public String
toString()
{
  StringBuffer str = new StringBuffer("{");
  int outputted = 0;

  str.append("id ");
  str.append(s_id);
  outputted++;

  if (s_type != null) {
    if (0 < outputted)
    str.append(", ");
    str.append("type ");
    str.append(s_type);
    outputted++;
  }

  if (s_value != null) {
    if (0 < outputted)
    str.append(", ");
    str.append("value ");
    str.append(s_value);
    outputted++;
  }

  if (s_term != null) {
    if (0 < outputted)
    str.append(", ");
    str.append("term ");
    str.append(s_term);
    outputted++;
  }

  str.append("}");

  return str.toString();
}

//----------------------------------------------------------------
/*
 * Internal variables for class.
 */

public ASN1ObjectIdentifier s_id;
public ASN1Integer s_type; // optional
public ASN1Integer s_value; // optional
public Term s_term; // optional

} // DiagFormat_attribute

//----------------------------------------------------------------
//EOF
