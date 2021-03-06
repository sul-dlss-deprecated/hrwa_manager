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
 * Generated by Zebulun ASN1tojava: 1998-09-08 03:15:24 UTC
 */

//----------------------------------------------------------------

package z3950.AccessCtrl_prompt;
import asn1.*;
import z3950.v3.DiagRec;
import z3950.v3.InternationalString;

//================================================================
/**
 * Class for representing a <code>PromptObject</code> from <code>AccessControlFormat-prompt-1</code>
 *
 * <pre>
 * PromptObject ::=
 * CHOICE {
 *   challenge [1] IMPLICIT Challenge
 *   response [2] IMPLICIT Response
 * }
 * </pre>
 *
 * @version	$Release$ $Date$
 */

//----------------------------------------------------------------

public final class PromptObject extends ASN1Any
{

  public final static String VERSION = "Copyright (C) Hoylen Sue, 1998. 199809080315Z";

//----------------------------------------------------------------
/**
 * Default constructor for a PromptObject.
 */

public
PromptObject()
{
}

//----------------------------------------------------------------
/**
 * Constructor for a PromptObject from a BER encoding.
 * <p>
 *
 * @param ber the BER encoding.
 * @param check_tag will check tag if true, use false
 *         if the BER has been implicitly tagged. You should
 *         usually be passing true.
 * @exception	ASN1Exception if the BER encoding is bad.
 */

public
PromptObject(BEREncoding ber, boolean check_tag)
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

  c_challenge = null;
  c_response = null;

  // Try choice challenge
  if (ber.tag_get() == 1 &&
      ber.tag_type_get() == BEREncoding.CONTEXT_SPECIFIC_TAG) {
    c_challenge = new Challenge(ber, false);
    return;
  }

  // Try choice response
  if (ber.tag_get() == 2 &&
      ber.tag_type_get() == BEREncoding.CONTEXT_SPECIFIC_TAG) {
    c_response = new Response(ber, false);
    return;
  }

  throw new ASN1Exception("Zebulun PromptObject: bad BER encoding: choice not matched");
}

//----------------------------------------------------------------
/**
 * Returns a BER encoding of PromptObject.
 *
 * @return	The BER encoding.
 * @exception	ASN1Exception Invalid or cannot be encoded.
 */

public BEREncoding
ber_encode()
       throws ASN1Exception
{
  BEREncoding chosen = null;

  // Encoding choice: c_challenge
  if (c_challenge != null) {
    chosen = c_challenge.ber_encode(BEREncoding.CONTEXT_SPECIFIC_TAG, 1);
  }

  // Encoding choice: c_response
  if (c_response != null) {
    if (chosen != null)
      throw new ASN1Exception("CHOICE multiply set");
    chosen = c_response.ber_encode(BEREncoding.CONTEXT_SPECIFIC_TAG, 2);
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

  throw new ASN1EncodingException("Zebulun PromptObject: cannot implicitly tag");
}

//----------------------------------------------------------------
/**
 * Returns a new String object containing a text representing
 * of the PromptObject. 
 */

public String
toString()
{
  StringBuffer str = new StringBuffer("{");

  boolean found = false;

  if (c_challenge != null) {
    if (found)
      str.append("<ERROR: multiple CHOICE: challenge> ");
    found = true;
    str.append("challenge ");
  str.append(c_challenge);
  }

  if (c_response != null) {
    if (found)
      str.append("<ERROR: multiple CHOICE: response> ");
    found = true;
    str.append("response ");
  str.append(c_response);
  }

  str.append("}");

  return str.toString();
}

//----------------------------------------------------------------
/*
 * Internal variables for class.
 */

public Challenge c_challenge;
public Response c_response;

} // PromptObject

//----------------------------------------------------------------
//EOF
