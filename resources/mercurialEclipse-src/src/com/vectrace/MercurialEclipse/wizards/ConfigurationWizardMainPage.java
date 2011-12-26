/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 *     Bastian Doetsch              - adaptions&additions
 *     Adam Berkes (Intland)        - repository location handling
 *     Zsolt Koppany (Intland)		- bug fixes
 *     Andrei Loskutov	- bug fixes
 ******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgPathsClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocationManager;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocationParser;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * Wizard page for entering information about a Hg repository location. This
 * wizard can be initialized using setProperties or using setDialogSettings
 */
public class ConfigurationWizardMainPage extends HgWizardPage {

	private static final String REPO_SEPARATOR = "----------------";
	public static final String PROP_PASSWORD = "password";
	public static final String PROP_USER = "user";
	public static final String PROP_URL = "url";

	private static final int COMBO_HISTORY_LENGTH = 10;

	/**  Dialog store id constant */
	private static final String STORE_USERNAME_ID = "ConfigurationWizardMainPage.STORE_USERNAME_ID"; //$NON-NLS-1$

	private boolean showCredentials;
	private boolean showBundleButton;

	private Combo userCombo;
	protected Text passwordText;

	/** url of the repository we want to add */
	private Combo urlCombo;

	/** local repositories button */
	private Button browseButton;

	private Button browseFileButton;

	private IHgRepositoryLocation initialRepo;
	private Composite authComposite;
	private HgRoot hgRoot;
	private Group urlGroup;

