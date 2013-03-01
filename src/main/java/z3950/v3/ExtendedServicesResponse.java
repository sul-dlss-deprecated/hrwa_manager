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
 * Generated by Zebulun ASN1tojava: 1998-09-08 03:15:05 UTC
 */

//----------------------------------------------------------------

package z3950.v3;
import asn1.*;


//================================================================
/**
 * Class for representing a <code>ExtendedServicesResponse</code> from <code>Z39-50-APDU-1995</code>
 *
 * <pre>
 * ExtendedServicesResponse ::=
 * SEQUENCE {
 *   referenceId ReferenceId OPTIONAL
 *   operationStatus [3] IMPLICIT INTEGER
 *   diagnostics [4] IMPLICIT SEQUENCE OF DiagRec OPTIONAL
 *   taskPackage [5] IMPLICIT EXTERNAL OPTIONAL
 *   otherInfo OtherInformation OPTIONAL
 * }
 * </pre>
 *
 * @version	$Release$ $Date$
 */

//----------------------------------------------------------------

public final class ExtendedServicesResponse extends ASN1Any
{

  public final static String VERSION = "Copyright (C) Hoylen Sue, 1998. 199809080315Z";

//----------------------------------------------------------------
/**
 * Default constructor for a ExtendedServicesResponse.
 */

public
ExtendedServicesResponse()
{
}

//----------------------------------------------------------------
/**
 * Constructor for a ExtendedServicesResponse from a BER encoding.
 * <p>
 *
 * @param ber the BER encoding.
 * @param check_tag will check tag if true, use false
 *         if the BER has been implicitly tagged. You should
 *         usually be passing true.
 * @exception	ASN1Exception if the BER encoding is bad.
 */

public
ExtendedServicesResponse(BEREncoding ber, boolean check_tag)
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
  // ExtendedServicesResponse should be encoded by a constructed BER

  BERConstructed ber_cons;
  try {
    ber_cons = (BERConstructed) ber;
  } catch (ClassCastException e) {
    throw new ASN1EncodingException
      ("Zebulun ExtendedServicesResponse: bad BER form\n");
  }

  // Prepare to decode the components

  int num_parts = ber_cons.number_components();
  int part = 0;
  BEREncoding p;

  // Decoding: referenceId ReferenceId OPTIONAL

  if (num_parts <= part) {
    // End of record, but still more elements to get
    throw new ASN1Exception("Zebulun ExtendedServicesResponse: incomplete");
  }
  p = ber_cons.elementAt(part);

  try {
    s_referenceId = new ReferenceId(p, true);
    part++; // yes, consumed
  } catch (ASN1Exception e) {
    s_referenceId = null; // no, not present
  }

  // Decoding: operationStatus [3] IMPLICIT INTEGER

  if (num_parts <= part) {
    // End of record, but still more elements to get
    throw new ASN1Exception("Zebulun ExtendedServicesResponse: incomplete");
  }
  p = ber_cons.elementAt(part);

  if (p.tag_get() != 3 ||
      p.tag_type_get() != BEREncoding.CONTEXT_SPECIFIC_TAG)
    throw new ASN1EncodingException
      ("Zebulun ExtendedServicesResponse: bad tag in s_operationStatus\n");

  s_operationStatus = new ASN1Integer(p, false);
  part++;

  // Remaining elements are optional, set variables
  // to null (not present) so can return at end of BER

  s_diagnostics = null;
  s_taskPackage = null;
  s_otherInfo = null;

  // Decoding: diagnostics [4] IMPLICIT SEQUENCE OF DiagRec OPTIONAL

  if (num_parts <= part) {
    return; // no more data, but ok (rest is optional)
  }
  p = ber_cons.elementAt(part);

  if (p.tag_get() == 4 &&
      p.tag_type_get() == BEREncoding.CONTEXT_SPECIFIC_TAG) {
    try {
      BERConstructed cons = (BERConstructed) p;
      int parts = cons.number_components();
      s_diagnostics = new DiagRec[parts];
      int n;
      for (n = 0; n < parts; n++) {
        s_diagnostics[n] = new DiagRec(cons.elementAt(n), true);
      }
    } catch (ClassCastException e) {
      throw new ASN1EncodingException("Bad BER");
    }
    part++;
  }

  // Decoding: taskPackage [5] IMPLICIT EXTERNAL OPTIONAL

  if (num_parts <= part) {
    return; // no more data, but ok (rest is optional)
  }
  p = ber_cons.elementAt(part);

