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
package com.vectrace.MercurialEclipse.commands.extensions.mq;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Patch;

/**
 * @author bastian
 *
 */
public class HgQSeriesClient extends AbstractClient {
	public static List<Patch> getPatchesInSeries(IResource resource)
			throws HgException {
		AbstractShellCommand command = new HgCommand("qseries", //$NON-NLS-1$
				"Invoking qseries", resource, true);

		command.addOptions("--config", "extensions.hgext.mq="); //$NON-NLS-1$ //$NON-NLS-2$

		command.addOptions("-v"); //$NON-NLS-1$
		command.addOptions("--summary"); //$NON-NLS-1$
		return parse(command.executeToString());
	}

	/**
	 * @param executeToString
	 * @return
	 */
	public static List<Patch> parse(String executeToString) {
		List<Patch> list = new ArrayList<Patch>();
		if (executeToString != null && executeToString.indexOf("\n") >= 0) { //$NON-NLS-1$
			String[] patches = executeToString.split("\n"); //$NON-NLS-1$
			for (String string : patches) {
                String[] components = string.split(":", 2); //$NON-NLS-1$
                String[] patchData = components[0].trim().split(" ", 3); //$NON-NLS-1$

				Patch p = new Patch();
				p.setIndex(getInt(patchData[0], -1));
				p.setApplied("A".equals(patchData[1])); //$NON-NLS-1$
				p.setName(patchData[2].trim());

				if (components.length>1) {
					String summary = components[1].trim();
					p.setSummary(summary);
				}

				list.add(p);
			}
		}
		return list;
	}

	private static int getInt(String s, int def) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return def;
		}
	}

	public static List<Patch> getPatchesNotInSeries(IResource resource)
			throws HgException {
		AbstractShellCommand command = new HgCommand("qseries", //$NON-NLS-1$
				"Invoking qseries", resource, true);
		command.addOptions("--config", "extensions.hgext.mq="); //$NON-NLS-1$ //$NON-NLS-2$

		command.addOptions("--summary", "--missing"); //$NON-NLS-1$ //$NON-NLS-2$
		return parse(command.executeToString());
	}
}
