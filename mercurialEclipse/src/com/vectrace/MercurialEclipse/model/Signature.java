/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.extensions.HgSigsClient;
import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * @author bastian
 *
 */
public class Signature {

	private final HgRoot root;
	private String key;
	private String nodeId;
	private boolean valid;
	private boolean checked;

	public Signature(String key, String nodeId, HgRoot root) {
		this.key = key;
		this.nodeId = nodeId;
		this.root = root;
	}

	public boolean validate() {
		if (checked) {
			return isValid();
		}
		try {
			String result = HgSigsClient.checkSig(root, nodeId);
			if (result != null && !result.contains("No valid signature for")) { //$NON-NLS-1$
				valid = true;
				key = result.split("\n")[1].trim(); //$NON-NLS-1$
			}
			checked = true;
			return valid;
		} catch (HgException e) {
			checked = true;
			valid = false;
			MercurialEclipsePlugin.logError(e);
			return false;
		}
	}

	@Override
	public String toString() {
		return valid + ":" + key + ", " + nodeId; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	/**
	 * @param changeSetId
	 *            the changeSetId to set
	 */
	public void setNodeId(String changeSetId) {
		this.nodeId = changeSetId;
	}

	/**
	 * @return the changeSetId
	 */
	public String getNodeId() {
		return nodeId;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public boolean isValid() {
		return valid;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
		result = prime * result + ((root == null) ? 0 : root.hashCode());
		result = prime * result + (valid ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Signature)) {
			return false;
		}
		Signature other = (Signature) obj;
		if (key == null) {
			if (other.key != null) {
				return false;
			}
		} else if (!key.equals(other.key)) {
			return false;
		}
		if (nodeId == null) {
			if (other.nodeId != null) {
				return false;
			}
		} else if (!nodeId.equals(other.nodeId)) {
			return false;
		}
		if (root == null) {
			if (other.root != null) {
				return false;
			}
		} else if (!root.equals(other.root)) {
			return false;
		}
		if (valid != other.valid) {
			return false;
		}
		return true;
	}

}
