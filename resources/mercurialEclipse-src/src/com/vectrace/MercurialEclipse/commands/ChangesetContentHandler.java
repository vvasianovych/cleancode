/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Bastian Doetsch	implementation
 * 		Andrei Loskutov - bugfixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileStatus;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.model.FileStatus.Action;
import com.vectrace.MercurialEclipse.team.cache.RemoteData;

/**
 * This class helps HgClients to parse the changeset output of hg to Changeset
 * objects.
 *
 * @author Bastian Doetsch
 */
final class ChangesetContentHandler implements ContentHandler {

	private static final String[] EMPTY = new String[0];
	private static final Pattern LT = Pattern.compile("&lt;");
	private static final Pattern GT = Pattern.compile("&gt;");
	private static final Pattern AMP = Pattern.compile("&amp;");
	private static final Pattern NEWLINE_TAB = Pattern.compile("\n\t");
	private static final Pattern WORDS =  Pattern.compile(" ");

	private String branchStr;
	private String tags;
	private int rev;
	private String nodeShort;
	private String nodeLong;
	private String dateIso;
	private String author;
	private String parents;
	private String description;
	private StringBuilder cDataChars;
	private boolean readCdata;
	private final IPath res;
	private final Direction direction;
	private final IHgRepositoryLocation repository;
	private final File bundleFile;
	private final HgRoot hgRoot;
	private final Map<IPath, Set<ChangeSet>> fileRevisions;
	private Set<String> filesModified;
	private Set<String> filesAdded;
	private Set<String> filesRemoved;
	private Map<String, String> filesCopied; // destination -> source
	private Map<String, String> filesMoved; // destination -> source
	private Action action;
	private String prevNodeShort;
	private int prevRev;
	private final String expectedBranch;
	private final IPath repoPath;
	private final boolean withFiles;


	ChangesetContentHandler(IPath res, boolean withFiles, Direction direction, IHgRepositoryLocation repository,
			File bundleFile, String branch, HgRoot hgRoot) {

		this.res = res;
		this.withFiles = withFiles;
		this.direction = direction;
		this.repository = repository;
		this.bundleFile = bundleFile;
		this.hgRoot = hgRoot;
		this.expectedBranch = branch;
		repoPath = new Path(hgRoot.getPath());
		fileRevisions = new HashMap<IPath, Set<ChangeSet>>();
	}

	private boolean hasFileData()
	{
		return filesModified != null;
	}

	private void initFileData()
	{
		if (!hasFileData())
		{
		filesModified = new TreeSet<String>();
		filesAdded = new TreeSet<String>();
		filesRemoved = new TreeSet<String>();
		filesCopied = new TreeMap<String, String>();
		filesMoved = new TreeMap<String, String>();
	}
	}

	private static String replaceAll(Pattern p, String source, String replacement){
		return p.matcher(source).replaceAll(replacement);
	}

	private static String unescape(String string) {
		String result = replaceAll(LT, string, "<");
		result = replaceAll(GT, result, ">");
		return replaceAll(AMP, result, "&");
	}

	/**
	 * Remove a leading tab on each line in the string.
	 *
	 * @param string
	 * @return
	 */
	private static String untab(String string) {
		return replaceAll(NEWLINE_TAB, string, "\n");
	}


	private static String[] splitWords(String string) {
		if (string == null || string.length() == 0) {
			return EMPTY;
		}
		return WORDS.split(string);
	}

	public void characters(char[] ch, int start, int length) {
		if(readCdata) {
			cDataChars.append(ch, start, length);
		}
	}

	public void endDocument() throws SAXException {
	}

