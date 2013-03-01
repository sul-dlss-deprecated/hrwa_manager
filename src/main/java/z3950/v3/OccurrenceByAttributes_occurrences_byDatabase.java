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
 * Generated by Zebulun ASN1tojava: 1998-09-08 03:15:06 UTC
 */

//----------------------------------------------------------------

package z3950.v3;
import asn1.*;


//================================================================
/**
 * Class for representing a <code>OccurrenceByAttributes_occurrences_byDatabase</code> from <code>Z39-50-APDU-1995</code>
 *
 * <pre>
 * OccurrenceByAttributes_occurrences_byDatabase ::=
 * SEQUENCE {
 *   db DatabaseName
 *   num [1] IMPLICIT INTEGER OPTIONAL
 *   otherDbInfo OtherInformation OPTIONAL
 * }
 * </pre>
 *
 * @version	$Release$ $Date$
 */

//----------------------------------------------------------------

public final class OccurrenceByAttributes_occurrences_byDatabase extends ASN1Any
{

  public final static String VERSION = "Copyright (C) Hoylen Sue, 1998. 199809080315Z";

//----------------------------------------------------------------
/**
 * Default constructor for a OccurrenceByAttributes_occurrences_byDatabase.
 */

public
OccurrenceByAttributes_occurrences_byDatabase()
{
}

//----------------------------------------------------------------
/**
 * Constructor for a OccurrenceByAttributes_occurrences_byDatabase from a BER encoding.
 * <p>
 *
 * @param ber the BER encoding.
 * @param check_tag will check tag if true, use false
 *         if the BER has been implicitly tagged. You should
 *         usually be passing true.
 * @exception	ASN1Exception if the BER encoding is bad.
 */

public
OccurrenceByAttributes_occurrences_byDatabase(BEREncoding ber, boolean check_tag)
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
  // OccurrenceByAttributes_occurrences_byDatabase should be encoded by a constructed BER

  BERConstructed ber_cons;
  try {
    ber_cons = (BERConstructed) ber;
  } catch (ClassCastException e) {
    throw new ASN1EncodingException
      ("Zebulun OccurrenceByAttributes_occurrences_byDatabase: bad BER form\n");
  }

  // Prepare to decode the components

  int num_parts = ber_cons.number_components();
  int part = 0;
  BEREncoding p;

  // Decoding: db DatabaseName

  if (num_parts <= part) {
    // End of record, but still more elements to get
    throw new ASN1Exception("Zebulun OccurrenceByAttributes_occurrences_byDatabase: incomplete");
  }
  p = ber_cons.elementAt(part);

  s_db = new DatabaseName(p, true);
  part++;

  // Remaining elements are optional, set variables
  // to null (not present) so can return at end of BER

  s_num = null;
  s_otherDbInfo = null;

  // Decoding: num [1] IMPLICIT INTEGER OPTIONAL

  if (num_parts <= part) {
    return; // no more data, but ok (rest is optional)
  }
  p = ber_cons.elementAt(part);

  if (p.tag_get() == 1 &&
      p.tag_type_get() == BEREncoding.CONTEXT_SPECIFIC_TAG) {
    s_num = new ASN1Integer(p, false);
    part++;
  }

  // Decoding: otherDbInfo OtherInformation OPTIONAL

  if (num_parts <= part) {
    return; // no more data, but ok (rest is optional)
  }
  p = ber_cons.elementAt(part);

  try {
    s_otherDbInfo = new OtherInformation(p, true);
    part++; // yes, consumed
  } catch (ASN1Exception e) {
    s_otherDbInfo = null; // no, not present
  }

  // Should not be any more parts

  if (part < num_parts) {
    throw new ASN1Exception("Zebulun OccurrenceByAttributes_occurrences_byDatabase: bad BER: extra data " + part + "/" + num_parts + " processed");
  }
}

//----------------------------------------------------------------
/**
 * Returns a BER encoding of the OccurrenceByAttributes_occurrences_byDatabase.
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
 * Returns a BER encoding of OccurrenceByAttributes_occurrences_byDatabase, implicitly tagged.
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
  if (s_num != null)
    num_fields++;
  if (s_otherDbInfo != null)
    num_fields++;

  // Encode it

  BEREncoding fields[] = new BEREncoding[num_fields];
  int x = 0;

  // Encoding s_db: DatabaseName 

  fields[x++] = s_db.ber_encode();

  // Encoding s_num: INTEGER OPTIONAL

  if (s_num != null) {
    fields[x++] = s_num.ber_encode(BEREncoding.CONTEXT_SPECIFIC_TAG, 1);
  }

  // Encoding s_otherDbInfo: OtherInformation OPTIONAL

  if (s_otherDbInfo != null) {
    fields[x++] = s_otherDbInfo.ber_encode();
  }

  return new BERConstructed(tag_type, tag, fields);
}

//----------------------------------------------------------------
/**
 * Returns a new String object containing a text representing
 * of the OccurrenceByAttributes_occurrences_byDatabase. 
 */

public String
toString()
{
  StringBuffer str = new StringBuffer("{");
  int outputted = 0;

  str.append("db ");
  str.append(s_db);
  outputted++;

  if (s_num != null) {
    if (0 < outputted)
    str.append(", ");
    str.append("num ");
    str.append(s_num);
    outputted++;
  }

  if (s_otherDbInfo != null) {
    if (0 < outputted)
    str.append(", ");
    str.append("otherDbInfo ");
    str.append(s_otherDbInfo);
    outputted++;
  }

  str.append("}");

  return str.toString();
}

//----------------------------------------------------------------
/*
 * Internal variables for class.
 */

public DatabaseName s_db;
public ASN1Integer s_num; // optional
public OtherInformation s_otherDbInfo; // optional

} // OccurrenceByAttributes_occurrences_byDatabase

//----------------------------------------------------------------
//EOF
