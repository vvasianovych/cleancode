/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Juerg Billeter, juergbi@ethz.ch - 47136 Search view should show match objects
 *     Ulrich Etter, etteru@ethz.ch - 47136 Search view should show match objects
 *     Roman Fuchs, fuchsro@ethz.ch - 47136 Search view should show match objects
 *     Bastian Doetsch - adaptation for MercurialEclipse
 *     Andrei Loskutov - bug fixes
 *     Philip Graf - Fixed bugs which FindBugs found
 *******************************************************************************/
package com.vectrace.MercurialEclipse.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.text.FindReplaceDocumentAdapterContentProposalProvider;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.search.internal.core.text.PatternConstructor;
import org.eclipse.search.internal.ui.SearchMessages;
import org.eclipse.search.internal.ui.SearchPlugin;
import org.eclipse.search.internal.ui.util.FileTypeEditor;
import org.eclipse.search.internal.ui.util.SWTUtil;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;
import com.vectrace.MercurialEclipse.utils.StringUtils;

@SuppressWarnings("restriction")
public class MercurialTextSearchPage extends DialogPage implements ISearchPage {

	private static final int HISTORY_SIZE = 12;
	public static final String EXTENSION_POINT_ID = "org.eclipse.search.internal.ui.text.TextSearchPage"; //$NON-NLS-1$

	private static final boolean IS_REGEX_SEARCH = true;

	// Dialog store id constants
	private static final String PAGE_NAME = "TextSearchPage"; //$NON-NLS-1$
	private static final String STORE_HISTORY = "HISTORY"; //$NON-NLS-1$
	private static final String STORE_HISTORY_SIZE = "HISTORY_SIZE"; //$NON-NLS-1$

	private final List<SearchPatternData> fPreviousSearchPatterns = new ArrayList<SearchPatternData>(
			20);

	private boolean fFirstTime = true;
	private Combo fPattern;
	private Combo fExtensions;
	private CLabel fStatusLabel;
	private ISearchPageContainer fContainer;
	private FileTypeEditor fFileTypeEditor;

	private ContentAssistCommandAdapter fPatterFieldContentAssist;
	private Button firstRevisionCheckbox;

	private static class SearchPatternData {
		public final boolean isCaseSensitive;
		public final boolean isRegExSearch;
		public final String textPattern;
		public final String[] fileNamePatterns;
		public final int scope;
		public final IWorkingSet[] workingSets;

		public SearchPatternData(String textPattern, boolean isCaseSensitive,
				boolean isRegExSearch, String[] fileNamePatterns, int scope,
				IWorkingSet[] workingSets) {
			Assert.isNotNull(fileNamePatterns);
			this.isCaseSensitive = isCaseSensitive;
			this.isRegExSearch = isRegExSearch;
			this.textPattern = textPattern;
			this.fileNamePatterns = fileNamePatterns;
			this.scope = scope;
			this.workingSets = workingSets; // can be null
		}

		public void store(IDialogSettings settings) {
			settings.put("ignoreCase", !isCaseSensitive); //$NON-NLS-1$
			settings.put("isRegExSearch", isRegExSearch); //$NON-NLS-1$
			settings.put("textPattern", textPattern); //$NON-NLS-1$
			settings.put("fileNamePatterns", fileNamePatterns); //$NON-NLS-1$
			settings.put("scope", scope); //$NON-NLS-1$
			if (workingSets != null) {
				String[] wsIds = new String[workingSets.length];
				for (int i = 0; i < workingSets.length; i++) {
					wsIds[i] = workingSets[i].getLabel();
				}
				settings.put("workingSets", wsIds); //$NON-NLS-1$
			} else {
				settings.put("workingSets", new String[0]); //$NON-NLS-1$
			}

		}

