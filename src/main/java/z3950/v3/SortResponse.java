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
 * Generated by Zebulun ASN1tojava: 1998-09-08 03:15:11 UTC
 */

//----------------------------------------------------------------

package z3950.v3;
import asn1.*;

//================================================================
/**
 * Class for representing a <code>SortResponse</code> from <code>Z39-50-APDU-1995</code>
 *
 * <pre>
 * SortResponse ::=
 * SEQUENCE {
 *   referenceId ReferenceId OPTIONAL
 *   sortStatus [3] IMPLICIT INTEGER
 *   resultSetStatus [4] IMPLICIT INTEGER OPTIONAL
 *   diagnostics [5] IMPLICIT SEQUENCE OF DiagRec OPTIONAL
 *   otherInfo OtherInformation OPTIONAL
 * }
 * </pre>
 *
 * @version	$Release$ $Date$
 */

//----------------------------------------------------------------

public final class SortResponse extends ASN1Any
{

  public final static String VERSION = "Copyright (C) Hoylen Sue, 1998. 199809080315Z";

//----------------------------------------------------------------
/**
 * Default constructor for a SortResponse.
 */

public
SortResponse()
{
}

//----------------------------------------------------------------
/**
 * Constructor for a SortResponse from a BER encoding.
 * <p>
 *
 * @param ber the BER encoding.
 * @param check_tag will check tag if true, use false
 *         if the BER has been implicitly tagged. You should
 *         usually be passing true.
 * @exception	ASN1Exception if the BER encoding is bad.
 */

public
SortResponse(BEREncoding ber, boolean check_tag)
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
  // SortResponse should be encoded by a constructed BER

  BERConstructed ber_cons;
  try {
    ber_cons = (BERConstructed) ber;
  } catch (ClassCastException e) {
    throw new ASN1EncodingException
      ("Zebulun SortResponse: bad BER form\n");
  }

  // Prepare to decode the components

  int num_parts = ber_cons.number_components();
  int part = 0;
  BEREncoding p;

  // Decoding: referenceId ReferenceId OPTIONAL

  if (num_parts <= part) {
    // End of record, but still more elements to get
    throw new ASN1Exception("Zebulun SortResponse: incomplete");
  }
  p = ber_cons.elementAt(part);

  try {
    s_referenceId = new ReferenceId(p, true);
    part++; // yes, consumed
  } catch (ASN1Exception e) {
    s_referenceId = null; // no, not present
  }

  // Decoding: sortStatus [3] IMPLICIT INTEGER

  if (num_parts <= part) {
    // End of record, but still more elements to get
    throw new ASN1Exception("Zebulun SortResponse: incomplete");
  }
  p = ber_cons.elementAt(part);

  if (p.tag_get() != 3 ||
      p.tag_type_get() != BEREncoding.CONTEXT_SPECIFIC_TAG)
    throw new ASN1EncodingException
      ("Zebulun SortResponse: bad tag in s_sortStatus\n");

  s_sortStatus = new ASN1Integer(p, false);
  part++;

  // Remaining elements are optional, set variables
  // to null (not present) so can return at end of BER

  s_resultSetStatus = null;
  s_diagnostics = null;
  s_otherInfo = null;

  // Decoding: resultSetStatus [4] IMPLICIT INTEGER OPTIONAL

  if (num_parts <= part) {
    return; // no more data, but ok (rest is optional)
  }
  p = ber_cons.elementAt(part);

  if (p.tag_get() == 4 &&
      p.tag_type_get() == BEREncoding.CONTEXT_SPECIFIC_TAG) {
    s_resultSetStatus = new ASN1Integer(p, false);
    part++;
  }

  // Decoding: diagnostics [5] IMPLICIT SEQUENCE OF DiagRec OPTIONAL

  if (num_parts <= part) {
    return; // no more data, but ok (rest is optional)
  }
  p = ber_cons.elementAt(part);

  if (p.tag_get() == 5 &&
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
    throw new ASN1Exception("Zebulun SortResponse: bad BER: extra data " + part + "/" + num_parts + " processed");
  }
}

//----------------------------------------------------------------
/**
 * Returns a BER encoding of the SortResponse.
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
 * Returns a BER encoding of SortResponse, implicitly tagged.
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
  if (s_resultSetStatus != null)
    num_fields++;
  if (s_diagnostics != null)
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

  // Encoding s_sortStatus: INTEGER 

  fields[x++] = s_sortStatus.ber_encode(BEREncoding.CONTEXT_SPECIFIC_TAG, 3);

  // Encoding s_resultSetStatus: INTEGER OPTIONAL

  if (s_resultSetStatus != null) {
    fields[x++] = s_resultSetStatus.ber_encode(BEREncoding.CONTEXT_SPECIFIC_TAG, 4);
  }

  // Encoding s_diagnostics: SEQUENCE OF OPTIONAL

  if (s_diagnostics != null) {
    f2 = new BEREncoding[s_diagnostics.length];

    for (p = 0; p < s_diagnostics.length; p++) {
      f2[p] = s_diagnostics[p].ber_encode();
    }

    fields[x++] = new BERConstructed(BEREncoding.CONTEXT_SPECIFIC_TAG, 5, f2);
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
 * of the SortResponse. 
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
  str.append("sortStatus ");
  str.append(s_sortStatus);
  outputted++;

  if (s_resultSetStatus != null) {
    if (0 < outputted)
    str.append(", ");
    str.append("resultSetStatus ");
    str.append(s_resultSetStatus);
    outputted++;
  }

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
public ASN1Integer s_sortStatus;
public ASN1Integer s_resultSetStatus; // optional
public DiagRec s_diagnostics[]; // optional
public OtherInformation s_otherInfo; // optional

//----------------------------------------------------------------
/*
 * Enumerated constants for class.
 */

// Enumerated constants for sortStatus
public static final int E_success = 0;
public static final int E_partial_1 = 1;
public static final int E_failure = 2;

// Enumerated constants for resultSetStatus
public static final int E_empty = 1;
public static final int E_interim = 2;
public static final int E_unchanged = 3;
public static final int E_none = 4;

} // SortResponse

//----------------------------------------------------------------
//EOF
