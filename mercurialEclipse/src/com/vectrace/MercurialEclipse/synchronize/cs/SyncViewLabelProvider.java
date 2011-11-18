/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.cs;

import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.internal.ui.mapping.ResourceModelLabelProvider;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileFromChangeSet;
import com.vectrace.MercurialEclipse.model.PathFromChangeSet;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.synchronize.cs.HgChangeSetContentProvider.FilteredPlaceholder;
import com.vectrace.MercurialEclipse.utils.StringUtils;

@SuppressWarnings("restriction")
public class SyncViewLabelProvider extends ResourceModelLabelProvider {

	@Override
	public Image getImage(Object element) {
		// we MUST call super, to get the nice in/outgoing decorations...
		return super.getImage(element);
	}

	@Override
	protected Image getDelegateImage(Object element) {
		Image image = null;
		if (element instanceof ChangeSet) {
			image = MercurialEclipsePlugin.getImage("elcl16/changeset_obj.gif");
		} else if (element instanceof ChangesetGroup){
			ChangesetGroup group = (ChangesetGroup) element;
			if(group.getDirection() == Direction.OUTGOING){
				image = MercurialEclipsePlugin.getImage("actions/commit.gif");
			} else {
				image = MercurialEclipsePlugin.getImage("actions/update.gif");
			}
		} else if(element instanceof FileFromChangeSet){
			FileFromChangeSet file = (FileFromChangeSet) element;
			if(file.getFile() != null){
				image = getDelegateLabelProvider().getImage(file.getFile());
			} else {
				image = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
			}
		} else if (element instanceof PathFromChangeSet) {
			PathFromChangeSet path = (PathFromChangeSet) element;
			if(path.isProjectClosed()) {
				image = PlatformUI.getWorkbench().getSharedImages().getImage(
						IDE.SharedImages.IMG_OBJ_PROJECT_CLOSED);
			} else {
			image = PlatformUI.getWorkbench().getSharedImages().getImage(
					ISharedImages.IMG_OBJ_FOLDER);
			}
		} else {
			try {
				image = super.getDelegateImage(element);
			} catch (NullPointerException npex) {
				// if element is invalid or not yet fully handled
				// NPE is possible
				MercurialEclipsePlugin.logError(npex);
			}
		}
		return image;
	}

	@Override
	protected Image decorateImage(Image base, Object element) {
		Image decoratedImage;
		if (element instanceof FileFromChangeSet) {
			FileFromChangeSet ffc = (FileFromChangeSet) element;
			int kind = ffc.getDiffKind();
			decoratedImage = getImageManager().getImage(base, kind);
		} else if (element instanceof ChangesetGroup){
			ChangesetGroup group = (ChangesetGroup) element;
			if(group.getDirection() == Direction.LOCAL){
				decoratedImage = getImageManager().getImage(base, Differencer.CHANGE);
			} else {
				decoratedImage = getImageManager().getImage(base, Differencer.NO_CHANGE);
			}
		} else if(element instanceof WorkingChangeSet) {
			WorkingChangeSet cs = (WorkingChangeSet) element;
			if(cs.isDefault()) {
				decoratedImage = getDefaultChangesetIcon();
			} else {
				decoratedImage = getWorkingChangesetIcon();
			}
		} else {
			decoratedImage = getImageManager().getImage(base, Differencer.NO_CHANGE);
		}
		return decoratedImage;
	}

	public static Image getDefaultChangesetIcon() {
		return MercurialEclipsePlugin.getImage("elcl16/uncommitted_cs.gif", "ovr/pinned_ovr.gif",
				IDecoration.TOP_RIGHT);
	}

	public static Image getWorkingChangesetIcon() {
		return MercurialEclipsePlugin.getImage("elcl16/uncommitted_cs.gif", "ovr/edited_ovr.gif",
				IDecoration.TOP_RIGHT);
	}

	@Override
	protected String getDelegateText(Object elementOrPath) {
		if(elementOrPath instanceof ChangeSet){
			ChangeSet cset = (ChangeSet) elementOrPath;
			StringBuilder sb = new StringBuilder();
			if(!(cset instanceof WorkingChangeSet)){
				sb.append(cset.getChangesetIndex());

				if (cset.isCurrent()) {
					sb.append('*');
				}

				sb.append(" [").append(cset.getAuthor()).append(']');
				sb.append(" (").append(cset.getAgeDate()).append(')');
			} else {
				sb.append(cset.getName());
				sb.append(" (").append(cset.getChangesetFiles().length).append(')');
			}
			if (!Branch.isDefault(cset.getBranch())) {
				sb.append(' ').append(cset.getBranch());
			}
			sb.append(':').append(' ').append(getShortComment(cset));
			return StringUtils.removeLineBreaks(sb.toString());
		}
		if(elementOrPath instanceof ChangesetGroup){
			ChangesetGroup group = (ChangesetGroup) elementOrPath;
			String name = group.getName();
			if(group.getChangesets().isEmpty()){
				return name + " (empty)";
			}
			if(group.getDirection() == Direction.LOCAL) {
				int files = 0;
				for (ChangeSet cs : group.getChangesets()) {
					files += cs.getChangesetFiles().length;
				}
				if(files == 0) {
					return name + " (empty)";
				}
				return name + " (" + files + ')';
			}
			return name + " (" + group.getChangesets().size() + ')';
		}
		if(elementOrPath instanceof FileFromChangeSet){
			FileFromChangeSet file = (FileFromChangeSet) elementOrPath;

			String delegateText;
			if(file.getFile() != null) {
				delegateText = super.getDelegateText(file.getFile());
				IProject project = file.getFile().getProject();
				if(!project.isOpen()) {
					delegateText += " (closed!)";
				}
			} else {
				delegateText = file.toString();
			}

			if(file.getCopySourceFile() != null) {
				delegateText += " (" + super.getDelegateText(file.getCopySourceFile()) + ")";
			}

			if(delegateText != null && delegateText.length() > 0){
				delegateText = " " + delegateText;
			}
			return delegateText;
		} else if (elementOrPath instanceof PathFromChangeSet) {
			return elementOrPath.toString();
		} else if (elementOrPath instanceof FilteredPlaceholder) {
			return elementOrPath.toString();
		}
		String delegateText = super.getDelegateText(elementOrPath);
		if(delegateText != null && delegateText.length() > 0){
			delegateText = " " + delegateText;
		}
		return delegateText;
	}

	/**
	 * TODO: The entire comment should be shown in a tool tip
	 */
	private String getShortComment(ChangeSet cset) {
		String comment = cset.getComment();
		if(comment.length() > 100){
			comment = comment.substring(0, 100) + "...";
		}
		return comment;
	}

	/**
	 * @see org.eclipse.team.ui.synchronize.AbstractSynchronizeLabelProvider#getFont(java.lang.Object)
	 */
	@Override
	public Font getFont(Object element) {
		if(element instanceof WorkingChangeSet) {
			WorkingChangeSet cs = (WorkingChangeSet) element;
			if(cs.isDefault()) {
				return JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
			}
		}
		return super.getFont(element);
	}
}