	public void endElement(String uri, String localName, String name) throws SAXException {

		if ("de".equals(name)) {
			description = cDataChars.toString();
		} else if ("br".equals(name)) {
			branchStr = cDataChars.toString();
		} else if ("tg".equals(name)) {
			tags = cDataChars.toString();
		} else if ("cs".equals(name)) {
			// only collect changesets from requested branch. Null is: collect everything.
			if(expectedBranch == null || Branch.same(branchStr, expectedBranch)){
				ChangeSet.Builder csb = new ChangeSet.Builder(rev, nodeLong, branchStr, dateIso, unescape(author), hgRoot);
				csb.tags(tags);
				csb.nodeShort(nodeShort);
				csb.description(untab(unescape(description)));

				addParentsInfo(csb);

				csb.bundleFile(bundleFile);
				csb.direction(direction);
				csb.repository(repository);

				if (hasFileData())
				{
				List<FileStatus> list = new ArrayList<FileStatus>(
						filesModified.size() + filesAdded.size() + filesRemoved.size());
				for (String file : filesModified) {
					list.add(new FileStatus(FileStatus.Action.MODIFIED, file, hgRoot));
				}
				for (String file : filesAdded) {
					list.add(new FileStatus(FileStatus.Action.ADDED, file, hgRoot));
				}
				for (String file : filesRemoved) {
					list.add(new FileStatus(FileStatus.Action.REMOVED, file, hgRoot));
				}
				for(Map.Entry<String, String> entry : filesCopied.entrySet()){
					list.add(new FileStatus(FileStatus.Action.COPIED, entry.getKey(), entry.getValue(), hgRoot));
				}

				for(Map.Entry<String, String> entry : filesMoved.entrySet()){
					list.add(new FileStatus(FileStatus.Action.MOVED, entry.getKey(), entry.getValue(), hgRoot));
				}
				csb.changedFiles(list.toArray(new FileStatus[list.size()]));
				}

				ChangeSet changeSet = csb.build();

				// changeset to resources & project
				addChangesetToResourceMap(changeSet);
			}

			if (hasFileData()) {
			filesModified.clear();
			filesAdded.clear();
			filesRemoved.clear();
			filesCopied.clear();
			filesMoved.clear();
			}
			prevRev = rev;
			prevNodeShort = nodeShort;
		}
	}

	/**
	 * It seems that hg do not report parents if the parent changeset is printed out in
	 * the same command output. So we guess: if parents are empty, we have to create them
	 * from the revision + short node of the previous run.
	 * @param csb
	 */
	private void addParentsInfo(ChangeSet.Builder csb) {
		String[] myParents = splitWords(parents);
		if(myParents.length == 0 && prevRev == rev - 1 && prevNodeShort != null){
			myParents = new String[]{prevRev + ":" + prevNodeShort};
		}
		csb.parents(myParents);
	}

