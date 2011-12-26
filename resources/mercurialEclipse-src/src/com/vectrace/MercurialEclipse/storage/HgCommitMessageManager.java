/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Zingo Andersen           - Save/Load commit messages using a xml file
 *     Adam Berkes (Intland)    - Fix encoding
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.jface.preference.IPreferenceStore;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.utils.StringUtils;

/**
 * A manager for all Mercurial commit messages. The commit messages are save to a xml file when
 * closing down and then re-read when the plugin is started.
 *
 */
public class HgCommitMessageManager {

	/**
	 * commit messages database (keep it simple)
	 */
	private static List<String> commitMessages = new ArrayList<String>();

	/**
	 * Prefix for the pref store for the hg root default commit name
	 */
	public static final String KEY_PREFIX_COMMIT_NAME = "commitName_"; //$NON-NLS-1$

	private static final String COMMIT_MESSAGE_FILE = "commit_messages.xml"; //$NON-NLS-1$
	private static final String XML_TAG_COMMIT_MESSAGE = "commitmessage"; //$NON-NLS-1$
	private static final String XML_TAG_COMMIT_MESSAGES = "commitmessages"; //$NON-NLS-1$

	private final XmlHandler xmlHandler;

	public HgCommitMessageManager() {
		xmlHandler = new XmlHandler(this);
	}

	/**
	 * Save message in in-memory database
	 */
	public void saveCommitMessage(String message) {
		if (commitMessages.contains(message)) {
			// remove it, and put it in front
			commitMessages.remove(message);
			commitMessages.add(0, message);
			return;
		}

		commitMessages.add(0, message);
		restrictSavedCommitMessages();
	}

	/**
	 * Make sure we don't have more commit messages than are allowed in the plugin prefs.
	 */
	private void restrictSavedCommitMessages() {
		final int prefsCommitMessageSizeMax = Integer.parseInt(MercurialUtilities.getPreference(
				MercurialPreferenceConstants.COMMIT_MESSAGE_BATCH_SIZE, "10")); //$NON-NLS-1$

		while (commitMessages.size() > prefsCommitMessageSizeMax) {
			commitMessages.remove(commitMessages.size() - 1);
		}
	}

	/**
	 * Save message in in-memory database new data last (used when loading from file)
	 */
	private void addCommitMessage(String message) {
		commitMessages.add(message);
		restrictSavedCommitMessages();
	}

	/**
	 * Get all messages from in-memory database
	 */
	public String[] getCommitMessages() {
		restrictSavedCommitMessages();
		return commitMessages.toArray(new String[0]);
	}

	/**
	 * Return a <code>File</code> object representing the location file. The file may or may not
	 * exist and must be checked before use.
	 */
	private File getLocationFile() {
		return MercurialEclipsePlugin.getDefault().getStateLocation().append(COMMIT_MESSAGE_FILE)
				.toFile();
	}

