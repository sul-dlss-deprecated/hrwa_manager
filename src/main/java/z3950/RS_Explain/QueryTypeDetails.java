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
 * Generated by Zebulun ASN1tojava: 1998-09-08 03:15:21 UTC
 */

//----------------------------------------------------------------

package z3950.RS_Explain;
import asn1.*;
import z3950.v3.AttributeElement;
import z3950.v3.AttributeList;
import z3950.v3.AttributeSetId;
import z3950.v3.DatabaseName;
import z3950.v3.ElementSetName;
import z3950.v3.IntUnit;
import z3950.v3.InternationalString;
import z3950.v3.OtherInformation;
import z3950.v3.Specification;
import z3950.v3.StringOrNumeric;
import z3950.v3.Term;
import z3950.v3.Unit;

//================================================================
/**
 * Class for representing a <code>QueryTypeDetails</code> from <code>RecordSyntax-explain</code>
 *
 * <pre>
 * QueryTypeDetails ::=
 * CHOICE {
 *   private [0] IMPLICIT PrivateCapabilities
 *   rpn [1] IMPLICIT RpnCapabilities
 *   iso8777 [2] IMPLICIT Iso8777Capabilities
 *   z39-58 [100] IMPLICIT HumanString
 *   erpn [101] IMPLICIT RpnCapabilities
 *   rankedList [102] IMPLICIT HumanString
 * }
 * </pre>
 *
 * @version	$Release$ $Date$
 */

//----------------------------------------------------------------

public final class QueryTypeDetails extends ASN1Any
{

  public final static String VERSION = "Copyright (C) Hoylen Sue, 1998. 199809080315Z";

//----------------------------------------------------------------
/**
 * Default constructor for a QueryTypeDetails.
 */

public
QueryTypeDetails()
{
}

//----------------------------------------------------------------
/**
 * Constructor for a QueryTypeDetails from a BER encoding.
 * <p>
 *
 * @param ber the BER encoding.
 * @param check_tag will check tag if true, use false
 *         if the BER has been implicitly tagged. You should
 *         usually be passing true.
 * @exception	ASN1Exception if the BER encoding is bad.
 */

public
QueryTypeDetails(BEREncoding ber, boolean check_tag)
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
  // Null out all choices

  c_private = null;
  c_rpn = null;
  c_iso8777 = null;
  c_z39_58 = null;
  c_erpn = null;
  c_rankedList = null;

  // Try choice private
  if (ber.tag_get() == 0 &&
      ber.tag_type_get() == BEREncoding.CONTEXT_SPECIFIC_TAG) {
    c_private = new PrivateCapabilities(ber, false);
    return;
  }

  // Try choice rpn
  if (ber.tag_get() == 1 &&
      ber.tag_type_get() == BEREncoding.CONTEXT_SPECIFIC_TAG) {
    c_rpn = new RpnCapabilities(ber, false);
    return;
  }

  // Try choice iso8777
  if (ber.tag_get() == 2 &&
      ber.tag_type_get() == BEREncoding.CONTEXT_SPECIFIC_TAG) {
    c_iso8777 = new Iso8777Capabilities(ber, false);
    return;
  }

  // Try choice z39-58
  if (ber.tag_get() == 100 &&
      ber.tag_type_get() == BEREncoding.CONTEXT_SPECIFIC_TAG) {
    c_z39_58 = new HumanString(ber, false);
    return;
  }

  // Try choice erpn
  if (ber.tag_get() == 101 &&
      ber.tag_type_get() == BEREncoding.CONTEXT_SPECIFIC_TAG) {
    c_erpn = new RpnCapabilities(ber, false);
    return;
  }

  // Try choice rankedList
  if (ber.tag_get() == 102 &&
      ber.tag_type_get() == BEREncoding.CONTEXT_SPECIFIC_TAG) {
    c_rankedList = new HumanString(ber, false);
    return;
  }

  throw new ASN1Exception("Zebulun QueryTypeDetails: bad BER encoding: choice not matched");
}

//----------------------------------------------------------------
/**
 * Returns a BER encoding of QueryTypeDetails.
 *
 * @return	The BER encoding.
 * @exception	ASN1Exception Invalid or cannot be encoded.
 */

