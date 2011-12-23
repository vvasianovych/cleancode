/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bastian					- implementation
 *     Andrei Loskutov			- bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.MercurialRootCache;
import com.vectrace.MercurialEclipse.utils.IniFile;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;
import com.vectrace.MercurialEclipse.utils.StringUtils;

/**
 * Hg root represents the root of hg repository as <b>canonical path</b>
 * (see {@link File#getCanonicalPath()})
 *
 * @author bastian
 */
public class HgRoot extends HgPath implements IHgRepositoryLocation {

	private static final String PATHS_SECTION = "paths";

	private static final String HG_HGRC = ".hg/hgrc";

	private static final long serialVersionUID = 3L;

	/** Place holder for a (not valid) hg root object */
	public static final HgRoot NO_ROOT;
	static {
		HgRoot root = null;
		try {
			root = new HgRoot("");
		} catch (IOException e) {
			MercurialEclipsePlugin.logError(e);
		} finally {
			NO_ROOT = root;
		}
	}

	/**
	 * Preferred encoding
	 */
	private String encoding;

	/**
	 * Cached encoding fall back encoding as specified in the config file
	 */
	private transient Charset fallbackencoding;

	/**
	 * Cached config file (.hg/hgrc)
	 */
	private transient File config;

	/**
	 * Cached user name or empty string if not specified in the config file
	 */
	private transient String user;

	private final IProject projectAdapter;

	public HgRoot(String pathname) throws IOException {
		this(new File(pathname));
	}

	public HgRoot(File file) throws IOException {
		super(file);
		Object adapter = super.getAdapter(IProject.class);
		if(adapter instanceof IProject) {
			projectAdapter = (IProject) adapter;
		} else {
			projectAdapter = new HgRootContainer(this);
		}
	}

	public void setEncoding(String charset) {
		this.encoding = charset;
	}

	/**
	 * @return never null, root specific encoding (may differ from the OS default encoding)
	 */
	public String getEncoding() {
		if(encoding == null){
			setEncoding(MercurialEclipsePlugin.getDefaultEncoding());
		}
		return encoding;
	}

	/**
	 * Gets the resource hgrc as a {@link java.io.File}.
	 *
	 * @return the {@link java.io.File} referencing the hgrc file, <code>null</code> if it doesn't exist.
	 */
	protected File getConfig() {
		if (config == null) {
			File hgrc = new File(this, HG_HGRC);
			if (hgrc.isFile()) {
				config = hgrc;
				return hgrc;
			}
		}
		return config;
	}

	protected String getConfigItem(String section, String key) {
		getConfig();
		if (config != null) {
			try {
				IniFile iniFile = new IniFile(config.getAbsolutePath());
				return iniFile.getKeyValue(section, key);
			} catch (FileNotFoundException e) {
			}
		}
		return null;
	}

	/**
	 * Get the entries from the paths section of the config file if available
	 * @return A map from logical names to paths
	 * @throws FileNotFoundException If the config file doesn't exist
	 */
	public Map<String, String> getPaths() throws FileNotFoundException {
		File hgrc = getConfig();
		Map<String, String> paths = new HashMap<String, String>();
		if (hgrc == null) {
			return paths;
		}

		IniFile ini = new IniFile(hgrc.getAbsolutePath());
		Map<String, String> section = ini.getSection(PATHS_SECTION);
		if (section != null) {
			for (Entry<String, String> entry : section.entrySet()) {
				String logicalName = entry.getKey();
				String path = entry.getValue();
				if(!StringUtils.isEmpty(logicalName) && !StringUtils.isEmpty(path)) {
					paths.put(logicalName, path);
				}
			}
		}

		return paths;
	}

	/**
	 * @return The fallback encoding as specified in the config file, otherwise windows-1251
	 */
	public Charset getFallbackencoding() {
		if(fallbackencoding == null){
			// set fallbackencoding to windows standard codepage
			String fallback = getConfigItem("ui", "fallbackencoding");
			if (fallback == null || fallback.length() == 0) {
				fallback = "windows-1251";
			}
			fallbackencoding = Charset.forName(fallback);
		}
		return fallbackencoding;
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	public int compareTo(IHgRepositoryLocation loc) {
		if(getLocation() == null) {
			return -1;
		}
		if(loc.getLocation() == null){
			return 1;
		}
		return getLocation().compareToIgnoreCase(loc.getLocation());
	}

	public String getLocation() {
		return getAbsolutePath();
	}

	public URI getUri() throws HgException {
		return toURI();
	}

	public String getLogicalName() {
		return null;
	}

	public String getPassword() {
		return null;
	}

	public String getUser() {
		if(user == null){
			String configItem = getConfigItem("ui", "username");
			if(StringUtils.isEmpty(configItem)){
				// set to empty string to avoid multiple reads from file
				user = "";
			} else {
				user = configItem.trim();
			}
		}
		return StringUtils.isEmpty(user)? null : user;
	}

	@Override
	public Object[] getChildren(Object o) {
		IProject[] projects = MercurialTeamProvider.getKnownHgProjects(this).toArray(
				new IProject[0]);
		if (projects.length == 1) {
			if (getIPath().equals(ResourceUtils.getPath(projects[0]))) {
				return projects;
			}
		}
		try {
			return getResource().members();
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
			return new Object[0];
		}
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return MercurialEclipsePlugin.getImageDescriptor("root.gif");
	}

	@Override
	public Object getAdapter(Class adapter) {
		if(adapter == IHgRepositoryLocation.class){
			return this;
		}
		Object object = super.getAdapter(adapter);
		if (object == null && (adapter == IProject.class || adapter == IResource.class)) {
			return getResource();
		}
		return object;
	}

	public boolean isLocal() {
		return true;
	}

	public IProject getResource() {
		if(projectAdapter instanceof HgRootContainer) {
			HgRootContainer container = (HgRootContainer) projectAdapter;
			container.init();
		} else {
			try {
				projectAdapter.open(IResource.BACKGROUND_REFRESH, null);
				if(!MercurialRootCache.isHgTeamProviderFor(projectAdapter)) {
					try {
						MercurialRootCache.markAsCached(projectAdapter, this);
					} catch (CoreException e) {
						MercurialEclipsePlugin.logError(e);
					}
				}
			} catch (CoreException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}
		return projectAdapter;
	}

	public HgRoot toHgRoot() {
		return this;
	}

}
