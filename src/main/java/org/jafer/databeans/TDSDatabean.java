/**
 * JAFER Toolkit Project.
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

/**
 *  Title: JAFER Toolkit
 *  Description:
 *  Copyright: Copyright (c) 2002
 *
 *@author     Antony Corfield; Matthew Dovey; Colin Tatham
 *@version    1.0
 */

package org.jafer.databeans;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import net.sourceforge.jtds.jdbcx.JtdsDataSource;

import org.jafer.exception.JaferException;
import org.jafer.query.XMLRPNQuery;
import org.w3c.dom.Node;

import z3950.v3.RPNQuery;

public class TDSDatabean extends JDBC {

/** uses settings from superclass: */
//  public final static String CONFIG_FILE = "org/jafer/conf/jdbcConfig/config.xml";
//  public final static String QUERY_XSLT = "org/jafer/conf/jdbcConfig/query.xsl";

  @Override
public int submitQuery(Object query) throws JaferException {

    if (query instanceof Node)
      return submitQuery((Node)query);
    else if (query instanceof RPNQuery)
      return submitQuery((RPNQuery)query);
    else
      throw new JaferException("Only queries of type Node or RPNQuery accepted. (See www.jafer.org)");
  }

  @Override
public int submitQuery(RPNQuery query) throws JaferException {

    return submitQuery(XMLRPNQuery.getXMLQuery(query));
  }

  @Override
public int submitQuery(Node query) throws JaferException {
/** Presumes ResultSet in use is NOT scrollable,
  and cannot be re-used for Present operation. */
/** @todo more specific error message: when connection isn't made... */
    if (dataSource == null)
      configureDataSource();

    this.query = query;
    setQueryString("select count(*) ");

    try {
      ResultSet results = getStatement().executeQuery(getQueryString());
//      logger.log(Level.FINE, "submitQuery(): "+getQueryString());
      System.out.println(getQueryString());
      results.next();
      nResults = results.getInt(1);

      return nResults;
    }
    catch (SQLException ex) {
      throw new JaferException("Error in database connection, or in generation/execution of SQL statement", ex);
    }
  }

  @Override
protected boolean alignCursor() throws SQLException, JaferException {

    while (resultSet.getRow() != getRecordCursor()) {
      if (resultSet.getRow() < getRecordCursor())
	resultSet.next(); // put in cache here?
      else
	search(); //TdsDataSource ResultSet is FORWARD ONLY
    }
    return true;
  }

  @Override
protected void configureDataSource() throws JaferException {

    dataSource = new JtdsDataSource();
    ((JtdsDataSource)dataSource).setServerName(getHost());
    ((JtdsDataSource)dataSource).setPortNumber(getPort());
    ((JtdsDataSource)dataSource).setDatabaseName(getCurrentDatabase());
    ((JtdsDataSource)dataSource).setUser(username);
    ((JtdsDataSource)dataSource).setPassword(password);
  }

  @Override
protected Statement getStatement() throws SQLException {

  return getConnection().createStatement();
  }
}