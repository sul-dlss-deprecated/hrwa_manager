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
 * Generated by Zebulun ASN1tojava: 1998-09-08 03:15:26 UTC
 */

//----------------------------------------------------------------

package z3950.ESFormat_PeriodicQuerySchedule;
import asn1.*;
import z3950.ESFormat_ExportSpec.Destination;
import z3950.ESFormat_ExportSpec.ExportSpecification;
import z3950.v3.IntUnit;
import z3950.v3.InternationalString;
import z3950.v3.Query;

//================================================================
/**
 * Class for representing a <code>OriginPartToKeep_exportParameters</code> from <code>ESFormat-PeriodicQuerySchedule</code>
 *
 * <pre>
 * OriginPartToKeep_exportParameters ::=
 * CHOICE {
 *   packageName [1] IMPLICIT InternationalString
 *   exportPackage [2] EXPLICIT ExportSpecification
 * }
 * </pre>
 *
 * @version	$Release$ $Date$
 */

//----------------------------------------------------------------

public final class OriginPartToKeep_exportParameters extends ASN1Any
{

  public final static String VERSION = "Copyright (C) Hoylen Sue, 1998. 199809080315Z";

//----------------------------------------------------------------
/**
 * Default constructor for a OriginPartToKeep_exportParameters.
 */

public
OriginPartToKeep_exportParameters()
{
}

//----------------------------------------------------------------
/**
 * Constructor for a OriginPartToKeep_exportParameters from a BER encoding.
 * <p>
 *
 * @param ber the BER encoding.
 * @param check_tag will check tag if true, use false
 *         if the BER has been implicitly tagged. You should
 *         usually be passing true.
 * @exception	ASN1Exception if the BER encoding is bad.
 */

public
OriginPartToKeep_exportParameters(BEREncoding ber, boolean check_tag)
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
  BERConstructed tagwrapper;

  // Null out all choices

  c_packageName = null;
  c_exportPackage = null;

  // Try choice packageName
  if (ber.tag_get() == 1 &&
      ber.tag_type_get() == BEREncoding.CONTEXT_SPECIFIC_TAG) {
    c_packageName = new InternationalString(ber, false);
    return;
  }

  // Try choice exportPackage
  if (ber.tag_get() == 2 &&
      ber.tag_type_get() == BEREncoding.CONTEXT_SPECIFIC_TAG) {
    try {
      tagwrapper = (BERConstructed) ber;
    } catch (ClassCastException e) {
      throw new ASN1EncodingException
        ("Zebulun OriginPartToKeep_exportParameters: bad BER form\n");
    }
    if (tagwrapper.number_components() != 1)
      throw new ASN1EncodingException
        ("Zebulun OriginPartToKeep_exportParameters: bad BER form\n");
    c_exportPackage = new ExportSpecification(tagwrapper.elementAt(0), true);
    return;
  }

  throw new ASN1Exception("Zebulun OriginPartToKeep_exportParameters: bad BER encoding: choice not matched");
}

//----------------------------------------------------------------
/**
 * Returns a BER encoding of OriginPartToKeep_exportParameters.
 *
 * @return	The BER encoding.
 * @exception	ASN1Exception Invalid or cannot be encoded.
 */

public BEREncoding
ber_encode()
       throws ASN1Exception
{
  BEREncoding chosen = null;

  BEREncoding enc[];

  // Encoding choice: c_packageName
  if (c_packageName != null) {
    chosen = c_packageName.ber_encode(BEREncoding.CONTEXT_SPECIFIC_TAG, 1);
  }

  // Encoding choice: c_exportPackage
  if (c_exportPackage != null) {
    if (chosen != null)
      throw new ASN1Exception("CHOICE multiply set");
    enc = new BEREncoding[1];
    enc[0] = c_exportPackage.ber_encode();
    chosen = new BERConstructed(BEREncoding.CONTEXT_SPECIFIC_TAG, 2, enc);
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

  throw new ASN1EncodingException("Zebulun OriginPartToKeep_exportParameters: cannot implicitly tag");
}

//----------------------------------------------------------------
/**
 * Returns a new String object containing a text representing
 * of the OriginPartToKeep_exportParameters. 
 */

public String
toString()
{
  StringBuffer str = new StringBuffer("{");

  boolean found = false;

  if (c_packageName != null) {
    if (found)
      str.append("<ERROR: multiple CHOICE: packageName> ");
    found = true;
    str.append("packageName ");
  str.append(c_packageName);
  }

  if (c_exportPackage != null) {
    if (found)
      str.append("<ERROR: multiple CHOICE: exportPackage> ");
    found = true;
    str.append("exportPackage ");
  str.append(c_exportPackage);
  }

  str.append("}");

  return str.toString();
}

//----------------------------------------------------------------
/*
 * Internal variables for class.
 */

public InternationalString c_packageName;
public ExportSpecification c_exportPackage;

} // OriginPartToKeep_exportParameters

//----------------------------------------------------------------
//EOF
