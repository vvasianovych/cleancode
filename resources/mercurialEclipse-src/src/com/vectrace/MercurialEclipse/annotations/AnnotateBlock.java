/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Charles O'Farrell - implementation
 *******************************************************************************/

package com.vectrace.MercurialEclipse.annotations;
import java.util.Date;

import com.vectrace.MercurialEclipse.HgRevision;

public class AnnotateBlock {

private HgRevision revision = new HgRevision("", -1); //$NON-NLS-1$
private String user = ""; //$NON-NLS-1$
private final int startLine;
private int endLine;
private Date date;

public AnnotateBlock(HgRevision revision, String user, Date date, int startLine, int endLine) {
	this.revision = revision;
	this.user = user;
	this.date = date;
	this.startLine = startLine;
	this.endLine = endLine;
}


/**
   * @return int the last source line of the receiver
   */
public int getEndLine() {
	return endLine;
}

/**
   * @param line
   */
public void setEndLine(int line) {
	endLine = line;
}

/**
   * @return the revision the receiver occured in.
   */
public HgRevision getRevision() {
	return revision;
}

/**
   * @return the first source line number of the receiver
   */
public int getStartLine() {
	return startLine;
}

/**
   * Answer true if the receiver contains the given line number, false otherwise.
   * @param i a line number
   * @return true if receiver contains a line number.
   */
public boolean contains(int i) {
	return i >= startLine && i <= endLine;
}

/**
   * @return Returns the date.
   */
public Date getDate() {
	return this.date;
}

/**
   * @param date The date to set.
   */
public void setDate(Date date) {
	this.date = date;
}

/**
   * @return Returns the user.
   */
public String getUser() {
	return this.user;
}

/**
   * @param user The user to set.
   */
public void setUser(String user) {
	this.user = user;
}
}