	/**
	 * Load all saved commit messages from the plug-in's default area.
	 *
	 * @throws HgException
	 */
	public void start() throws HgException {
		File file = getLocationFile();
		if (!file.isFile()) {
			return;
		}
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file),
					MercurialEclipsePlugin.getDefaultEncoding()));
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			parserFactory.setValidating(false);
			SAXParser parser = parserFactory.newSAXParser();
			parser.parse(new InputSource(reader), xmlHandler);
		} catch (SAXException e) {
			throw new HgException("Failed to open commit database file: " + file, e);
		} catch (ParserConfigurationException e) {
			throw new HgException("Failed to open commit database file: " + file, e);
		} catch (IOException e) {
			throw new HgException("Failed to open commit database file: " + file, e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					MercurialEclipsePlugin.logError(e);
				}
			}
		}
	}

	/**
	 * Save all commit messages from the in-memory database to the plug-in's default area.
	 */
	public void stop() throws IOException {
		File file = getLocationFile();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(file), MercurialEclipsePlugin.getDefaultEncoding()));

		StreamResult streamResult = new StreamResult(writer);
		SAXTransformerFactory transformerFactory = (SAXTransformerFactory) TransformerFactory
				.newInstance();

		try {
			TransformerHandler transformerHandler = transformerFactory.newTransformerHandler();
			Transformer transformer = transformerHandler.getTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, MercurialEclipsePlugin
					.getDefaultEncoding());
			/*
			 * transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,"mercurialeclipse_commitmessage.dtd"
			 * ); //$NON-NLS-1$
			 */
			transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
			transformerHandler.setResult(streamResult);
			transformerHandler.startDocument();

			AttributesImpl atts = new AttributesImpl();
			atts.clear();
			transformerHandler.startElement("", "", XML_TAG_COMMIT_MESSAGES, atts); //$NON-NLS-1$

			int size = commitMessages.size();

			final int prefsCommitMessageSizeMax = Integer.parseInt(MercurialUtilities.getPreference(
					MercurialPreferenceConstants.COMMIT_MESSAGE_BATCH_SIZE, "10")); //$NON-NLS-1$

			/* Do not save more then the prefs size */
			if (size > prefsCommitMessageSizeMax) {
				size = prefsCommitMessageSizeMax;
			}

			for (int i = 0; i < size; i++) {
				String msg = commitMessages.get(i);
				transformerHandler.startElement("", "", XML_TAG_COMMIT_MESSAGE, atts); //$NON-NLS-1$
				transformerHandler.characters(msg.toCharArray(), 0, msg.length());
				transformerHandler.endElement("", "", XML_TAG_COMMIT_MESSAGE); //$NON-NLS-1$
			}
			transformerHandler.endElement("", "", XML_TAG_COMMIT_MESSAGES); //$NON-NLS-1$
			transformerHandler.endDocument();
		} catch (TransformerConfigurationException e) {
			MercurialEclipsePlugin.logError(e);
		} catch (IllegalArgumentException e) {
			MercurialEclipsePlugin.logError(e);
		} catch (SAXException e) {
			MercurialEclipsePlugin.logError(e);
		}

		writer.close();
	}

	/**
	 * Get the commit name for given root
	 *
	 * @param hgRoot
	 *            non null
	 * @return never null, but might be empty
	 */
	public static String getDefaultCommitName(HgRoot hgRoot) {
		// first the stored commit name
		IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();
		String commitName = store.getString(getKey(hgRoot));
		if (!StringUtils.isEmpty(commitName)) {
			return commitName;
		}

		String defaultUserName = hgRoot.getUser();
		if(StringUtils.isEmpty(defaultUserName)){
			defaultUserName = MercurialUtilities.getDefaultUserName();
		}

		/*
		 * dependent on the preference, use configured Mercurial name or repository
		 * username (in some corporate environments this seems to be necessary)
		 */
		if ("true".equals(MercurialUtilities.getPreference(
				MercurialPreferenceConstants.PREF_USE_MERCURIAL_USERNAME, "true"))) {
			return defaultUserName;
		}

		IHgRepositoryLocation repoLocation = MercurialEclipsePlugin.getRepoManager()
				.getDefaultRepoLocation(hgRoot);
		if (repoLocation != null) {
			String user = repoLocation.getUser();
			if (!StringUtils.isEmpty(user)) {
				return user;
			}
		}
		return defaultUserName;
	}

	public static void setDefaultCommitName(HgRoot hgRoot, String name) {
		IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();
		store.setValue(getKey(hgRoot), name);
	}

	private static String getKey(HgRoot root) {
		return HgCommitMessageManager.KEY_PREFIX_COMMIT_NAME + root.getAbsolutePath();
	}

	/**
	 * SAX Handler methods class to handle XML parsing
	 */
	private static final class XmlHandler extends DefaultHandler {

		private final HgCommitMessageManager mgr;
		private String tmpMessage;

		private XmlHandler(HgCommitMessageManager mgr) {
			this.mgr = mgr;
		}

		/**
		 * Called when the starting of the Element is reached. For Example if we have Tag called
		 * <Title> ... </Title>, then this method is called when <Title> tag is Encountered while
		 * parsing the Current XML File. The AttributeList Parameter has the list of all Attributes
		 * declared for the Current Element in the XML File.
		 */
		@Override
		public void startElement(String uri, String localName, String qname, Attributes attr) {
			/* Clear char string */
			tmpMessage = ""; //$NON-NLS-1$
		}

		/**
		 * Called when the Ending of the current Element is reached. For example in the above
		 * explanation, this method is called when </Title> tag is reached
		 */
		@Override
		public void endElement(String uri, String localName, String qname) {
			/* If it was a commit message save the char string in the database */
			if (qname.equalsIgnoreCase(XML_TAG_COMMIT_MESSAGE)) {
				mgr.addCommitMessage(tmpMessage);
			}
		}

		/**
		 * While Parsing the XML file, if extra characters like space or enter Character are
		 * encountered then this method is called. If you don't want to do anything special with
		 * these characters, then you can normally leave this method blank.
		 */
		@Override
		public void characters(char[] ch, int start, int length) {
			/* Collect the char string together this will be called for every special char */
			tmpMessage = tmpMessage + new String(ch, start, length);
		}
	}

}