		public static SearchPatternData create(IDialogSettings settings) {
			String textPattern = settings.get("textPattern"); //$NON-NLS-1$
			String[] wsIds = settings.getArray("workingSets"); //$NON-NLS-1$
			IWorkingSet[] workingSets = null;
			if (wsIds != null && wsIds.length > 0) {
				IWorkingSetManager workingSetManager = PlatformUI.getWorkbench()
						.getWorkingSetManager();
				workingSets = new IWorkingSet[wsIds.length];
				for (int i = 0; workingSets != null && i < wsIds.length; i++) {
					workingSets[i] = workingSetManager.getWorkingSet(wsIds[i]);
					if (workingSets[i] == null) {
						workingSets = null;
					}
				}
			}
			String[] fileNamePatterns = settings.getArray("fileNamePatterns"); //$NON-NLS-1$
			if (fileNamePatterns == null) {
				fileNamePatterns = new String[0];
			}
			try {
				int scope = settings.getInt("scope"); //$NON-NLS-1$
				boolean isRegExSearch = settings.getBoolean("isRegExSearch"); //$NON-NLS-1$
				boolean ignoreCase = settings.getBoolean("ignoreCase"); //$NON-NLS-1$

				return new SearchPatternData(textPattern, !ignoreCase, isRegExSearch,
						fileNamePatterns, scope, workingSets);
			} catch (NumberFormatException e) {
				return null;
			}
		}

	}

	static class TextSearchPageInput {

		private final String fSearchText;
		private final MercurialTextSearchScope fScope;

		public TextSearchPageInput(String searchText, MercurialTextSearchScope scope) {
			fSearchText = searchText;
			fScope = scope;
		}

		public String getSearchText() {
			return fSearchText;
		}

		public MercurialTextSearchScope getScope() {
			return fScope;
		}
	}

	// ---- Action Handling ------------------------------------------------

	private ISearchQuery newQuery() {
		SearchPatternData data = getPatternData();
		TextSearchPageInput input = new TextSearchPageInput(data.textPattern,
				createTextSearchScope());
		return new MercurialTextSearchQueryProvider(firstRevisionCheckbox.getSelection())
				.createQuery(input);
	}

	public boolean performAction() {
		NewSearchUI.runQueryInBackground(newQuery());
		return true;
	}

	private String getPattern() {
		return fPattern.getText();
	}

	public MercurialTextSearchScope createTextSearchScope() {
		// Setup search scope
		switch (getContainer().getSelectedScope()) {
		case ISearchPageContainer.WORKSPACE_SCOPE:
			return MercurialTextSearchScope.newWorkspaceScope(getExtensions(), firstRevisionCheckbox.getSelection());
		case ISearchPageContainer.SELECTION_SCOPE:
			return getSelectedResourcesScope();
		case ISearchPageContainer.SELECTED_PROJECTS_SCOPE:
			return getEnclosingProjectScope();
		case ISearchPageContainer.WORKING_SET_SCOPE:
			IWorkingSet[] workingSets = getContainer().getSelectedWorkingSets();
			return MercurialTextSearchScope.newSearchScope(workingSets, getExtensions(),
					firstRevisionCheckbox.getSelection());
		default:
			// unknown scope
			return MercurialTextSearchScope.newWorkspaceScope(getExtensions(),
					firstRevisionCheckbox.getSelection());
		}
	}

	private MercurialTextSearchScope getSelectedResourcesScope() {
		HashSet<IResource> resources = new HashSet<IResource>();
		ISelection sel = getContainer().getSelection();
		if (sel instanceof IStructuredSelection && !sel.isEmpty()) {
			Iterator<?> iter = ((IStructuredSelection) sel).iterator();
			while (iter.hasNext()) {
				Object curr = iter.next();
				if (curr instanceof IWorkingSet) {
					IWorkingSet workingSet = (IWorkingSet) curr;
					if (workingSet.isAggregateWorkingSet() && workingSet.isEmpty()) {
						return MercurialTextSearchScope.newWorkspaceScope(getExtensions(),
								firstRevisionCheckbox.getSelection());
					}
					IAdaptable[] elements = workingSet.getElements();
					for (int i = 0; i < elements.length; i++) {
						IResource resource = (IResource) elements[i].getAdapter(IResource.class);
						if (resource != null && resource.isAccessible()) {
							resources.add(resource);
						}
					}
				} else if (curr instanceof IAdaptable) {
					IResource resource = (IResource) ((IAdaptable) curr)
							.getAdapter(IResource.class);
					if (resource != null && resource.isAccessible()) {
						resources.add(resource);
					}
				}
			}
		}
		IResource[] arr = resources.toArray(new IResource[resources.size()]);
		return MercurialTextSearchScope.newSearchScope(arr, getExtensions(), firstRevisionCheckbox
				.getSelection());
	}

