/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Administrator            - implementation
 *     Andrei Loskutov          - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ResourceListSelectionDialog;
import org.eclipse.ui.dialogs.SaveAsDialog;

import com.vectrace.MercurialEclipse.utils.ClipboardUtils;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;
import com.vectrace.MercurialEclipse.wizards.Messages;

/**
 * control for choose location: clipboard, file or workspace file
 *
 * @author Administrator
 *
 */
public class LocationChooser extends Composite implements Listener {

	public enum LocationType {
		Clipboard, FileSystem, Workspace
	}

	private Button btnClipboard;

	private Button btnFilesystem;
	private Text txtSystemFile;
	private Button btnBrowseFileSystem;

	private Button btnWorkspace;
	private Text txtWorkspaceFile;
	private Button btnBrowseWorkspace;

	private final boolean save;

	private final ListenerList stateListeners = new ListenerList();

	private final IDialogSettings settings;

	// constructors

	public LocationChooser(Composite parent, boolean save, IDialogSettings settings) {
		this(parent, save, settings, null);
	}

	public LocationChooser(Composite parent, boolean save, IDialogSettings settings,
			String defaultFileName) {
		super(parent, SWT.None);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		setLayout(layout);
		createLocationControl();
		this.save = save;
		this.settings = settings;
		restoreSettings(defaultFileName);
	}

	// operations

