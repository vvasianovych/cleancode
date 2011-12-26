/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * zluspai	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.mylyn;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Platform;
import org.eclipse.mylyn.context.core.ContextCore;
import org.eclipse.mylyn.context.core.IInteractionContext;
import org.eclipse.mylyn.internal.resources.ui.ResourcesUiBridgePlugin;
import org.eclipse.mylyn.internal.team.ui.ContextChangeSet;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.ui.TasksUi;

/**
 * The real mylyn facade class wraps all mylyn functionality accessed from this plugin.
 *
 * @author zluspai
 */
@SuppressWarnings("restriction")
class MylynFacadeImpl implements IMylynFacade {

	static ITask getCurrentTask() {
		return TasksUi.getTaskActivityManager().getActiveTask();
	}

	static IInteractionContext getActiveContext() {
		return ContextCore.getContextManager().getActiveContext();
	}

	/**
	 * Get comment for the current mylyn task.
	 * @param resources
	 * @return The comment created by mylyn from the task-template
	 */
	public String getCurrentTaskComment(IResource[] resources) {
		if (resources == null) {
			return null;
		}
		ITask task = getCurrentTask();
		if (task == null) {
			return null;
		}
		boolean checkTaskRepository = true;
		String comment = ContextChangeSet.getComment(checkTaskRepository, task, resources);
		return comment;
	}

	/**
	 * Get the resources for the current task, which is the mylyn context
	 * @return The resources, or null if no current task present
	 */
	public IResource[] getCurrentTaskResources() {
		ITask task = getCurrentTask();
		if (task == null) {
			return null;
		}

		if (Platform.isRunning() && ResourcesUiBridgePlugin.getDefault() != null && task.isActive()) {
			IInteractionContext context = getActiveContext();
			if (context == null) {
				return null;
			}
			List<IResource> resources = ResourcesUiBridgePlugin.getDefault().getInterestingResources(context);
			if (resources == null) {
				return null;
			}
			return resources.toArray(new IResource[0]);
		}
		return null;
	}

}