	private MercurialTextSearchScope getEnclosingProjectScope() {
		String[] enclosingProjectName = getContainer().getSelectedProjectNames();
		if (enclosingProjectName == null) {
			return MercurialTextSearchScope.newWorkspaceScope(getExtensions(),
					firstRevisionCheckbox.getSelection());
		}

		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource[] res = new IResource[enclosingProjectName.length];
		for (int i = 0; i < res.length; i++) {
			res[i] = root.getProject(enclosingProjectName[i]);
		}

		return MercurialTextSearchScope.newSearchScope(res, getExtensions(), firstRevisionCheckbox
				.getSelection());
	}

	private SearchPatternData findInPrevious(String pattern) {
		for (Iterator<SearchPatternData> iter = fPreviousSearchPatterns.iterator(); iter.hasNext();) {
			SearchPatternData element = iter.next();
			if (pattern.equals(element.textPattern)) {
				return element;
			}
		}
		return null;
	}

	/**
	 * Return search pattern data and update previous searches. An existing entry will be updated.
	 *
	 * @return the search pattern data
	 */
	private SearchPatternData getPatternData() {
		SearchPatternData match = findInPrevious(fPattern.getText());
		if (match != null) {
			fPreviousSearchPatterns.remove(match);
		}
		match = new SearchPatternData(getPattern(), false, IS_REGEX_SEARCH, getExtensions(),
				getContainer().getSelectedScope(), getContainer().getSelectedWorkingSets());
		fPreviousSearchPatterns.add(0, match);
		return match;
	}

	private String[] getPreviousExtensions() {
		List<String> extensions = new ArrayList<String>(fPreviousSearchPatterns.size());
		int size = fPreviousSearchPatterns.size();
		for (int i = 0; i < size; i++) {
			SearchPatternData data = fPreviousSearchPatterns.get(i);
			String text = FileTypeEditor.typesToString(data.fileNamePatterns);
			if (!extensions.contains(text)) {
				extensions.add(text);
			}
		}
		return extensions.toArray(new String[extensions.size()]);
	}

	private String[] getPreviousSearchPatterns() {
		int size = fPreviousSearchPatterns.size();
		String[] patterns = new String[size];
		for (int i = 0; i < size; i++) {
			patterns[i] = (fPreviousSearchPatterns.get(i)).textPattern;
		}
		return patterns;
	}

