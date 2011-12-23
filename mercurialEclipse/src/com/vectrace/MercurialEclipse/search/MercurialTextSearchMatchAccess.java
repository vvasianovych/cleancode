/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian	   - implementation
 * Philip Graf - Fixed bugs which FindBugs found
 *******************************************************************************/
package com.vectrace.MercurialEclipse.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.search.core.text.TextSearchMatchAccess;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialRevisionStorage;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author Bastian
 *
 */
public class MercurialTextSearchMatchAccess extends TextSearchMatchAccess {
	private final HgRoot root;
	private int rev;
	private int lineNumber;
	private String user;
	private String date;
	private IFile file;
	private boolean becomesMatch = true;
	private final String extract;
	private String fileContent;
	private MercurialRevisionStorage mercurialRevisionStorage;

	/**
	 * Expects a line like filename:rev:linenumber:-|+:username:date
	 *
	 * @param line
	 * @throws HgException
	 */
	public MercurialTextSearchMatchAccess(HgRoot root, String line, boolean all) throws HgException {
		this.root = root;
		try {
			String[] split = line.trim().split(":");
			int i=0;
			Path path = new Path(root.getAbsolutePath() + File.separator + split[i++]);
			this.file = ResourceUtils.getFileHandle(path);
			this.rev = Integer.parseInt(split[i++]);
			this.lineNumber = Integer.parseInt(split[i++]);
			if (all) {
				this.becomesMatch = !"-".equals(split[i++]);
			}
			this.user = split[i++];
			this.extract = split[i++];
		} catch (Exception e) {
			// result is not correctly formed or the line is not a search result entry
			throw new HgException("Failed to parse search result", e);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (becomesMatch ? 1231 : 1237);
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		result = prime * result + lineNumber;
		result = prime * result + rev;
		result = prime * result + ((user == null) ? 0 : user.hashCode());
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
		if (getClass() != obj.getClass()) {
			return false;
		}
		MercurialTextSearchMatchAccess other = (MercurialTextSearchMatchAccess) obj;
		if (becomesMatch != other.becomesMatch) {
			return false;
		}
		if (date == null) {
			if (other.date != null) {
				return false;
			}
		} else if (!date.equals(other.date)) {
			return false;
		}
		if (file == null) {
			if (other.file != null) {
				return false;
			}
		} else if (!file.getFullPath().equals(other.file.getFullPath())) {
			return false;
		}
		if (lineNumber != other.lineNumber) {
			return false;
		}
		if (rev != other.rev) {
			return false;
		}
		if (user == null) {
			if (other.user != null) {
				return false;
			}
		} else if (!user.equals(other.user)) {
			return false;
		}
		return true;
	}

	public int getRev() {
		return rev;
	}

	public void setRev(int rev) {
		this.rev = rev;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	@Override
	public IFile getFile() {
		return file;
	}

	public void setFile(IFile file) {
		this.file = file;
	}

	public boolean isBecomesMatch() {
		return becomesMatch;
	}

	public void setBecomesMatch(boolean becomesMatch) {
		this.becomesMatch = becomesMatch;
	}

	@Override
	public String getFileContent(int offset, int length) {
		String sub = getFileContent();
		if (sub.length() > 0) {
			return sub.substring(offset, offset + length);
		}
		return "";
	}

	/**
	 * @return
	 *
	 */
	private String getFileContent() {
		if (fileContent == null) {
			getMercurialRevisionStorage();
			BufferedReader reader = null;
			try {
				InputStream is = mercurialRevisionStorage.getContents();

				if (is != null) {
					StringBuilder sb = new StringBuilder();
					String line;

					reader = new BufferedReader(
							new InputStreamReader(is, root.getEncoding()));
					while ((line = reader.readLine()) != null) {
						sb.append(line).append("\n");
					}
					this.fileContent = sb.toString();
				}
			} catch (IOException e) {
				MercurialEclipsePlugin.logError(e);
			} catch (CoreException e) {
				MercurialEclipsePlugin.logError(e);
			} finally {
				try {
					if (reader != null) {
						reader.close();
					}
				} catch (IOException e) {
				}
			}
		}
		if (fileContent == null) {
			fileContent = "";
		}
		return fileContent;
	}

	@Override
	public char getFileContentChar(int offset) {
		String sub = getFileContent(offset, 1);
		if (sub != null && sub.length() > 0) {
			return sub.charAt(0);
		}
		return ' ';
	}

	@Override
	public int getFileContentLength() {
		return getFileContent().length();
	}

	@Override
	public int getMatchLength() {
		return extract.length();
	}

	@Override
	public int getMatchOffset() {
		return getFileContent().indexOf(extract);
	}

	public HgRoot getRoot() {
		return root;
	}

	public String getExtract() {
		return extract;
	}

	public void setFileContent(String fileContent) {
		this.fileContent = fileContent;
	}

	public MercurialRevisionStorage getMercurialRevisionStorage() {
		if (mercurialRevisionStorage == null) {
			mercurialRevisionStorage = new MercurialRevisionStorage(file, rev);
		}
		return mercurialRevisionStorage;
	}

	public void setMercurialRevisionStorage(
			MercurialRevisionStorage mercurialRevisionStorage) {
		this.mercurialRevisionStorage = mercurialRevisionStorage;
	}

}
