/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      bastian	implementation
 *      Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.OutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author bastian
 *
 */
public class HgServeClient extends AbstractClient {

	/**
	 * Rule which forbids running server on same port
	 */
	private static final class ServeRule implements ISchedulingRule {

		final int port;

		private ServeRule(int port) {
			super();
			this.port = port;
		}

		public boolean isConflicting(ISchedulingRule rule) {
			return contains(rule);
		}

		public boolean contains(ISchedulingRule rule) {
			if(this == rule){
				return true;
			}
			if(!(rule instanceof ServeRule)){
				return false;
			}
			return this.port == ((ServeRule) rule).port;
		}

	}

	static class HgServeJob extends Job {
		private final HgRoot hgRoot;
		private final boolean ipv6;
		private final String name;
		private final String prefix;
		private final int port;
		private final String webdirConf;
		private final boolean stdio;
		private final IProgressMonitor progress;

		public HgServeJob(HgRoot hgRoot, int port, String prefix,
				String name, String webdirConf, boolean ipv6, boolean stdio) {
			super(Messages.getString("HgServeClient.serveJob.name") //$NON-NLS-1$
					+ hgRoot.getName() + "..."); //$NON-NLS-1$
			this.hgRoot = hgRoot;
			this.port = port;
			this.prefix = prefix;
			this.name = name;
			this.webdirConf = webdirConf;
			this.ipv6 = ipv6;
			this.stdio = stdio;
			progress = getJobManager().createProgressGroup();
			progress.beginTask("Local Mercurial Server on port " + port, IProgressMonitor.UNKNOWN);
			setProgressGroup(progress, IProgressMonitor.UNKNOWN);
			setPriority(LONG);
			setUser(false);
			setSystem(false);
			setRule(new ServeRule(port));
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final AbstractShellCommand command = getCommand(progress, hgRoot, port, prefix,
					name, webdirConf, stdio, ipv6);
			try {
				command.executeToBytes(Integer.MAX_VALUE, false);
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
			} finally {
				command.terminate();
				progress.done();
				monitor.done();
			}
			return Status.OK_STATUS;
		}

		@Override
		public boolean belongsTo(Object family) {
			return HgServeClient.class == family;
		}
	}

	public static boolean serve(HgRoot hgRoot, int port, String prefixPath,
			String name, String webdirConf, boolean stdio, boolean ipv6) {

		Job[] jobs = Job.getJobManager().find(HgServeClient.class);
		for (Job job : jobs) {
			ServeRule rule = (ServeRule) job.getRule();
			if(rule != null && rule.port == port && job.getState() == Job.RUNNING){
				return false;
			}
		}

		new HgServeJob(hgRoot, port, prefixPath, name, webdirConf, ipv6, stdio)
				.schedule();
		return true;
	}

	private static AbstractShellCommand getCommand(final IProgressMonitor progress, HgRoot hgRoot,
			final int port, String prefixPath, String name, String webdirConf, boolean stdio,
			boolean ipv6) {

		final AbstractShellCommand command = new HgCommand("serve", "Serving repository", hgRoot, true){
			@Override
			protected ProzessWrapper createProcessWrapper(OutputStream output, String jobName,
					ProcessBuilder builder) {
				ProzessWrapper wrapper = super.createProcessWrapper(output, jobName, builder);
				wrapper.setProgressGroup(progress, IProgressMonitor.UNKNOWN);
				wrapper.setPriority(Job.LONG);
				wrapper.setUser(false);
				wrapper.setSystem(true);
				return wrapper;
			}
		};

		command.setExecutionRule(new AbstractShellCommand.DefaultExecutionRule());

		if (port != 8000) {
			command.addOptions("--port", String.valueOf(port)); //$NON-NLS-1$
		}
		if (prefixPath != null && prefixPath.length() > 0) {
			command.addOptions("--prefix", prefixPath); //$NON-NLS-1$
		}
		if (name != null && name.length() > 0) {
			command.addOptions("--name", name); //$NON-NLS-1$
		}
		if (webdirConf != null && webdirConf.length() > 0) {
			command.addOptions("--webdir-conf", webdirConf); //$NON-NLS-1$
		}
		if (stdio) {
			command.addOptions("--stdio"); //$NON-NLS-1$
		}
		if (ipv6) {
			command.addOptions("--ipv6"); //$NON-NLS-1$
		}
		// start daemon
		return command;
	}

}