	private String[] getExtensions() {
		return fFileTypeEditor.getFileTypes();
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible && fPattern != null) {
			if (fFirstTime) {
				fFirstTime = false;
				// Set item and text here to prevent page from resizing
				fPattern.setItems(getPreviousSearchPatterns());
				fExtensions.setItems(getPreviousExtensions());
				// if (fExtensions.getItemCount() == 0) {
				// loadFilePatternDefaults();
				// }
				if (!initializePatternControl()) {
					fPattern.select(0);
					fExtensions.setText("*"); //$NON-NLS-1$
					handleWidgetSelected();
				}
			}
			fPattern.setFocus();
		}
		updateOKStatus();
		super.setVisible(visible);
	}

	final void updateOKStatus() {
		boolean regexStatus = validateRegex();
		boolean hasFilePattern = fExtensions.getText().length() > 0;
		boolean hasTextPattern = fPattern.getText().length() > 0;
		getContainer().setPerformActionEnabled(regexStatus && hasFilePattern && hasTextPattern);
	}

	// ---- Widget creation ------------------------------------------------

	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		readConfiguration();

		Composite result = new Composite(parent, SWT.NONE);
		result.setFont(parent.getFont());
		GridLayout layout = new GridLayout(2, false);
		result.setLayout(layout);

		addTextPatternControls(result);

		Label separator = new Label(result, SWT.NONE);
		separator.setVisible(false);
		GridData data = new GridData(GridData.FILL, GridData.FILL, false, false, 2, 1);
		data.heightHint = convertHeightInCharsToPixels(1) / 3;
		separator.setLayoutData(data);

		addFileNameControls(result);

		separator = new Label(result, SWT.NONE);
		separator.setVisible(false);
		data = new GridData(GridData.FILL, GridData.FILL, false, false, 2, 1);
		data.heightHint = convertHeightInCharsToPixels(1) / 3;
		separator.setLayoutData(data);

		addSearchInControls(result);

		setControl(result);
		Dialog.applyDialogFont(result);
	}

	private void addSearchInControls(Composite result) {
		Group g = SWTWidgetHelper.createGroup(result, "Search in");
		this.firstRevisionCheckbox = SWTWidgetHelper.createCheckBox(g,
				"Search in all changesets");
		this.firstRevisionCheckbox.setEnabled(true);
		this.firstRevisionCheckbox.setSelection(false);
	}

	private boolean validateRegex() {
		if (IS_REGEX_SEARCH) {
			try {
				PatternConstructor.createPattern(fPattern.getText(), false, true);
			} catch (PatternSyntaxException e) {
				String locMessage = e.getLocalizedMessage();
				int i = 0;
				while (i < locMessage.length() && "\n\r".indexOf(locMessage.charAt(i)) == -1) { //$NON-NLS-1$
					i++;
				}
				statusMessage(true, locMessage.substring(0, i)); // only take first line
				return false;
			}
			statusMessage(false, ""); //$NON-NLS-1$
		} else {
			statusMessage(false, SearchMessages.SearchPage_containingText_hint);
		}
		return true;
	}

	private void addTextPatternControls(Composite group) {
		// grid layout with 2 columns

		// Info text
		Label label = new Label(group, SWT.LEAD);
		label.setText(SearchMessages.SearchPage_containingText_text);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		label.setFont(group.getFont());

		// Pattern combo
		fPattern = new Combo(group, SWT.SINGLE | SWT.BORDER);
		// Not done here to prevent page from resizing
		// fPattern.setItems(getPreviousSearchPatterns());
		fPattern.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleWidgetSelected();
				updateOKStatus();
			}
		});
		// add some listeners for regex syntax checking
		fPattern.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateOKStatus();
			}
		});
		fPattern.setFont(group.getFont());
		GridData data = new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1);
		data.widthHint = convertWidthInCharsToPixels(50);
		fPattern.setLayoutData(data);

		// Text line which explains the special characters
		fStatusLabel = new CLabel(group, SWT.LEAD);
		fStatusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		fStatusLabel.setFont(group.getFont());
		fStatusLabel.setAlignment(SWT.LEFT);
		fStatusLabel.setText(SearchMessages.SearchPage_containingText_hint);

		ComboContentAdapter contentAdapter = new ComboContentAdapter();
		FindReplaceDocumentAdapterContentProposalProvider findProposer = new FindReplaceDocumentAdapterContentProposalProvider(
				true);
		fPatterFieldContentAssist = new ContentAssistCommandAdapter(fPattern, contentAdapter,
				findProposer, ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, new char[] {
						'\\', '[', '(' }, true);
		fPatterFieldContentAssist.setEnabled(IS_REGEX_SEARCH);
	}

	private void handleWidgetSelected() {
		int selectionIndex = fPattern.getSelectionIndex();
		if (selectionIndex < 0 || selectionIndex >= fPreviousSearchPatterns.size()) {
			return;
		}

		SearchPatternData patternData = fPreviousSearchPatterns.get(selectionIndex);
		if (!fPattern.getText().equals(patternData.textPattern)) {
			return;
		}

		fPattern.setText(patternData.textPattern);
		fFileTypeEditor.setFileTypes(patternData.fileNamePatterns);
		if (patternData.workingSets != null) {
			getContainer().setSelectedWorkingSets(patternData.workingSets);
		} else {
			getContainer().setSelectedScope(patternData.scope);
		}
	}

	private boolean initializePatternControl() {
		ISelection selection = getSelection();
		if (selection instanceof ITextSelection && !selection.isEmpty()) {
			String text = ((ITextSelection) selection).getText();
			if (text != null) {
				if (IS_REGEX_SEARCH) {
					fPattern.setText(escapeForRegExPattern(text));
				} else {
					fPattern.setText(insertEscapeChars(text));
				}

				if (getPreviousExtensions().length > 0) {
					fExtensions.setText(getPreviousExtensions()[0]);
				} else {
					String extension = getExtensionFromEditor();
					if (extension != null) {
						fExtensions.setText(extension);
					} else {
						fExtensions.setText("*"); //$NON-NLS-1$
					}
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * TODO this is a temporary copy from FindReplaceDocumentAdapter (Eclipse 3.5) we have to keep
	 * it as long as we want to be Eclipse 3.4 compatible
	 *
	 * Escapes special characters in the string, such that the resulting pattern matches the given
	 * string.
	 *
	 * @param string
	 *            the string to escape
	 * @return a regex pattern that matches the given string
	 * @since 3.5
	 */
	private static String escapeForRegExPattern(String string) {
		// implements https://bugs.eclipse.org/bugs/show_bug.cgi?id=44422

		StringBuffer pattern = new StringBuffer(string.length() + 16);
		int length = string.length();
		if (length > 0 && string.charAt(0) == '^') {
			pattern.append('\\');
		}
		for (int i = 0; i < length; i++) {
			char ch = string.charAt(i);
			switch (ch) {
			case '\\':
			case '(':
			case ')':
			case '[':
			case ']':
			case '{':
			case '}':
			case '.':
			case '?':
			case '*':
			case '+':
			case '|':
				pattern.append('\\').append(ch);
				break;

			case '\r':
				if (i + 1 < length && string.charAt(i + 1) == '\n') {
					i++;
				}
				pattern.append("\\R"); //$NON-NLS-1$
				break;
			case '\n':
				pattern.append("\\R"); //$NON-NLS-1$
				break;
			case '\t':
				pattern.append("\\t"); //$NON-NLS-1$
				break;
			case '\f':
				pattern.append("\\f"); //$NON-NLS-1$
				break;
			case 0x07:
				pattern.append("\\a"); //$NON-NLS-1$
				break;
			case 0x1B:
				pattern.append("\\e"); //$NON-NLS-1$
				break;

			default:
				if (0 <= ch && ch < 0x20) {
					pattern.append("\\x"); //$NON-NLS-1$
					pattern.append(Integer.toHexString(ch).toUpperCase());
				} else {
					pattern.append(ch);
				}
			}
		}
		if (length > 0 && string.charAt(length - 1) == '$') {
			pattern.insert(pattern.length() - 1, '\\');
		}
		return pattern.toString();
	}

	private String insertEscapeChars(String text) {
		if (StringUtils.isEmpty(text)) {
			return ""; //$NON-NLS-1$
		}
		StringBuffer sbIn = new StringBuffer(text);
		BufferedReader reader = new BufferedReader(new StringReader(text));
		int lengthOfFirstLine = 0;
		try {
			lengthOfFirstLine = reader.readLine().length();
		} catch (IOException ex) {
			return ""; //$NON-NLS-1$
		}
		StringBuffer sbOut = new StringBuffer(lengthOfFirstLine + 5);
		int i = 0;
		while (i < lengthOfFirstLine) {
			char ch = sbIn.charAt(i);
			if (ch == '*' || ch == '?' || ch == '\\') {
				sbOut.append("\\"); //$NON-NLS-1$
			}
			sbOut.append(ch);
			i++;
		}
		return sbOut.toString();
	}

	private String getExtensionFromEditor() {
		IEditorPart ep = SearchPlugin.getActivePage().getActiveEditor();
		if (ep != null) {
			Object elem = ep.getEditorInput();
			if (elem instanceof IFileEditorInput) {
				String extension = ((IFileEditorInput) elem).getFile().getFileExtension();
				if (extension == null) {
					return ((IFileEditorInput) elem).getFile().getName();
				}
				return "*." + extension; //$NON-NLS-1$
			}
		}
		return null;
	}

	private void addFileNameControls(Composite group) {
		// grid layout with 2 columns

		// Line with label, combo and button
		Label label = new Label(group, SWT.LEAD);
		label.setText(SearchMessages.SearchPage_fileNamePatterns_text);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		label.setFont(group.getFont());

		fExtensions = new Combo(group, SWT.SINGLE | SWT.BORDER);
		fExtensions.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateOKStatus();
			}
		});
		GridData data = new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1);
		data.widthHint = convertWidthInCharsToPixels(50);
		fExtensions.setLayoutData(data);
		fExtensions.setFont(group.getFont());

		Button button = new Button(group, SWT.PUSH);
		button.setText(SearchMessages.SearchPage_browse);
		GridData gridData = new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 1, 1);
		gridData.widthHint = SWTUtil.getButtonWidthHint(button);
		button.setLayoutData(gridData);
		button.setFont(group.getFont());

		fFileTypeEditor = new FileTypeEditor(fExtensions, button);

		// Text line which explains the special characters
		Label description = new Label(group, SWT.LEAD);
		description.setText(SearchMessages.SearchPage_fileNamePatterns_hint);
		description.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		description.setFont(group.getFont());
	}

	/**
	 * Sets the search page's container.
	 *
	 * @param container
	 *            the container to set
	 */
	public void setContainer(ISearchPageContainer container) {
		fContainer = container;
	}

	private ISearchPageContainer getContainer() {
		return fContainer;
	}

	private ISelection getSelection() {
		return fContainer.getSelection();
	}

	// --------------- Configuration handling --------------

	@Override
	public void dispose() {
		writeConfiguration();
		super.dispose();
	}

	/**
	 * Returns the page settings for this Text search page.
	 *
	 * @return the page settings to be used
	 */
	private IDialogSettings getDialogSettings() {
		return SearchPlugin.getDefault().getDialogSettingsSection(PAGE_NAME);
	}

	/**
	 * Initializes itself from the stored page settings.
	 */
	private void readConfiguration() {
		IDialogSettings s = getDialogSettings();
		try {
			int historySize = s.getInt(STORE_HISTORY_SIZE);
			for (int i = 0; i < historySize; i++) {
				IDialogSettings histSettings = s.getSection(STORE_HISTORY + i);
				if (histSettings != null) {
					SearchPatternData data = SearchPatternData.create(histSettings);
					if (data != null) {
						fPreviousSearchPatterns.add(data);
					}
				}
			}
		} catch (NumberFormatException e) {
			// ignore
		}
	}

	/**
	 * Stores it current configuration in the dialog store.
	 */
	private void writeConfiguration() {
		IDialogSettings s = getDialogSettings();

		int historySize = Math.min(fPreviousSearchPatterns.size(), HISTORY_SIZE);
		s.put(STORE_HISTORY_SIZE, historySize);
		for (int i = 0; i < historySize; i++) {
			IDialogSettings histSettings = s.addNewSection(STORE_HISTORY + i);
			SearchPatternData data = fPreviousSearchPatterns.get(i);
			data.store(histSettings);
		}
	}

	private void statusMessage(boolean error, String message) {
		fStatusLabel.setText(message);
		if (error) {
			fStatusLabel.setForeground(JFaceColors.getErrorText(fStatusLabel.getDisplay()));
		} else {
			fStatusLabel.setForeground(null);
		}
	}

}
