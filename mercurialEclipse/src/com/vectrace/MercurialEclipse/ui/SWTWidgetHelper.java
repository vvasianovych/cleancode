/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FontFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;

import com.vectrace.MercurialEclipse.model.ChangeSet;

/**
 * @author bastian
 *
 */
public final class SWTWidgetHelper {
	public static final int LABEL_WIDTH_HINT = 400;
	public static final int LABEL_INDENT_WIDTH = 32;
	public static final int LIST_HEIGHT_HINT = 100;
	public static final int SPACER_HEIGHT = 8;

	private SWTWidgetHelper() {
		// hide constructor of utility class.
	}

	/**
	 * Creates a new checkbox instance and sets the default layout data.
	 *
	 * @param group
	 *            the composite in which to create the checkbox
	 * @param label
	 *            the string to set into the checkbox
	 * @return the new checkbox
	 */
	public static Button createCheckBox(Composite group, String label) {
		Button button = new Button(group, SWT.CHECK | SWT.LEFT);
		button.setText(label);
		GridData data = new GridData();
		data.horizontalSpan = 2;
		button.setLayoutData(data);
		return button;
	}

	/**
	 * @return
	 */
	public static GridData getFillGD(int minHeight) {
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.minimumHeight = minHeight;
		return gd;
	}

