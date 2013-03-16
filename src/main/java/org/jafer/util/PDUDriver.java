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

package org.jafer.util;

import org.jafer.util.ConnectionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Hashtable;

import asn1.ASN1Exception;
import asn1.ASN1Integer;
import asn1.BEREncoding;
import z3950.v3.PDU;
import z3950.v3.CloseReason;

import java.util.TimerTask;
import java.util.Timer;


class KillSocketTask extends TimerTask {
  private static Logger logger = LoggerFactory.getLogger("org.jafer.util");;
  private Socket sock;

  public KillSocketTask(Socket asock) {
    
    this.sock = asock;
  }

  public void run() {
    try {
      logger.trace("Attempting to force close timed out socket");
      this.sock.close();
    }
    catch (Exception ex) {
      logger.trace("Force closed timed out socket failed: {}", ex.getMessage());
    }
  }
}

/**
 * <p>Used by ZClient and ZServer Session for Z39.50 input/output - each session has it's own PDUDriver.
 * Includes methods for get/send PDU and close. I/O errors throw org.jafer.util.ConnectionException -
 * caught and handled by the session object.</p>
 * @author Antony Corfield; Matthew Dovey; Colin Tatham
 * @version 1.0
 */
public class PDUDriver {
  private static long DATA_TIMEOUT = 60000; // Allow 60 seconds for typical data transfer

  private static Logger logger = LoggerFactory.getLogger("org.jafer.util");;
  private Hashtable<Integer, String> closeReason;
  private Socket socket;
  private BufferedInputStream src;
  private BufferedOutputStream dest;
  private String sessionName;

  /**
   * @todo: how do we handle preferredMessageSize and exceptionalRecordSize?
   */
  /**
   * @todo: read closeReason from config
   */
  public PDUDriver(String sessionName, Socket socket, int timeout) throws IOException {

    this.sessionName = sessionName;
    this.socket = socket;
    closeReason = loadCloseReason(new Hashtable<Integer, String>());

    try {
      this.socket.setSoTimeout(timeout);
      src = new BufferedInputStream(socket.getInputStream());
      dest = new BufferedOutputStream(socket.getOutputStream());
      } catch(java.net.SocketException e) {
        logger.error("{} could not reset timeout on socket", sessionName, e);
        throw e;
      } catch (IOException e) {
        try {
          socket.close();
          } catch (IOException ex) {}
          socket = null;
          throw e;
      }
  }

  public BEREncoding getBEREncoding() throws ConnectionException {
    Timer timer = null;
    try {
      long timeout = this.socket.getSoTimeout();
      if (timeout > 0) {
        timer = new Timer();
        timer.schedule(new KillSocketTask(this.socket), (long) (timeout + DATA_TIMEOUT));
      }
      BEREncoding berEncoding = BEREncoding.input(src);
      if (timer != null) {
        timer.cancel();
      }
      return berEncoding;
    }
    catch (java.net.SocketException ex) {
      if (timer != null) {
        timer.cancel();
      }
      throw new ConnectionException("socket closed", ex);
    }
    catch (java.net.SocketTimeoutException ex) {
      if (timer != null) {
        timer.cancel();
      }
      initClose(7);
      throw new ConnectionException("association timed out", ex);
    }
    catch (java.io.InterruptedIOException ex) {
      if (timer != null) {
        timer.cancel();
      }
      initClose(7);
      throw new ConnectionException("association interrupted", ex);
    }
    catch (java.io.IOException ex) {
      if (timer != null) {
        timer.cancel();
      }
      initClose(6);
      throw new ConnectionException(ex.toString(), ex);
    }
    catch (ASN1Exception ex) {
      if (timer != null) {
        timer.cancel();
      }
      initClose(2);
      throw new ConnectionException(ex.toString(), ex);
    }
  }

  private String dumpPDU(PDU pdu) {
    return pdu.toString();
  }

  public PDU getPDU() throws ConnectionException {

    try {
      BEREncoding ber = getBEREncoding();
      PDU pduRequest = new PDU(ber, true);
      if (logger.isTraceEnabled()) logger.trace("{} incoming PDU: {}", sessionName, dumpPDU(pduRequest));
      return pduRequest;
    } catch (asn1.ASN1Exception ex) {
      throw new ConnectionException("ASN1 data error", ex);
    } catch (NullPointerException ex) {
      throw new ConnectionException("connection dropped by client", ex);
    }
  }