public BEREncoding
ber_encode()
       throws ASN1Exception
{
  BEREncoding chosen = null;

  // Encoding choice: c_private
  if (c_private != null) {
    chosen = c_private.ber_encode(BEREncoding.CONTEXT_SPECIFIC_TAG, 0);
  }

  // Encoding choice: c_rpn
  if (c_rpn != null) {
    if (chosen != null)
      throw new ASN1Exception("CHOICE multiply set");
    chosen = c_rpn.ber_encode(BEREncoding.CONTEXT_SPECIFIC_TAG, 1);
  }

  // Encoding choice: c_iso8777
  if (c_iso8777 != null) {
    if (chosen != null)
      throw new ASN1Exception("CHOICE multiply set");
    chosen = c_iso8777.ber_encode(BEREncoding.CONTEXT_SPECIFIC_TAG, 2);
  }

  // Encoding choice: c_z39_58
  if (c_z39_58 != null) {
    if (chosen != null)
      throw new ASN1Exception("CHOICE multiply set");
    chosen = c_z39_58.ber_encode(BEREncoding.CONTEXT_SPECIFIC_TAG, 100);
  }

  // Encoding choice: c_erpn
  if (c_erpn != null) {
    if (chosen != null)
      throw new ASN1Exception("CHOICE multiply set");
    chosen = c_erpn.ber_encode(BEREncoding.CONTEXT_SPECIFIC_TAG, 101);
  }

  // Encoding choice: c_rankedList
  if (c_rankedList != null) {
    if (chosen != null)
      throw new ASN1Exception("CHOICE multiply set");
    chosen = c_rankedList.ber_encode(BEREncoding.CONTEXT_SPECIFIC_TAG, 102);
  }

  // Check for error of having none of the choices set
  if (chosen == null)
    throw new ASN1Exception("CHOICE not set");

  return chosen;
}

//----------------------------------------------------------------

/**
 * Generating a BER encoding of the object
 * and implicitly tagging it.
 * <p>
 * This method is for internal use only. You should use
 * the ber_encode method that does not take a parameter.
 * <p>
 * This function should never be used, because this
 * production is a CHOICE.
 * It must never have an implicit tag.
 * <p>
 * An exception will be thrown if it is called.
 *
 * @param tag_type the type of the tag.
 * @param tag the tag.
 * @exception ASN1Exception if it cannot be BER encoded.
 */

public BEREncoding
ber_encode(int tag_type, int tag)
       throws ASN1Exception
{
  // This method must not be called!

  // Method is not available because this is a basic CHOICE
  // which does not have an explicit tag on it. So it is not
  // permitted to allow something else to apply an implicit
  // tag on it, otherwise the tag identifying which CHOICE
  // it is will be overwritten and lost.

  throw new ASN1EncodingException("Zebulun QueryTypeDetails: cannot implicitly tag");
}

//----------------------------------------------------------------
/**
 * Returns a new String object containing a text representing
 * of the QueryTypeDetails. 
 */

public String
toString()
{
  StringBuffer str = new StringBuffer("{");

  boolean found = false;

  if (c_private != null) {
    if (found)
      str.append("<ERROR: multiple CHOICE: private> ");
    found = true;
    str.append("private ");
  str.append(c_private);
  }

  if (c_rpn != null) {
    if (found)
      str.append("<ERROR: multiple CHOICE: rpn> ");
    found = true;
    str.append("rpn ");
  str.append(c_rpn);
  }

  if (c_iso8777 != null) {
    if (found)
      str.append("<ERROR: multiple CHOICE: iso8777> ");
    found = true;
    str.append("iso8777 ");
  str.append(c_iso8777);
  }

  if (c_z39_58 != null) {
    if (found)
      str.append("<ERROR: multiple CHOICE: z39-58> ");
    found = true;
    str.append("z39-58 ");
  str.append(c_z39_58);
  }

  if (c_erpn != null) {
    if (found)
      str.append("<ERROR: multiple CHOICE: erpn> ");
    found = true;
    str.append("erpn ");
  str.append(c_erpn);
  }

  if (c_rankedList != null) {
    if (found)
      str.append("<ERROR: multiple CHOICE: rankedList> ");
    found = true;
    str.append("rankedList ");
  str.append(c_rankedList);
  }

  str.append("}");

  return str.toString();
}

//----------------------------------------------------------------
/*
 * Internal variables for class.
 */

public PrivateCapabilities c_private;
public RpnCapabilities c_rpn;
public Iso8777Capabilities c_iso8777;
public HumanString c_z39_58;
public RpnCapabilities c_erpn;
public HumanString c_rankedList;

} // QueryTypeDetails

//----------------------------------------------------------------
//EOF