  if (p.tag_get() == 5 &&
      p.tag_type_get() == BEREncoding.CONTEXT_SPECIFIC_TAG) {
    s_taskPackage = new ASN1External(p, false);
    part++;
  }

  // Decoding: otherInfo OtherInformation OPTIONAL

  if (num_parts <= part) {
    return; // no more data, but ok (rest is optional)
  }
  p = ber_cons.elementAt(part);

  try {
    s_otherInfo = new OtherInformation(p, true);
    part++; // yes, consumed
  } catch (ASN1Exception e) {
    s_otherInfo = null; // no, not present
  }

  // Should not be any more parts

  if (part < num_parts) {
    throw new ASN1Exception("Zebulun ExtendedServicesResponse: bad BER: extra data " + part + "/" + num_parts + " processed");
  }
}

//----------------------------------------------------------------
/**
 * Returns a BER encoding of the ExtendedServicesResponse.
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
 * Returns a BER encoding of ExtendedServicesResponse, implicitly tagged.
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
  if (s_referenceId != null)
    num_fields++;
  if (s_diagnostics != null)
    num_fields++;
  if (s_taskPackage != null)
    num_fields++;
  if (s_otherInfo != null)
    num_fields++;

  // Encode it

  BEREncoding fields[] = new BEREncoding[num_fields];
  int x = 0;
  BEREncoding f2[];
  int p;

  // Encoding s_referenceId: ReferenceId OPTIONAL

  if (s_referenceId != null) {
    fields[x++] = s_referenceId.ber_encode();
  }

  // Encoding s_operationStatus: INTEGER 

  fields[x++] = s_operationStatus.ber_encode(BEREncoding.CONTEXT_SPECIFIC_TAG, 3);

  // Encoding s_diagnostics: SEQUENCE OF OPTIONAL

  if (s_diagnostics != null) {
    f2 = new BEREncoding[s_diagnostics.length];

    for (p = 0; p < s_diagnostics.length; p++) {
      f2[p] = s_diagnostics[p].ber_encode();
    }

    fields[x++] = new BERConstructed(BEREncoding.CONTEXT_SPECIFIC_TAG, 4, f2);
  }

  // Encoding s_taskPackage: EXTERNAL OPTIONAL

  if (s_taskPackage != null) {
    fields[x++] = s_taskPackage.ber_encode(BEREncoding.CONTEXT_SPECIFIC_TAG, 5);
  }

  // Encoding s_otherInfo: OtherInformation OPTIONAL

  if (s_otherInfo != null) {
    fields[x++] = s_otherInfo.ber_encode();
  }

  return new BERConstructed(tag_type, tag, fields);
}

//----------------------------------------------------------------
/**
 * Returns a new String object containing a text representing
 * of the ExtendedServicesResponse. 
 */

public String
toString()
{
  int p;
  StringBuffer str = new StringBuffer("{");
  int outputted = 0;

  if (s_referenceId != null) {
    str.append("referenceId ");
    str.append(s_referenceId);
    outputted++;
  }

  if (0 < outputted)
    str.append(", ");
  str.append("operationStatus ");
  str.append(s_operationStatus);
  outputted++;

  if (s_diagnostics != null) {
    if (0 < outputted)
    str.append(", ");
    str.append("diagnostics ");
    str.append("{");
    for (p = 0; p < s_diagnostics.length; p++) {
      if (p != 0)
        str.append(", ");
      str.append(s_diagnostics[p]);
    }
    str.append("}");
    outputted++;
  }

  if (s_taskPackage != null) {
    if (0 < outputted)
    str.append(", ");
    str.append("taskPackage ");
    str.append(s_taskPackage);
    outputted++;
  }

  if (s_otherInfo != null) {
    if (0 < outputted)
    str.append(", ");
    str.append("otherInfo ");
    str.append(s_otherInfo);
    outputted++;
  }

  str.append("}");

  return str.toString();
}

//----------------------------------------------------------------
/*
 * Internal variables for class.
 */

public ReferenceId s_referenceId; // optional
public ASN1Integer s_operationStatus;
public DiagRec s_diagnostics[]; // optional
public ASN1External s_taskPackage; // optional
public OtherInformation s_otherInfo; // optional

//----------------------------------------------------------------
/*
 * Enumerated constants for class.
 */

// Enumerated constants for operationStatus
public static final int E_done = 1;
public static final int E_accepted = 2;
public static final int E_failure = 3;

} // ExtendedServicesResponse

//----------------------------------------------------------------
//EOF
