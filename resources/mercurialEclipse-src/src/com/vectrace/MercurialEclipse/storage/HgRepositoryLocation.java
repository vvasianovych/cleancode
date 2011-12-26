/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - implementation
 *     Stefan C                  - Code cleanup
 *     Bastian Doetsch           - additions for repository view
 *     Subclipse contributors    - fromProperties() initial source
 *     Adam Berkes (Intland)     - bug fixes
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.storage;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;

/**
 * A class abstracting a Mercurial repository location which may be either local
 * or remote.
 */
public class HgRepositoryLocation implements  Comparable<IHgRepositoryLocation>, IHgRepositoryLocation {

	private static final String PASSWORD_MASK = "***";

	private String logicalName;
	protected String location;
	private String user;
	private String password;

	private boolean isLocal;

	/**
	 * hg repository which is represented by a bundle file (on local disk)
	 */
	public static class BundleRepository extends HgRepositoryLocation {

		/**
		 * @param location canonical representation of a bundle file path, never null
		 */
		public BundleRepository(File location) {
			super(null, null, null);
			this.location = location.getAbsolutePath();
		}

		@Override
		protected URI getUri(boolean isSafe) throws HgException {
			return null;
		}

		@Override
		public boolean isLocal() {
			return true;
		}
	}

	private HgRepositoryLocation(String logicalName, String user, String password){
		super();
		this.logicalName = logicalName;
		this.user = user;
		this.password = password;
	}

	HgRepositoryLocation(String logicalName, String location, String user, String password) throws HgException {
		this(logicalName, user, password);
		URI uri = HgRepositoryLocationParser.parseLocationToURI(location, user, password);
		if(uri != null) {
			isLocal = uri.getScheme() == null || uri.getScheme().equalsIgnoreCase("file");
			try {
				this.location = new URI(uri.getScheme(),
						null,
						uri.getHost(),
						uri.getPort(),
						uri.getPath(),
						null,
						uri.getFragment()).toASCIIString();
			} catch (URISyntaxException ex) {
				MercurialEclipsePlugin.logError(ex);
			}
		} else {
			this.location = location;
			isLocal = true;
		}
	}

	HgRepositoryLocation(String logicalName, URI uri) throws HgException {
		this(logicalName, HgRepositoryLocationParser.getUserNameFromURI(uri),
				HgRepositoryLocationParser.getPasswordFromURI(uri));
		if (uri == null) {
			throw new HgException("Given URI cannot be null");
		}
		isLocal = uri.getScheme() == null || uri.getScheme().equalsIgnoreCase("file");
		try {
			this.location = new URI(uri.getScheme(),
					null,
					uri.getHost(),
					uri.getPort(),
					uri.getPath(),
					null,
					uri.getFragment()).toASCIIString();
		} catch (URISyntaxException ex) {
			MercurialEclipsePlugin.logError(ex);
		}
	}

	public static boolean validateLocation(String validate) {
		try {
			IHgRepositoryLocation location2 = HgRepositoryLocationParser.parseLocation(validate, null, null);
			if(location2 == null){
				return false;
			}
			return location2.getUri() != null
					|| (location2.getLocation() != null && new File(location2.getLocation()).exists());
		} catch (HgException ex) {
			MercurialEclipsePlugin.logError(ex);
			return false;
		}
	}

	public int compareTo(File loc) {
		if(getLocation() == null) {
			return -1;
		}
		if(loc == null){
			return 1;
		}
		return getLocation().compareTo(loc.getAbsolutePath());
	}

	public int compareTo(IHgRepositoryLocation loc) {
		if(getLocation() == null) {
			return -1;
		}
		if(loc.getLocation() == null){
			return 1;
		}
		return getLocation().compareTo(loc.getLocation());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((location == null) ? 0 : location.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof IHgRepositoryLocation)) {
			return false;
		}
		final IHgRepositoryLocation other = (IHgRepositoryLocation) obj;
		if (location == null) {
			if (other.getLocation() != null) {
				return false;
			}
		} else if (!location.equals(other.getLocation())) {
			return false;
		}
		return true;
	}

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}

	/**
	 * Return unsafe (with password) URI for repository location if possible
	 * @return a valid URI of the repository or null if repository is local directory
	 * @throws HgException unable to parse to URI or location is invalid.
	 */
	public URI getUri() throws HgException {
		return getUri(false);
	}

	/**
	 * Return URI for repository location if possible
	 * @param isSafe add password to userinfo if false or add a mask instead
	 * @return a valid URI of the repository or null if repository is local directory
	 * @throws HgException unable to parse to URI or location is invalid.
	 */
	protected URI getUri(boolean isSafe) throws HgException {
		return HgRepositoryLocationParser.parseLocationToURI(getLocation(), getUser(),
				isSafe ? PASSWORD_MASK : getPassword());
	}

	@Override
	public String toString() {
		return location;
	}

	public Object[] getChildren(Object o) {
		HgRoot hgRoot = toHgRoot();
		if(hgRoot != null){
			// local repo with one single root
			return new Object[]{ hgRoot };
		}
		// remote repo with possible multiple roots on local file system
		return MercurialEclipsePlugin.getRepoManager().getAllRepoLocationRoots(this).toArray(
				new IHgRepositoryLocation[0]);
	}

	public HgRoot toHgRoot() {
		if(isLocal()){
			try {
				return new HgRoot(getLocation());
			} catch (IOException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}
		return null;
	}

	public String getLocation() {
		return location;
	}

	public String getLogicalName() {
		return logicalName;
	}

	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class adapter) {
		if(adapter == IHgRepositoryLocation.class){
			return this;
		}
		if (adapter == IWorkbenchAdapter.class) {
			return this;
		}
		return null;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public void setLogicalName(String logicalName) {
		this.logicalName = logicalName;
	}

	public String getLabel(Object o) {
		return o.toString();
	}

	public Object getParent(Object o) {
		return null;
	}

	public ImageDescriptor getImageDescriptor(Object object) {
		return MercurialEclipsePlugin.getImageDescriptor("cview16/repository_rep.gif");
	}

	public boolean isLocal() {
		return isLocal;
	}
}