	public static Button createPushButton(Composite parent, String label, int span) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(label);
		GridData data = new GridData();
		data.horizontalSpan = span;
		button.setLayoutData(data);
		return button;
	}

	public static ColorFieldEditor createColorFieldEditor(String preferenceName, String label,
			Composite parent, DialogPage page, IPreferenceStore preferenceStore) {
		ColorFieldEditor editor = new ColorFieldEditor(preferenceName, label, parent);
		editor.setPage(page);
		editor.setPreferenceStore(preferenceStore);
		return editor;
	}

	/**
	 * Utility method that creates a combo box
	 *
	 * @param parent
	 *            the parent for the new label
	 * @return the new widget
	 */
	public static Combo createCombo(Composite parent) {
		Combo combo = new Combo(parent, SWT.READ_ONLY);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		combo.setLayoutData(data);
		return combo;
	}

	/**
	 * Utility method that creates an editable combo box
	 *
	 * @param parent
	 *            the parent for the new label
	 * @return the new widget
	 */
	public static Combo createEditableCombo(Composite parent) {
		Combo combo = new Combo(parent, SWT.NULL);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		combo.setLayoutData(data);
		return combo;
	}

	/**
	 * Creates composite control and sets the default layout data.
	 *
	 * @param parent
	 *            the parent of the new composite
	 * @param numColumns
	 *            the number of columns for the new composite
	 * @return the newly-created coposite
	 */
	public static Composite createComposite(Composite parent, int numColumns) {
		Composite composite = new Composite(parent, SWT.NULL);

		// GridLayout
		GridLayout layout = new GridLayout();
		layout.numColumns = numColumns;
		composite.setLayout(layout);

		// GridData
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.verticalAlignment = SWT.FILL;
		data.horizontalAlignment = SWT.FILL;
		composite.setLayoutData(data);
		return composite;
	}

	/**
	 * Utility method that creates a label instance and sets the default layout data.
	 *
	 * @param parent
	 *            the parent for the new label
	 * @param text
	 *            the text for the new label
	 * @return the new label
	 */
	public static Label createLabel(Composite parent, String text) {
		return createIndentedLabel(parent, text, 0);
	}

	/**
	 * Utility method that creates a label instance indented by the specified number of pixels and
	 * sets the default layout data.
	 *
	 * @param parent
	 *            the parent for the new label
	 * @param text
	 *            the text for the new label
	 * @param indent
	 *            the indent in pixels, or 0 for none
	 * @return the new label
	 */
	public static Label createIndentedLabel(Composite parent, String text, int indent) {
		Label label = new Label(parent, SWT.LEFT);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalSpan = 1;
		data.horizontalAlignment = GridData.FILL;
		data.horizontalIndent = indent;
		label.setLayoutData(data);
		return label;
	}

	/**
	 * Utility method that creates a label instance with word wrap and sets the default layout data.
	 *
	 * @param parent
	 *            the parent for the new label
	 * @param text
	 *            the text for the new label
	 * @param indent
	 *            the indent in pixels, or 0 for none
	 * @param widthHint
	 *            the nominal width of the label
	 * @return the new label
	 */
	public static Label createWrappingLabel(Composite parent, String text, int indent) {
		return createWrappingLabel(parent, text, indent, 1);
	}

	public static Label createWrappingLabel(Composite parent, String text, int indent,
			int horizontalSpan) {
		Label label = new Label(parent, SWT.LEFT | SWT.WRAP);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalSpan = horizontalSpan;
		data.horizontalAlignment = GridData.FILL;
		data.horizontalIndent = indent;
		data.grabExcessHorizontalSpace = true;
		data.widthHint = SWTWidgetHelper.LABEL_WIDTH_HINT;
		label.setLayoutData(data);
		return label;
	}

	/**
	 * Create a text field specific for this application
	 *
	 * @param parent
	 *            the parent of the new text field
	 * @return the new text field
	 */
	public static Text createTextField(Composite parent) {
		Text text = new Text(parent, SWT.SINGLE | SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.verticalAlignment = GridData.CENTER;
		data.grabExcessVerticalSpace = false;
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		text.setLayoutData(data);
		return text;
	}

	/**
	 * Create a password entry field specific for this application
	 *
	 * @param parent
	 *            the parent of the new text field
	 * @return the new password field
	 */
	public static Text createPasswordField(Composite parent) {
		Text text = new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.verticalAlignment = GridData.CENTER;
		data.grabExcessVerticalSpace = false;
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		text.setLayoutData(data);
		return text;
	}

	/**
	 * Utility method to create a radio button
	 *
	 * @param parent
	 *            the parent of the radio button
	 * @param label
	 *            the label of the radio button
	 * @param span
	 *            the number of columns to span
	 * @return the created radio button
	 */
	public static Button createRadioButton(Composite parent, String label, int span) {
		Button button = new Button(parent, SWT.RADIO);
		button.setText(label);
		GridData data = new GridData();
		data.horizontalSpan = span;
		button.setLayoutData(data);
		return button;
	}

	/**
	 * Utility method to create a full width separator preceeded by a blank space
	 *
	 * @param parent
	 *            the parent of the separator
	 * @param verticalSpace
	 *            the vertical whitespace to insert before the label
	 */
	public static void createSeparator(Composite parent, int verticalSpace) {
		// space
		Label label = new Label(parent, SWT.NONE);
		GridData data = new GridData();
		data.heightHint = verticalSpace;
		label.setLayoutData(data);
		// separator
		label = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		data = new GridData(GridData.FILL_HORIZONTAL);
		label.setLayoutData(data);
	}

	/**
	 * Creates a ListViewer whose input is an array of IFiles.
	 *
	 * @param parent
	 *            the parent of the viewer
	 * @param title
	 *            the text for the title label
	 * @param heightHint
	 *            the nominal height of the list
	 * @return the created list viewer
	 */
	public static ListViewer createFileListViewer(Composite parent, String title, int heightHint) {
		createLabel(parent, title);
		ListViewer listViewer = new ListViewer(parent, SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL
				| SWT.BORDER);
		listViewer.setContentProvider(new IStructuredContentProvider() {
			public Object[] getElements(Object inputElement) {
				return (Object[]) inputElement;
			}

			public void dispose() {
			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		});
		listViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((IFile) element).getFullPath().toString();
			}
		});
		listViewer.setComparator(new org.eclipse.ui.model.WorkbenchViewerComparator());

		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = heightHint;
		listViewer.getList().setLayoutData(data);
		return listViewer;
	}

	/**
	 * Creates a ListViewer whose input is an array of ChangeSets.
	 *
	 * @param parent
	 *            the parent of the viewer
	 * @param title
	 *            the text for the title label
	 * @param heightHint
	 *            the nominal height of the list
	 * @return the created list viewer
	 */
	public static ListViewer createChangeSetListViewer(Composite parent, String title,
			int heightHint) {
		if (title != null) {
			createLabel(parent, title);
		}
		ListViewer listViewer = new ListViewer(parent, SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL
				| SWT.BORDER);
		listViewer.setContentProvider(new IStructuredContentProvider() {
			public Object[] getElements(Object inputElement) {
				return (Object[]) inputElement;
			}

			public void dispose() {
			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}

		});
		listViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				ChangeSet cs = (ChangeSet) element;
				return cs + "\t" + cs.getDateString() + "\t" + cs.getUser(); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});

		ViewerComparator comparator = new org.eclipse.ui.model.WorkbenchViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				return ((ChangeSet) e2).compareTo((ChangeSet) e1);
			}
		};

		listViewer.setComparator(comparator);

		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = heightHint;
		listViewer.getList().setLayoutData(data);
		listViewer.setUseHashlookup(true);
		return listViewer;
	}

	/**
	 * Creates a ListViewer
	 *
	 * @param parent
	 *            the parent of the viewer
	 * @param title
	 *            the text for the title label
	 * @param heightHint
	 *            the nominal height of the list
	 * @param the
	 *            label decorator
	 * @return the created list viewer
	 */
	public static ListViewer createListViewer(Composite parent, String title, int heightHint,
			IBaseLabelProvider labelProvider) {
		if (title != null) {
			createLabel(parent, title);
		}
		ListViewer listViewer = new ListViewer(parent, SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL
				| SWT.BORDER | SWT.MULTI);
		listViewer.setContentProvider(new IStructuredContentProvider() {
			public Object[] getElements(Object inputElement) {
				return (Object[]) inputElement;
			}

			public void dispose() {
			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}

		});
		listViewer.setLabelProvider(labelProvider);

		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = heightHint;
		listViewer.getList().setLayoutData(data);
		listViewer.setUseHashlookup(true);
		return listViewer;
	}

	public static Group createGroup(Composite parent, String text, int span, int style) {
		Group group = new Group(parent, SWT.NULL);
		group.setText(text);
		GridData data = new GridData(style);
		data.horizontalSpan = span;
		// data.widthHint = GROUP_WIDTH;

		group.setLayoutData(data);
		GridLayout layout = new GridLayout();
		layout.numColumns = span;
		group.setLayout(layout);
		return group;
	}

	public static Group createGroup(Composite parent, String text) {
		return createGroup(parent, text, GridData.FILL_HORIZONTAL);
	}

	public static Group createGroup(Composite parent, String text, int style) {
		return createGroup(parent, text, 2, style);
	}

	public static SourceViewer createTextArea(Composite container) {
		SourceViewer textBox = new SourceViewer(container, null, SWT.V_SCROLL | SWT.MULTI
				| SWT.BORDER | SWT.WRAP);
		textBox.setEditable(true);

		// set up spell-check annotations
		final SourceViewerDecorationSupport decorationSupport = new SourceViewerDecorationSupport(
				textBox, null, new DefaultMarkerAnnotationAccess(), EditorsUI.getSharedTextColors());

		AnnotationPreference pref = EditorsUI.getAnnotationPreferenceLookup()
				.getAnnotationPreference(SpellingAnnotation.TYPE);

		decorationSupport.setAnnotationPreference(pref);
		decorationSupport.install(EditorsUI.getPreferenceStore());

		textBox.configure(new TextSourceViewerConfiguration(EditorsUI.getPreferenceStore()));
		textBox.setDocument(new Document(), new AnnotationModel());
		textBox.getTextWidget().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				decorationSupport.uninstall();
			}
		});
		GridData data = new GridData(GridData.FILL_BOTH);
		// data.heightHint = heightHint;
		textBox.getControl().setLayoutData(data);
		return textBox;
	}

	/**
	 * @param prefHistoryMergeChangesetFont
	 * @param string
	 * @param g
	 * @param historyPreferencePage
	 * @param preferenceStore
	 * @return
	 */
	public static FieldEditor createFontFieldEditor(String pref, String label, Composite parent,
			DialogPage page, IPreferenceStore preferenceStore) {
		FontFieldEditor editor = new FontFieldEditor(pref, label, parent);
		editor.setPage(page);
		editor.setPreferenceStore(preferenceStore);
		return editor;
	}

	/*
	 * incompatible with 3.2 public static DateTime createDateTime(Composite c, int style) {
	 * DateTime dt = new DateTime(c, style); GridData data = new GridData(GridData.FILL_HORIZONTAL);
	 * data.verticalAlignment = GridData.CENTER; data.grabExcessVerticalSpace = false;
	 * data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH; dt.setLayoutData(data); return dt; }
	 */

}