	/**
	 * @param pageName
	 *            the name of the page
	 * @param title
	 *            the title of the page
	 * @param titleImage
	 *            the image for the page
	 */
	public ConfigurationWizardMainPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}

	/**
	 * Adds an entry to a history, while taking care of duplicate history items
	 * and excessively long histories. The assumption is made that all histories
	 * should be of length
	 * <code>ConfigurationWizardMainPage.COMBO_HISTORY_LENGTH</code>.
	 *
	 * @param history
	 *            the current history
	 * @param newEntry
	 *            the entry to add to the history
	 * @param limitHistory
	 *            number of max entries, -1 if no limit
	 * @return the history with the new entry appended
	 */
	private String[] addToHistory(String[] history, String newEntry, int limitHistory) {
		ArrayList<String> list = new ArrayList<String>();
		if (history != null) {
			list.addAll(Arrays.asList(history));
		}

		list.remove(newEntry);
		list.add(0, newEntry);

		// since only one new item was added, we can be over the limit
		// by at most one item
		if (list.size() > COMBO_HISTORY_LENGTH && limitHistory > 0) {
			list.remove(COMBO_HISTORY_LENGTH);
		}

		return list.toArray(new String[list.size()]);
	}

	/**
	 * Creates the UI part of the page.
	 *
	 * @param parent
	 *            the parent of the created widgets
	 */
	public void createControl(Composite parent) {
		Composite composite = SWTWidgetHelper.createComposite(parent, 1);

		Listener listener = new Listener() {
			public void handleEvent(Event event) {
				urlChanged();
			}
		};

		createUrlControl(composite, listener);

		if (showCredentials) {
			createAuthenticationControl(composite);
		}
		setControl(composite);
		urlCombo.setFocus();

		initializeValues();
		boolean ok = validateFields();
		setPageComplete(ok);
		if(ok) {
			setErrorMessage(null);
		}
	}

	private void createUrlControl(Composite composite, final Listener listener) {
		Composite urlComposite = SWTWidgetHelper.createComposite(composite, 4);

		urlGroup = SWTWidgetHelper.createGroup(urlComposite,
				Messages.getString("ConfigurationWizardMainPage.urlGroup.title"), 4, //$NON-NLS-1$
				GridData.FILL_HORIZONTAL);

		// repository Url
		SWTWidgetHelper.createLabel(urlGroup, Messages.getString("ConfigurationWizardMainPage.urlLabel.text")); //$NON-NLS-1$
		urlCombo = createEditableCombo(urlGroup);
		urlCombo.addListener(SWT.Modify, listener);

		browseButton = SWTWidgetHelper.createPushButton(urlGroup, Messages.getString("ConfigurationWizardMainPage.browseButton.text"), 1); //$NON-NLS-1$
		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				File localDirectory = getLocalDirectory(getUrlText());
				if(localDirectory != null) {
					dialog.setFilterPath(localDirectory.getAbsolutePath());
				}
				dialog.setMessage(Messages.getString("ConfigurationWizardMainPage.dialog.message")); //$NON-NLS-1$
				String dir = dialog.open();
				if (dir != null) {
					dir = dir.trim();
					getUrlCombo().setText(dir);
				}
			}
		});

		if (showBundleButton) {
			browseFileButton = SWTWidgetHelper.createPushButton(urlGroup, Messages.getString("PullPage.browseFileButton.text"), 1); //$NON-NLS-1$

			browseFileButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					FileDialog dialog = new FileDialog(getShell());
					dialog.setText(Messages.getString("PullPage.bundleDialog.text")); //$NON-NLS-1$
					String file = dialog.open();
					if (file != null) {
						getUrlCombo().setText(file);
					}
				}
			});
		}

		urlCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {

				IHgRepositoryLocation repo;
				try {
					// note that repo will not be null, will be blank
					// repo if no existing one was found
					repo = MercurialEclipsePlugin.getRepoManager().getRepoLocation(getUrlText());
				} catch (HgException e1) {
					// Lookup obviously failed, but verification will pick this error up later
					// no need to report typing error
					return;
				}
				if (getUserCombo() != null) {
					String user = repo.getUser();
					if (user != null && user.length() != 0) {
						getUserCombo().setText(user);
					} else {
						getUserCombo().setText("");
					}
				}
				if (getPasswordText() != null) {
					String password = repo.getPassword();
					if (password != null && password.length() != 0) {
						passwordText.setText(password);
					} else {
						passwordText.setText("");
					}
				}
			}
		});
	}

	public void setUrlGroupEnabled(boolean enable){
		if(urlGroup != null) {
			urlGroup.setEnabled(enable);
			Control[] children = urlGroup.getChildren();
			if(children != null){
				for (Control control : children) {
					control.setEnabled(enable);
				}
			}
		}
	}

	private void createAuthenticationControl(Composite composite) {
		authComposite = SWTWidgetHelper.createComposite(composite, 2);
		Group g = SWTWidgetHelper.createGroup(
				authComposite,
				Messages.getString("ConfigurationWizardMainPage.authenticationGroup.title")); //$NON-NLS-1$

		// User name
		SWTWidgetHelper.createLabel(g, Messages.getString("ConfigurationWizardMainPage.userLabel.text")); //$NON-NLS-1$
		userCombo = createEditableCombo(g);

		// Password
		SWTWidgetHelper.createLabel(g, Messages.getString("ConfigurationWizardMainPage.passwordLabel.text")); //$NON-NLS-1$
		passwordText = SWTWidgetHelper.createPasswordField(g);

		userCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent arg0) {
				canFlipToNextPage();
			}
		});

		passwordText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent arg0) {
				canFlipToNextPage();
			}
		});
	}

	protected void setAuthCompositeEnabled(boolean enable){
		authComposite.setEnabled(enable);
		userCombo.setEnabled(enable);
		passwordText.setEnabled(enable);
	}

	/**
	 * Utility method to create an editable combo box
	 *
	 * @param parent
	 *            the parent of the combo box
	 * @return the created combo
	 */
	protected Combo createEditableCombo(Composite parent) {
		Combo combo = new Combo(parent, SWT.NULL);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		combo.setLayoutData(data);
		return combo;
	}

	/**
	 * @see HgWizardPage#finish
	 */
	@Override
	public boolean finish(IProgressMonitor monitor) {
		// Set the result to be the current values
		properties = createProperties();

		saveWidgetValues();
		return true;
	}

	protected Properties createProperties() {
		Properties result = new Properties();
		if (showCredentials) {
			result.setProperty(PROP_USER, getUserText());
			result.setProperty(PROP_PASSWORD, passwordText.getText());
		}
		result.setProperty(PROP_URL, getUrlText());
		return result;
	}

	/**
	 * Initializes states of the controls.
	 */
	private void initializeValues() {
		// Set remembered values
		IDialogSettings setts = getDialogSettings();
		if (setts != null) {
			String[] hostNames = updateHostNames();
			if (hostNames != null) {
				for (String hn : hostNames) {
					urlCombo.add(hn);
				}
			}
			if (showCredentials) {
				String[] userNames = setts.getArray(STORE_USERNAME_ID);
				if (userNames != null) {
					for (String un : userNames) {
						userCombo.add(un);
					}
				}
			}
		}

		if (properties != null) {
			if (showCredentials) {
				String user = properties.getProperty(PROP_USER);
				if (user != null) {
					userCombo.setText(user);
				}

				String password = properties.getProperty(PROP_PASSWORD);
				if (password != null) {
					passwordText.setText(password);
				}
			}
			String host = properties.getProperty(PROP_URL);
			if (host != null) {
				urlCombo.setText(host);
			}
		}
	}

	/**
	 * Saves the widget values for the next time
	 */
	private void saveWidgetValues() {
		// Update history
		IDialogSettings dialogSettings = getDialogSettings();
		if (settings != null) {
			if (showCredentials) {
				String[] userNames = dialogSettings.getArray(STORE_USERNAME_ID);
				if (userNames == null) {
					userNames = new String[0];
				}
				userNames = addToHistory(userNames, getUserText(), COMBO_HISTORY_LENGTH);
				dialogSettings.put(STORE_USERNAME_ID, userNames);
			}
		}
	}

	protected String getUserText() {
		return userCombo.getText().trim();
	}

	protected String getUrlText() {
		String text = urlCombo.getText();
		if(REPO_SEPARATOR.equals(text)){
			return "";
		}
		text = HgRepositoryLocationParser.trimLocation(text);
		return text;
	}

	private String[] updateHostNames() {
		List<String> newHostNames = new ArrayList<String>();
		Set<IHgRepositoryLocation> repositories;
		Set<IHgRepositoryLocation> all = MercurialEclipsePlugin.getRepoManager().getAllRepoLocations();
		if (hgRoot == null) {
			repositories = new TreeSet<IHgRepositoryLocation>();
		} else {
			repositories = MercurialEclipsePlugin.getRepoManager().getAllRepoLocations(hgRoot);
		}
		for (IHgRepositoryLocation repoLocation : repositories) {
			if(repoLocation.getLocation() != null) {
				newHostNames.add(repoLocation.getLocation());
			}
		}
		if(repositories.size() > 0) {
			newHostNames.add(REPO_SEPARATOR);
		}
		for (IHgRepositoryLocation repoLocation : all) {
			String location = repoLocation.getLocation();
			if(location != null && !newHostNames.contains(location)) {
				newHostNames.add(location);
			}
		}
		return newHostNames.toArray(new String[newHostNames.size()]);
	}

	/**
	 * Validates the contents of the editable fields and set page completion and
	 * error messages appropriately. Call each time url or username is modified
	 */
	protected boolean validateFields() {
		// first check the url of the repository
		String url = getUrlText();

		if (url.length() == 0) {
			setErrorMessage(null);
			return false;
		}
		File localDirectory = getLocalDirectory(url);
		if(localDirectory != null){
			if(!localDirectory.exists()){
				setErrorMessage("Please provide a valid url or an existing directory!");
				return false;
			}
			File hgRepo = new File(localDirectory, ".hg");
			if(!hgRepo.isDirectory()){
				setErrorMessage("Directory " + localDirectory + " does not contain a valid hg repository!");
				return false;
			}
		}
		return true;
	}

	/**
	 * @param urlString non null
	 * @return true if the given url can be threated as local directory
	 */
	protected File getLocalDirectory(String urlString) {
		if (urlString != null) {
			urlString = urlString.trim();
		}

		if (urlString == null || urlString.length() == 0 || urlString.contains("http:") || urlString.contains("https:") || urlString.contains("ftp:") || urlString.contains("ssh:")){
			return null;
		}

		File dir = new File(urlString);
		if (dir.isDirectory()) {
			return dir;
		}

		try {
			// Supporting file:// URLs
			URL url = new URL(urlString);
			return new File(url.getPath());
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			if (urlCombo != null) {
				urlCombo.setFocus();
			}
		}
	}

	@Override
	public boolean canFlipToNextPage() {
		return super.canFlipToNextPage();
	}

	public boolean isShowCredentials() {
		return showCredentials;
	}

	public void setShowCredentials(boolean showCredentials) {
		this.showCredentials = showCredentials;
	}

	protected Combo getUserCombo() {
		return userCombo;
	}

	public String getPasswordText() {
		return passwordText.getText();
	}

	protected Combo getUrlCombo() {
		return urlCombo;
	}

	public boolean isShowBundleButton() {
		return showBundleButton;
	}

	public void setShowBundleButton(boolean showBundleButton) {
		this.showBundleButton = showBundleButton;
	}

	protected boolean optionChanged() {
		return urlChanged();
	}

	/**
	 * Triggered if the user has changed repository url. Override to implement additional
	 * checks after it.
	 * @return true, if the filed validation was successful
	 */
	protected boolean urlChanged() {
		boolean ok = validateFields();
		setPageComplete(ok);
		if(ok) {
			setErrorMessage(null);
		}
		return ok;
	}

	protected HgRoot getHgRoot() {
		return hgRoot;
	}

	protected void initDefaultLocation() {
		IHgRepositoryLocation defaultLocation = null;
		if (getHgRoot() != null) {
			defaultLocation = getRepoFromRoot();
		}
		if(defaultLocation == null){
			defaultLocation = getInitialRepo();
		}
		setRepository(defaultLocation);
	}

	protected IHgRepositoryLocation getRepoFromRoot(){
		HgRepositoryLocationManager mgr = MercurialEclipsePlugin.getRepoManager();
		IHgRepositoryLocation defaultLocation = mgr.getDefaultRepoLocation(getHgRoot());
		Set<IHgRepositoryLocation> repos = mgr.getAllRepoLocations(getHgRoot());
		if (defaultLocation == null) {
			for (IHgRepositoryLocation repo : repos) {
				if (HgPathsClient.DEFAULT_PULL.equals(repo.getLogicalName())
						|| HgPathsClient.DEFAULT.equals(repo.getLogicalName())) {
					defaultLocation = repo;
					break;
				}
			}
		}
		return defaultLocation;
	}

	public void setRepository(IHgRepositoryLocation repo) {
		if (repo == null) {
			return;
		}
		getUrlCombo().setText(repo.getLocation());

		String user = repo.getUser();
		if (user != null && user.length() != 0) {
			getUserCombo().setText(user);
		}
		String password = repo.getPassword();
		if (password != null && password.length() != 0) {
			passwordText.setText(password);
		}
	}

	@Override
	public void setProperties(Properties properties) {
		super.setProperties(properties);
		if(urlCombo != null && isValid(properties, PROP_URL)){
			String[] items = urlCombo.getItems();
			if(items != null){
				String url = properties.getProperty(PROP_URL);
				for (int i = 0; i < items.length; i++) {
					if(url.equals(items[i])){
						urlCombo.select(i);
						break;
					}
				}
			}
		}
		if(userCombo != null && isValid(properties, PROP_USER)){
			String[] items = userCombo.getItems();
			if(items != null){
				String user = properties.getProperty(PROP_USER);
				for (int i = 0; i < items.length; i++) {
					if(user.equals(items[i])){
						userCombo.select(i);
						break;
					}
				}
			}
		}
		if(passwordText != null && isValid(properties, PROP_PASSWORD)){
			passwordText.setText(properties.getProperty(PROP_PASSWORD));
		}
	}

	private static boolean isValid(Properties properties, String key){
		String value = properties.getProperty(key);
		return value != null && value.trim().length() > 0;
	}

	public void setInitialRepo(IHgRepositoryLocation initialRepo) {
		this.initialRepo = initialRepo;
	}

	public IHgRepositoryLocation getInitialRepo() {
		return initialRepo;
	}

	public void setHgRoot(HgRoot hgRoot) {
		this.hgRoot = hgRoot;
	}
}