	/**
	 * <p>
	 * Format of input is defined in the two style files in /styles and is as
	 * follows for each changeset.
	 *
	 * <pre>
	 * &lt;cs&gt;
	 * &lt;br v=&quot;{branches}&quot;/&gt;
	 * &lt;tg v=&quot;{tags}&quot;/&gt;
	 * &lt;rv v=&quot;{rev}&quot;/&gt;
	 * &lt;ns v=&quot;{node|short}&quot;/&gt;
	 * &lt;nl v=&quot;{node}&quot;/&gt;
	 * &lt;di v=&quot;{date|isodate}&quot;/&gt;
	 * &lt;da v=&quot;{date|age}&quot;/&gt;
	 * &lt;au v=&quot;{author|person}&quot;/&gt;
	 * &lt;pr v=&quot;{parents}&quot;/&gt;
	 * &lt;de v=&quot;{desc|escape|tabindent}&quot;/&gt;
	 * &lt;fl v=&quot;{files}&quot;/&gt;
	 * &lt;fa v=&quot;{file_adds}&quot;/&gt;
	 * &lt;fd v=&quot;{file_dels}&quot;/&gt;
	 * &lt;f v=&quot;{root relative path}&quot;/&gt;
	 * &lt;/cs&gt;
	 * </pre>
	 * {@inheritDoc}
	 */
	public void startElement(String uri, String localName, String name, Attributes atts)
			throws SAXException {
		/*
		 * <br v="{branches}"/> <tg v="{tags}"/> <rv v="{rev}"/> <ns
		 * v="{node|short}"/> <nl v="{node}"/> <di v="{date|isodate}"/> <da
		 * v="{date|age}"/> <au v="{author|person}"/> <pr v="{parents}"/>
		 * <de>{desc|escape|tabindent}</de>
		 */
		readCdata = false;
		if ("br".equals(name)) {
			branchStr = null;
			cDataChars = new StringBuilder(42);
			readCdata = true;
		} else if ("tg".equals(name)) {
			tags = null;
			cDataChars = new StringBuilder(42);
			readCdata = true;
		} else if ("rv".equals(name)) {
			rev = Integer.parseInt(atts.getValue(0));
		} else if ("ns".equals(name)) {
			nodeShort = atts.getValue(0);
		} else if ("nl".equals(name)) {
			nodeLong = atts.getValue(0);
		} else if ("di".equals(name)) {
			dateIso = atts.getValue(0);
		} else if ("au".equals(name)) {
			author = atts.getValue(0);
		} else if ("pr".equals(name)) {
			parents = atts.getValue(0);
		} else if ("de".equals(name)) {
			description = null;
			cDataChars = new StringBuilder(42);
			readCdata = true;
		} else if ("fl".equals(name)) {
			initFileData();
			action = FileStatus.Action.MODIFIED;
		} else if ("fa".equals(name)) {
			initFileData();
			action = FileStatus.Action.ADDED;
		} else if ("fd".equals(name)) {
			initFileData();
			action = FileStatus.Action.REMOVED;
		} else if ("fc".equals(name)) {
			initFileData();
			action = FileStatus.Action.COPIED;
		} else if ("f".equals(name)) {
			if (action == Action.MODIFIED) {
				filesModified.add(atts.getValue("", "v"));
			} else if (action == Action.ADDED) {
				String value = atts.getValue("", "v");
				filesAdded.add(value);
			} else if (action == Action.REMOVED) {
				String value = atts.getValue("", "v");
				filesRemoved.add(value);
			} else if(action == Action.COPIED) {
				String dest = atts.getValue("", "v");
				String src = atts.getValue("", "s");
				if(filesRemoved.contains(src)){
					filesMoved.put(dest, src);
				}else{
					filesCopied.put(dest, src);
				}
				filesAdded.remove(dest);
				filesRemoved.remove(src);
			}
		}
	}

	private void addChangesetToResourceMap(final ChangeSet cs) {
		if(withFiles) {
			if (cs.getChangedFiles() != null) {
				for (FileStatus file : cs.getChangedFiles()) {
					IPath fileAbsPath = file.getAbsolutePath();
					mapPathToChangeset(fileAbsPath, cs);
				}
			}
			mapPathToChangeset(res, cs);
		} else {
			mapPathToChangeset(repoPath, cs);
		}
	}

	private void mapPathToChangeset(IPath path, ChangeSet cs) {
		Set<ChangeSet> fileRevs = fileRevisions.get(path);
		if (fileRevs == null) {
			fileRevs = new HashSet<ChangeSet>();
		}
		fileRevs.add(cs);
		fileRevisions.put(path, fileRevs);
	}

	/**
	 * @return never null
	 */
	public Map<IPath, Set<ChangeSet>> getFileRevisions() {
		return fileRevisions;
	}

	public RemoteData createRemoteData() {
		return new RemoteData(repository, hgRoot, expectedBranch, direction, fileRevisions);
	}

	//####################################################################################

	public void endPrefixMapping(String prefix) throws SAXException {
	}

	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
	}

	public void processingInstruction(String target, String data) throws SAXException {
	}

	public void setDocumentLocator(Locator locator) {
	}

	public void skippedEntity(String name) throws SAXException {
	}

	public void startDocument() throws SAXException {
	}

	public void startPrefixMapping(String prefix, String uri) throws SAXException {
	}
}