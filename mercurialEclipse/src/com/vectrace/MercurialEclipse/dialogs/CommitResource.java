/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     StefanC - implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import java.io.File;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

public class CommitResource {
	private final String statusMessage;
	private final int status;
	private final IResource resource;
	private final File path;

	public CommitResource(int statusBitField, IResource resource, File path) {
		this.status = statusBitField;
		this.statusMessage = convertStatus(status);
		this.resource = resource;
		this.path = path;
	}

	private String convertStatus(int bits) {
		if ((bits & MercurialStatusCache.BIT_MODIFIED) != 0) {
			return CommitDialog.FILE_MODIFIED;
		} else if ((bits & MercurialStatusCache.BIT_ADDED) != 0) {
			return CommitDialog.FILE_ADDED;
		} else if ((bits & MercurialStatusCache.BIT_REMOVED) != 0) {
			return CommitDialog.FILE_REMOVED;
		} else if ((bits & MercurialStatusCache.BIT_UNKNOWN) != 0) {
			return CommitDialog.FILE_UNTRACKED;
		} else if ((bits & MercurialStatusCache.BIT_MISSING) != 0) {
			return CommitDialog.FILE_DELETED;
		} else if ((bits & MercurialStatusCache.BIT_CLEAN) != 0) {
			return CommitDialog.FILE_CLEAN;
		} else {
			return "status error: " + bits; //$NON-NLS-1$
		}
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public IResource getResource() {
		return resource;
	}

	public File getPath() {
		return path;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CommitResource [status=");
		builder.append(statusMessage);
		builder.append(", path=");
		builder.append(path);
		builder.append(", resource=");
		builder.append(resource);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((resource == null) ? 0 : resource.hashCode());
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
		if (!(obj instanceof CommitResource)) {
			return false;
		}
		CommitResource other = (CommitResource) obj;
		if (resource == null) {
			if (other.resource != null) {
				return false;
			}
		} else if (!resource.equals(other.resource)) {
			return false;
		}
		return true;
	}

	public boolean isUnknown() {
		return (status & MercurialStatusCache.BIT_UNKNOWN) != 0;
	}

	public boolean isMissing() {
		return (status & MercurialStatusCache.BIT_MISSING) != 0;
	}
}