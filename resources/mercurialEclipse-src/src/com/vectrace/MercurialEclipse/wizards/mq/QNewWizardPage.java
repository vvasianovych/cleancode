/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian       implementation
 * Philip Graf   bug fix
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards.mq;

import static com.vectrace.MercurialEclipse.ui.SWTWidgetHelper.*;

import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;

import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.storage.HgCommitMessageManager;
import com.vectrace.MercurialEclipse.ui.CommitFilesChooser;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;
import com.vectrace.MercurialEclipse.wizards.HgWizardPage;

/**
 * @author bastian
 *
 */
public class QNewWizardPage extends HgWizardPage {

	protected final HgRoot root;
	private Text patchNameTextField;
	private Text userTextField;
	private Text date;
	private final boolean showPatchName;
	private SourceViewer commitTextBox;
	private SourceViewerDecorationSupport decorationSupport;
	private IDocument commitTextDocument;
	private CommitFilesChooser fileChooser;

	public QNewWizardPage(String pageName, String title,
			ImageDescriptor titleImage, String description,
			HgRoot root, boolean showPatchName) {
		super(pageName, title, titleImage, description);

		Assert.isNotNull(root);
		this.root = root;
		this.showPatchName = showPatchName;
		this.commitTextDocument = new Document();
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite composite = createComposite(parent, 1);
		//Group g = createGroup(composite, Messages.getString("QNewWizardPage.patchDataGroup.title")); //$NON-NLS-1$
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.minimumHeight = 150;
		composite.setLayoutData(data);
		if (showPatchName) {
			createLabel(composite, Messages.getString("QNewWizardPage.patchNameLabel.title")); //$NON-NLS-1$
			this.patchNameTextField = createTextField(composite);
		}

		createLabel(composite, Messages.getString("QNewWizardPage.userNameLabel.title")); //$NON-NLS-1$
		userTextField = createTextField(composite);
		userTextField.setText(HgCommitMessageManager.getDefaultCommitName(root));

		createLabel(composite, Messages.getString("QNewWizardPage.dateLabel.title")); //$NON-NLS-1$
		date = createTextField(composite);

		createLabel(composite, Messages
				.getString("QNewWizardPage.commitMessageLabel.title")); //$NON-NLS-1$
		commitTextBox = new SourceViewer(composite, null, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.WRAP);
		commitTextBox.getTextWidget().setLayoutData(data);

		// set up spell-check annotations
		decorationSupport = new SourceViewerDecorationSupport(commitTextBox,
				null, new DefaultMarkerAnnotationAccess(), EditorsUI
						.getSharedTextColors());

		AnnotationPreference pref = EditorsUI.getAnnotationPreferenceLookup()
				.getAnnotationPreference(SpellingAnnotation.TYPE);

		decorationSupport.setAnnotationPreference(pref);
		decorationSupport.install(EditorsUI.getPreferenceStore());

		commitTextBox.configure(new TextSourceViewerConfiguration(EditorsUI
				.getPreferenceStore()));
		AnnotationModel annotationModel = new AnnotationModel();
		commitTextBox.setDocument(commitTextDocument, annotationModel);
		commitTextBox.getTextWidget().addDisposeListener(new DisposeListener() {

			public void widgetDisposed(DisposeEvent e) {
				decorationSupport.uninstall();
			}
		});

		Group g = SWTWidgetHelper.createGroup(composite, "Add changes to patch:", 1, GridData.FILL_BOTH); //$NON-NLS-1$
		// TODO: Resource calculation wrong for repos below root
		fileChooser = new CommitFilesChooser(g, true, new ArrayList<IResource>(ResourceUtils
				.getProjects(root)), true, true, false);

		setControl(composite);
	}

	/**
	 * @return the patchNameTextField
	 */
	public Text getPatchNameTextField() {
		return patchNameTextField;
	}

	/**
	 * @param patchNameTextField
	 *            the patchNameTextField to set
	 */
	public void setPatchNameTextField(Text patchNameTextField) {
		this.patchNameTextField = patchNameTextField;
	}

	/**
	 * @return the date
	 */
	public Text getDate() {
		return date;
	}

	/**
	 * @param date
	 *            the date to set
	 */
	public void setDate(Text date) {
		this.date = date;
	}

	/**
	 * @return the userTextField
	 */
	public Text getUserTextField() {
		return userTextField;
	}

	/**
	 * @param userTextField
	 *            the userTextField to set
	 */
	public void setUserTextField(Text userTextField) {
		this.userTextField = userTextField;
	}

	/**
	 * @return the commitTextDocument
	 */
	public IDocument getCommitTextDocument() {
		return commitTextDocument;
	}

	/**
	 * @param commitTextDocument
	 *            the commitTextDocument to set
	 */
	public void setCommitTextDocument(IDocument commitTextDocument) {
		this.commitTextDocument = commitTextDocument;
	}

	public CommitFilesChooser getFileChooser()
	{
		return fileChooser;
	}
}