	protected void createLocationControl() {
		btnClipboard = SWTWidgetHelper.createRadioButton(this, Messages
				.getString("ExportPatchWizard.Clipboard"), 3); //$NON-NLS-1$
		btnClipboard.addListener(SWT.Selection, this);

		btnFilesystem = SWTWidgetHelper.createRadioButton(this, Messages
				.getString("ExportPatchWizard.FileSystem"), //$NON-NLS-1$
				1);
		btnFilesystem.addListener(SWT.Selection, this);
		txtSystemFile = SWTWidgetHelper.createTextField(this);
		txtSystemFile.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fireStateChanged();
			}
		});
		txtSystemFile.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				((Text) e.getSource()).selectAll();
			}
		});
		btnBrowseFileSystem = SWTWidgetHelper.createPushButton(this, "...", 1); //$NON-NLS-1$
		btnBrowseFileSystem.addListener(SWT.Selection, this);

		btnWorkspace = SWTWidgetHelper.createRadioButton(this, Messages
				.getString("ExportPatchWizard.Workspace"), 1); //$NON-NLS-1$
		btnWorkspace.addListener(SWT.Selection, this);
		txtWorkspaceFile = SWTWidgetHelper.createTextField(this);
		txtWorkspaceFile.setEditable(false);
		btnBrowseWorkspace = SWTWidgetHelper.createPushButton(this, "...", 1); //$NON-NLS-1$
		btnBrowseWorkspace.addListener(SWT.Selection, this);
	}

	public void handleEvent(Event event) {
		if (event.widget == btnBrowseFileSystem) {
			FileDialog dialog = new FileDialog(getDisplay().getActiveShell(),
					SWT.PRIMARY_MODAL | (save ? SWT.SAVE : SWT.OPEN));
			// dialog.setText("Choose file to save");
			dialog.setFileName(txtSystemFile.getText());
			String file = dialog.open();
			if (file != null) {
				txtSystemFile.setText(new Path(file).toOSString());
			}
		} else if (event.widget == btnBrowseWorkspace) {
			if (save) {
				SaveAsDialog dialog = new SaveAsDialog(getDisplay()
						.getActiveShell());
				IFile file = getWorkspaceFile();

				if (file != null) {
					dialog.setOriginalFile(file);
				} else {
					dialog.setOriginalName(txtWorkspaceFile.getText());
				}

				// dialog.setText(txtWorkspaceFile.getText());
				// dialog.setTitle(getTitle());
				if (dialog.open() == Window.OK) {
					txtWorkspaceFile.setText(dialog.getResult().toString());
				}
			} else {
				// no folder
				// OpenResourceDialog dialog = new
				// OpenResourceDialog(getShell(),ResourcesPlugin.getWorkspace().getRoot(),IResource.FILE);
				// multi
				// ResourceSelectionDialog dialog = new
				// ResourceSelectionDialog(getShell(),
				// ResourcesPlugin.getWorkspace().getRoot(),null);

				ResourceListSelectionDialog dialog = new ResourceListSelectionDialog(
						getShell(), ResourcesPlugin.getWorkspace().getRoot(),
						IResource.FILE);
				List<String> list = new ArrayList<String>(1);
				list.add(txtWorkspaceFile.getText());
				dialog.setInitialElementSelections(list);
				dialog.open();
				Object[] result = dialog.getResult();
				if (result != null && result.length > 0) {
					txtWorkspaceFile.setText(((IFile) result[0]).getFullPath()
							.toPortableString());
				}
			}
		} else if (event.widget == btnClipboard
				|| event.widget == btnFilesystem
				|| event.widget == btnWorkspace) {
			updateBtnStatus();
		}
		fireStateChanged();
	}

	public String validate() {
		boolean valid = false;
		LocationType type = getLocationType();
		if (type == null) {
			return null;
		}
		switch (type) {
		case Workspace:
			// valid = isValidWorkSpaceLocation(getWorkspaceFile());
			// break;
		case FileSystem:
			valid = isValidSystemFile(getPatchFile());
			break;
		case Clipboard:
			return validateClipboard();
		}
		if (valid) {
			return null;
		}
		return Messages.getString("ExportPatchWizard.InvalidFileName"); //$NON-NLS-1$
	}

	private String validateClipboard() {
		if (save) {
			return null;
		}
		return ClipboardUtils.isEmpty() ? Messages
				.getString("LocationChooser.clipboardEmpty") : null; //$NON-NLS-1$
	}

	private boolean isValidSystemFile(File file) {
		if (file == null || file.getPath().length() == 0) {
			return false;
		}
		if (!file.isAbsolute()) {
			return false;
		}
		if (file.isDirectory()) {
			return false;
		}
		if (save) {
			File parent = file.getParentFile();
			if (parent == null) {
				return false;
			}
			if (!parent.exists()) {
				return false;
			}
			if (!parent.isDirectory()) {
				return false;
			}
		} else {
			if (!file.exists()) {
				return false;
			}
		}
		return true;
	}

	public File getPatchFile() {
		switch (getLocationType()) {
		case FileSystem:
			return btnFilesystem.getSelection() ? new File(txtSystemFile
					.getText()) : null;
		case Clipboard:
			return null;
		case Workspace:
			IFile file = getWorkspaceFile();
			return file == null ? null : ResourceUtils.getFileHandle(file);
		default:
			return null;
		}
	}

	private IFile getWorkspaceFile() {
		if (!btnWorkspace.getSelection() || txtWorkspaceFile.getText() == null
				|| txtWorkspaceFile.getText().length() == 0) {
			return null;
		}

		try {
			IPath parentToWorkspace = new Path(txtWorkspaceFile.getText());
			return ResourcesPlugin.getWorkspace().getRoot().getFile(
					parentToWorkspace);
		} catch (Throwable e) {
			// Invalid path
			return null;
		}
	}

	// private boolean isValidWorkSpaceLocation(IFile file) {
	// if (save)
	// return file != null && file.getParent().exists();
	// return file != null && file.exists();
	// }

	public LocationType getLocationType() {
		if (btnClipboard.getSelection()) {
			return LocationType.Clipboard;
		} else if (btnFilesystem.getSelection()) {
			return LocationType.FileSystem;
		} else if (btnWorkspace.getSelection()) {
			return LocationType.Workspace;
		}
		return null;
	}

	private void updateBtnStatus() {
		LocationType type = getLocationType();
		txtSystemFile.setEnabled(type == LocationType.FileSystem);
		btnBrowseFileSystem.setEnabled(type == LocationType.FileSystem);
		txtWorkspaceFile.setEnabled(type == LocationType.Workspace);
		btnBrowseWorkspace.setEnabled(type == LocationType.Workspace);
	}

	public void addStateListener(Listener listener) {
		stateListeners.add(listener);
	}

	protected void fireStateChanged() {
		for (Object obj : stateListeners.getListeners()) {
			((Listener) obj).handleEvent(null);
		}
	}

	public Location getCheckedLocation() {
		return new Location(getLocationType(), getPatchFile(),
				getWorkspaceFile());
	}

	public static class Location {

		private final LocationType locationType;

		public LocationType getLocationType() {
			return locationType;
		}

		public File getFile() {
			return file;
		}

		public IFile getWorkspaceFile() {
			return workspaceFile;
		}

		private final File file;
		private final IFile workspaceFile;

		public Location(LocationType locationType, File file,
				IFile workspaceFile) {
			this.locationType = locationType;
			this.file = file;
			this.workspaceFile = workspaceFile;
		}

	}

	/**
	 * @param defaultFilename May be null
	 */
	protected void restoreSettings(String defaultFileName) {
		if (settings == null) {
			return;
		}

		String val;

		if ((val = settings.get("LocationType")) != null) {
			setLocationType(LocationType.valueOf(val));
		}

		if ((val = applyDefaultFileName(defaultFileName, settings.get("TxtSystemFile"))) != null) {
			txtSystemFile.setText(val);
		}

		if ((val = applyDefaultFileName(defaultFileName, settings.get("TxtWorkspaceFile"))) != null) {
			txtWorkspaceFile.setText(val);
		}
	}

	private static String applyDefaultFileName(String defaultFileName, String val) {
		if (val == null) {
			return defaultFileName;
		} else if (defaultFileName == null || defaultFileName.length() == 0) {
			return val;
		}

		int nEnd = Math.max(val.lastIndexOf('\\'), val.lastIndexOf('/'));

		if (nEnd >= 0) {
			return val.substring(0, nEnd + 1) + defaultFileName;
		}

		return defaultFileName;
	}

	public void saveSettings() {
		if (settings == null) {
			return;
		}

		settings.put("LocationType", getLocationType().name());
		settings.put("TxtSystemFile", txtSystemFile.getText());
		settings.put("TxtWorkspaceFile", txtWorkspaceFile.getText());
	}

	private void setLocationType(LocationType type) {
		switch (type) {
		case Clipboard:
			btnClipboard.setSelection(true);
			break;
		case FileSystem:
			btnFilesystem.setSelection(true);
			break;
		case Workspace:
			btnWorkspace.setSelection(true);
			break;
		}
		updateBtnStatus();
	}
}