  public synchronized void sendPDU(PDU pduResponse) throws ConnectionException {

    Timer timer = null;
    try {
        if (logger.isTraceEnabled()) logger.trace("{} outcoming PDU: {}", sessionName, dumpPDU(pduResponse));
      long timeout = this.socket.getSoTimeout();
      if (timeout > 0) {
        timer = new Timer();
        timer.schedule(new KillSocketTask(this.socket), (long)(timeout + DATA_TIMEOUT));
      }
      pduResponse.ber_encode().output(dest);
      dest.flush();
      if (timer != null) {
        timer.cancel();
      }
    } catch (java.net.SocketException ex) {
      if (timer != null) {
        timer.cancel();
      }
      throw new ConnectionException("socket closed", ex);
    } catch (java.net.SocketTimeoutException ex) {
      if (timer != null) {
        timer.cancel();
      }
      throw new ConnectionException("association timed out", ex);
    } catch (java.io.InterruptedIOException ex) {
      if (timer != null) {
        timer.cancel();
      }
      throw new ConnectionException("association interrupted", ex);
    } catch (java.io.IOException ex) {
      if (timer != null) {
        timer.cancel();
      }
      throw new ConnectionException(ex.toString(), ex);
    } catch (asn1.ASN1Exception ex) {
      if (timer != null) {
        timer.cancel();
      }
      throw new ConnectionException(ex.toString(), ex);
    }
  }

   boolean reentrant = false;
   public void initClose(int reason) throws ConnectionException {
     if (reentrant) {
       return;
     }
     reentrant = true;

     String closeReason = getCloseReason(reason);
     logger.debug("{} requesting close due to {}", sessionName, closeReason);

     PDU pduResponse = new PDU();
     pduResponse.c_close = new z3950.v3.Close();
     pduResponse.c_close.s_closeReason = new CloseReason();
     pduResponse.c_close.s_closeReason.value = new ASN1Integer(reason);
     pduResponse.c_close.s_referenceId = null;
     sendPDU(pduResponse);
     waitClosePDU();
     reentrant = false;
   }

  public void respClose(PDU pduRequest) throws ConnectionException {

    int k = 9;
    if (pduRequest.c_close.s_closeReason.value != null)
      k = pduRequest.c_close.s_closeReason.value.get();
    logger.debug("{} close requested due to ", sessionName, getCloseReason(k));

    PDU pduResponse = new PDU();
    pduResponse.c_close = new z3950.v3.Close();
    pduResponse.c_close.s_closeReason = new CloseReason();
    pduResponse.c_close.s_closeReason.value = new ASN1Integer(pduResponse.c_close.s_closeReason.E_peerAbort);
    pduResponse.c_close.s_referenceId = pduRequest.c_close.s_referenceId;
    sendPDU(pduResponse);
  }

  private void waitClosePDU() {

    PDU pdu = null;

    logger.debug("{} waiting for close response", sessionName);
    try {
      for (int n = 0; n < 10; n++) {
        pdu = waitForPDU();
        if (pdu != null && pdu.c_close != null) {
            logger.debug("{} got close response", sessionName);
            return;
        }
      }
      logger.debug("{} close PDU not received", sessionName);
    } catch (Exception ex) {
      logger.warn("{} communications error waiting for Close response", sessionName, ex);
    }
  }

  private PDU waitForPDU() throws ConnectionException {

    BEREncoding ber = getBEREncoding();
    if (ber == null)
      return null;

    PDU pdu = null;
    try {
      pdu = new PDU(ber, true);
    } catch (ASN1Exception ex) {
      return null;
    }
    return pdu;
  }

  private String getCloseReason(int k) {

    Integer key = Integer.valueOf(k);
    if (closeReason.containsKey(key))
      return (String)closeReason.get(key);
    return (String)closeReason.get(Integer.valueOf(9));
  }

  private Hashtable<Integer, String> loadCloseReason(Hashtable<Integer, String> closeReason) {
    /** @todo Read from config */
    closeReason.put(Integer.valueOf(0), "Finished");
    closeReason.put(Integer.valueOf(1), "Shutdown");
    closeReason.put(Integer.valueOf(2), "System Problem");
    closeReason.put(Integer.valueOf(3), "Cost Limit");
    closeReason.put(Integer.valueOf(4), "Resources");
    closeReason.put(Integer.valueOf(5), "Security violation");
    closeReason.put(Integer.valueOf(6), "Protocol error");
    closeReason.put(Integer.valueOf(7), "Lack of activity");
    closeReason.put(Integer.valueOf(8), "Peer abort");
    closeReason.put(Integer.valueOf(9), "unknown reason");
    return closeReason;
  }
}
