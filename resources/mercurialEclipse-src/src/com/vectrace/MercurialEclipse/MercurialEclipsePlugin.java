/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Stefan Groschupf          - logError
 *     Jérôme Nègre              - some fixes
 *     Stefan C                  - Code cleanup
 *     Andrei Loskutov           - bug fixes
 *     Zsolt Koppany (Intland)
 *     Adam Berkes (Intland)     - default encoding
 *     Philip Graf               - proxy support
 *     Bastian Doetsch           - bug fixes and implementation
 *******************************************************************************/

package com.vectrace.MercurialEclipse;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.util.tracker.ServiceTracker;

import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.HgDebugInstallClient;
import com.vectrace.MercurialEclipse.commands.RootlessHgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.storage.HgCommitMessageManager;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocationManager;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.utils.StringUtils;
import com.vectrace.MercurialEclipse.views.console.HgConsoleHolder;

/**
 * The main plugin class to be used in the desktop.
 */
public class MercurialEclipsePlugin extends AbstractUIPlugin {

	public static final String ID = "com.vectrace.MercurialEclipse"; //$NON-NLS-1$

	public static final String ID_CHANGELOG_VIEW = "com.vectrace.MercurialEclipse.views.ChangeLogView"; //$NON-NLS-1$

	public static final String BUNDLE_FILE_PREFIX = "bundlefile"; //$NON-NLS-1$

	// The shared instance.
	private static MercurialEclipsePlugin plugin;

	private static final String HGENCODING;

	static {
		// next in line is HGENCODING in environment
		String enc = System.getProperty("HGENCODING");

		// next is platform encoding as available in JDK
		if (!StringUtils.isEmpty(enc) && Charset.isSupported(enc)) {
			HGENCODING = enc;
		} else {
			if(Charset.isSupported("UTF-8")){
				HGENCODING = Charset.forName("UTF-8").name();
			} else {
				HGENCODING = Charset.defaultCharset().name();
			}
		}
	}

	// the repository manager
	private static HgRepositoryLocationManager repoManager = new HgRepositoryLocationManager();

	// the commit message manager
	private static HgCommitMessageManager commitMessageManager = new HgCommitMessageManager();

	private boolean hgUsable = true;

	private ServiceTracker proxyServiceTracker;

	/** Observed hg version */
	public /*final*/ Version hgVersion = Version.emptyVersion;

	private static final Pattern VERSION_PATTERN = Pattern.compile(".*version\\s+(\\d(\\.\\d)+)+.*", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$


	public MercurialEclipsePlugin() {
		// should NOT do anything until started by OSGI
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		DefaultConfiguration cfg = new DefaultConfiguration();
		HgClients.initialize(cfg, cfg, cfg);
		proxyServiceTracker = new ServiceTracker(context, IProxyService.class.getName(), null);
		proxyServiceTracker.open();

		final Job job = new Job(Messages.getString("MercurialEclipsePlugin.startingMercurialEclipse")) { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask(Messages
							.getString("MercurialEclipsePlugin.startingMercurialEclipse"), 3); //$NON-NLS-1$
					monitor.subTask(Messages
							.getString("MercurialEclipsePlugin.checkingMercurialInstallation")); //$NON-NLS-1$
					checkHgInstallation();
					monitor.worked(1);
					// read known repositories
					monitor.subTask(Messages
							.getString("MercurialEclipsePlugin.loadingKnownMercurialRepositories")); //$NON-NLS-1$
					repoManager.start();
					monitor.worked(1);
					// read in commit messages from disk
					monitor.subTask(Messages
							.getString("MercurialEclipsePlugin.startingCommitMessageManager")); //$NON-NLS-1$
					commitMessageManager.start();
					monitor.worked(1);
					monitor.done();
					return new Status(IStatus.OK, ID, Messages
							.getString("MercurialEclipsePlugin.startedSuccessfully")); //$NON-NLS-1$
				} catch (Throwable e) {
					hgUsable = false;
					logError(Messages.getString("MercurialEclipsePlugin.unableToStart"), e); //$NON-NLS-1$
					return new Status(IStatus.ERROR, ID, e.getLocalizedMessage(), e);
				}
			}
		};

		// show console on startup if configured
		if (getPreferenceStore().getBoolean(
				MercurialPreferenceConstants.PREF_CONSOLE_SHOW_ON_STARTUP)) {
			job.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent event) {
					// open console in SWT GUI Thread
					new SafeUiJob(Messages
							.getString("MercurialEclipsePlugin.openingMercurialConsole")) { //$NON-NLS-1$
						@Override
						protected IStatus runSafe(IProgressMonitor monitor) {
							HgConsoleHolder.getInstance().showConsole(true);
							return super.runSafe(monitor);
						}
					}.schedule();
					super.done(event);
				}
			});
		}

		// Image registry must be initialized. See first stack trace in http://www.javaforge.com/issue/14327
		// Why JFaceResources wasn't initialized I don't know.
		new SafeUiJob(Messages.getString("MercurialEclipsePlugin.startingMercurialEclipse")) {
			@Override
			protected IStatus runSafe(IProgressMonitor monitor) {
				try {
					getImageRegistry();
				} finally {
		job.schedule();
	}
				return super.runSafe(monitor);
			}
		}.schedule();
	}

	/**
	 * Checks if Mercurial is configured properly by issuing the hg debuginstall command.
	 */
	public void checkHgInstallation() {
		try {
			hgUsable = true;
			MercurialUtilities.getHGExecutable(true);
			String result = HgDebugInstallClient.debugInstall();
			hgVersion = checkHgVersion();
			if (!result.endsWith("No problems detected")) { //$NON-NLS-1$
				logInfo(result, null);
			}
		} catch (Throwable e) {
			hgUsable = false;
			MercurialEclipsePlugin.logError(e);
			MercurialEclipsePlugin.showError(e);
			hgVersion = Version.emptyVersion;
		} finally {
			AbstractShellCommand.hgInitDone();
			if(isDebugging()) {
				System.out.println(HgFeatures.printSummary());
			}
		}
		}

	/**
	 * Plugin depends on native mercurial installation, which has to be checked at plugin startup.
	 * Note that the right plugin state will be set only some time after plugin startup, so that in
	 * a short time between plugin activation and
	 * {@link MercurialEclipsePlugin#checkHgInstallation()} call plugin state might be not yet
	 * initialized properly.
	 *
	 * @return true if we have already tried to identify mercurial version (independently if the
	 *         check fails or not), false if plugin is still not yet initialized properly
	 */
	public static boolean isVersionCheckDone() {
		MercurialEclipsePlugin mep = MercurialEclipsePlugin.getDefault();
		return !mep.isHgUsable() || !mep.getHgVersion().equals(Version.emptyVersion);
	}

	private Version checkHgVersion() throws HgException {
		AbstractShellCommand command = new RootlessHgCommand("version", "Checking for required version") {
			{
				isInitialCommand = startSignal.getCount() > 0;
			}
		};
		Version preferredVersion = HgFeatures.getPreferredVersion();
		String version = new String(command.executeToBytes(Integer.MAX_VALUE)).trim();
		String[] split = version.split("\\n"); //$NON-NLS-1$
		version = split.length > 0 ? split[0] : ""; //$NON-NLS-1$
		Matcher matcher = VERSION_PATTERN.matcher(version);
		boolean failedToParse = !matcher.matches() || matcher.groupCount() <= 0
				|| (version = matcher.group(1)) == null;
		if (failedToParse) {
			HgFeatures.setToVersion(preferredVersion);
			HgFeatures.applyAllTo(getPreferenceStore());
			logWarning("Can't uderstand Mercurial version string: '" + version
					+ "'. Assume that at least " + preferredVersion + " is available.", null);
			return preferredVersion;
		}
		Version detectedVersion = new Version(version);
		HgFeatures.setToVersion(detectedVersion);
		HgFeatures.applyAllTo(getPreferenceStore());
		if (!HgFeatures.isSupported(detectedVersion)) {
			throw new HgException(Messages.getString("MercurialEclipsePlugin.unsupportedHgVersion") //$NON-NLS-1$
					+ version + Messages.getString("MercurialEclipsePlugin.expectedAtLeast") //$NON-NLS-1$
					+ HgFeatures.getLowestWorkingVersion() + "."); //$NON-NLS-1$
				}
		if (!HgFeatures.isHappyWith(detectedVersion)) {
			logWarning("Can not use some of the new Mercurial features, "
					+ "hg version greater equals " + preferredVersion + " required, but "
					+ detectedVersion + " found. Features state:\n" + HgFeatures.printSummary() + ".",
					null);
			}
		return detectedVersion;
		}

	/**
	 * @return the observer hg version, never null. Returns {@link Version#emptyVersion} in case the
	 *         hg version is either not detected yet or can't be parsed properly
	 */
	public Version getHgVersion() {
		return hgVersion;
	}

	/**
	 * Gets the repository manager
	 */
	public static HgRepositoryLocationManager getRepoManager() {
		return repoManager;
	}

	public static HgCommitMessageManager getCommitMessageManager() {
		return commitMessageManager;
	}

	public static void setCommitMessageManager(HgCommitMessageManager commitMessageManager) {
		MercurialEclipsePlugin.commitMessageManager = commitMessageManager;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		try {
			repoManager.stop();
			// save commit messages to disk
			commitMessageManager.stop();
			proxyServiceTracker.close();
			MercurialUtilities.disposeColorsAndFonts();
		} finally {
			super.stop(context);
		}
	}

	/**
	 * Returns the shared instance.
	 */
	public static MercurialEclipsePlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in relative path.
	 *
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		ImageDescriptor descriptor = getDefault().getImageRegistry().getDescriptor(path);
		if (descriptor == null) {
			descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(ID, "icons/" + path); //$NON-NLS-1$
			getDefault().getImageRegistry().put(path, descriptor);
		}
		return descriptor;
	}

	/**
	 * Returns an image at the given plug-in relative path.
	 *
	 * @param path
	 *            the path
	 * @return the image
	 */
	public static Image getImage(String path) {
		// make sure descriptor is created
		getImageDescriptor(path);
		return getDefault().getImageRegistry().get(path);
	}

	/**
	 * Returns an image with overlay at given place at the given plug-in relative path.
	 *
	 * @param basePath
	 *            the base image plug-in relative path.
	 * @param overlayPath
	 *            the overlay image plug-in relative path.
	 * @param quadrant
	 *            the quadrant (one of {@link IDecoration} ({@link IDecoration#TOP_LEFT},
	 *            {@link IDecoration#TOP_RIGHT}, {@link IDecoration#BOTTOM_LEFT},
	 *            {@link IDecoration#BOTTOM_RIGHT} or {@link IDecoration#UNDERLAY})
	 * @return the image
	 */
	public static Image getImage(String basePath, String overlayPath, int quadrant) {
		getImageDescriptor(basePath, overlayPath, quadrant);
		return getDefault().getImageRegistry().get(basePath + overlayPath + quadrant);
	}

	/**
	 * Returns an image with overlay at given place at the given plug-in relative path.
	 *
	 * @param basePath
	 *            the base image plug-in relative path.
	 * @param overlayPath
	 *            the overlay image plug-in relative path.
	 * @param quadrant
	 *            the quadrant (one of {@link IDecoration} ({@link IDecoration#TOP_LEFT},
	 *            {@link IDecoration#TOP_RIGHT}, {@link IDecoration#BOTTOM_LEFT},
	 *            {@link IDecoration#BOTTOM_RIGHT} or {@link IDecoration#UNDERLAY})
	 * @return the image
	 */
	public static ImageDescriptor getImageDescriptor(String basePath, String overlayPath, int quadrant) {
		String key = basePath + overlayPath + quadrant;
		ImageDescriptor descriptor = getDefault().getImageRegistry().getDescriptor(key);
		if(descriptor == null) {
			Image base = getImage(basePath);
			ImageDescriptor overlay = getImageDescriptor(overlayPath);
			descriptor = new DecorationOverlayIcon(base, overlay, quadrant);
			getDefault().getImageRegistry().put(key, descriptor);
		}
		return descriptor;
	}


	public static final void logError(String message, Throwable error) {
		getDefault().getLog().log(createStatus(message, 0, IStatus.ERROR, error));
	}

	public static void showError(final Throwable error) {
		new ErrorJob(error).schedule(100);
	}

	public static final void logWarning(String message, Throwable error) {
		getDefault().getLog().log(createStatus(message, 0, IStatus.WARNING, error));
	}

	public static final void logInfo(String message, Throwable error) {
		getDefault().getLog().log(createStatus(message, 0, IStatus.INFO, error));
	}

	public static IStatus createStatus(String msg, int code, int severity, Throwable ex) {
		return new Status(severity, ID, code, msg, ex);
	}

	public static final void logError(Throwable ex) {
		logError(ex.getMessage(), ex);
	}

	/**
	 * Creates a busy cursor and runs the specified runnable. May be called from a non-UI thread.
	 *
	 * @param parent
	 *            the parent Shell for the dialog
	 * @param cancelable
	 *            if true, the dialog will support cancelation
	 * @param runnable
	 *            the runnable
	 *
	 * @exception InvocationTargetException
	 *                when an exception is thrown from the runnable
	 * @exception InterruptedException
	 *                when the progress monitor is cancelled
	 */
	public static void runWithProgress(Shell parent, boolean cancelable,
			final IRunnableWithProgress runnable) throws InvocationTargetException,
			InterruptedException {

		boolean createdShell = false;
		Shell myParent = parent;
		try {
			if (myParent == null || myParent.isDisposed()) {
				Display display = Display.getCurrent();
				if (display == null) {
					// cannot provide progress (not in UI thread)
					runnable.run(new NullProgressMonitor());
					return;
				}
				// get the active shell or a suitable top-level shell
				myParent = display.getActiveShell();
				if (myParent == null) {
					myParent = new Shell(display);
					createdShell = true;
				}
			}
			// pop up progress dialog after a short delay
			final Exception[] holder = new Exception[1];
			BusyIndicator.showWhile(myParent.getDisplay(), new Runnable() {
				public void run() {
					try {
						runnable.run(new NullProgressMonitor());
					} catch (InvocationTargetException e) {
						holder[0] = e;
					} catch (InterruptedException e) {
						holder[0] = e;
					}
				}
			});
			if (holder[0] != null) {
				if (holder[0] instanceof InvocationTargetException) {
					throw (InvocationTargetException) holder[0];
				}
				throw (InterruptedException) holder[0];

			}
			// new TimeoutProgressMonitorDialog(parent, TIMEOUT).run(true
			// /*fork*/, cancelable, runnable);
		} finally {
			if (createdShell) {
				parent.dispose();
			}
		}
	}

	/**
	 * Convenience method to get the currently active workbench page. Note that the active page may
	 * not be the one that the usr perceives as active in some situations so this method of
	 * obtaining the activae page should only be used if no other method is available.
	 *
	 * @return the active workbench page
	 */
	public static IWorkbenchPage getActivePage() {
		return getActiveWindow().getActivePage();
	}

	/**
	 * Convenience method to get the currently active shell.
	 *
	 * @return the active workbench shell. Never null, if there is at least one window open.
	 */
	public static Shell getActiveShell() {
		return getActiveWindow().getShell();
	}

	/**
	 * Convenience method to get the currently active workbench window.
	 *
	 * @return the active workbench window. Never null, if there is at least one window open.
	 */
	public static IWorkbenchWindow getActiveWindow() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			return window;
		}
		return PlatformUI.getWorkbench().getWorkbenchWindows()[0];
	}

	public boolean isHgUsable() {
		return hgUsable;
	}

	public static Display getStandardDisplay() {
		return PlatformUI.getWorkbench().getDisplay();
	}

	public IProxyService getProxyService() {
		return (IProxyService) proxyServiceTracker.getService();
	}

	/**
	 * The default encoding which is used by the current environment.
	 * <p>
	 * <b>Note</b>: you probably want use {@link HgRoot#getEncoding()} instead, as each repository
	 * may use it's own encoding
	 * <p>
	 * <b>Note</b>: Python's encoding isn't 1-1 with Charset.name() so do not store
	 * {@link java.nio.charset.Charset}.
	 *
	 * @return a valid encoding name, never null.
	 */
	public static String getDefaultEncoding() {
		return HGENCODING;
	}

	/**
	 * Job to show error dialogs. Avoids to show hunderts of dialogs by ussing an exclusive rule.
	 *
	 * @author Andrei
	 */
	private static final class ErrorJob extends SafeUiJob {

		static class ExclusiveRule implements ISchedulingRule {
			public boolean isConflicting(ISchedulingRule rule) {
				return contains(rule);
			}

			public boolean contains(ISchedulingRule rule) {
				return rule instanceof ExclusiveRule;
			}
		}

		final IStatus status;

		private ErrorJob(Throwable error) {
			super(Messages.getString("MercurialEclipsePlugin.showError")); //$NON-NLS-1$
			if (error instanceof CoreException) {
				status = ((CoreException) error).getStatus();
			} else {
				status = createStatus(error.getMessage(), 0, IStatus.ERROR, error);
			}
			setRule(new ExclusiveRule());
		}

		@Override
		protected IStatus runSafe(IProgressMonitor monitor) {

			IJobManager jobManager = Job.getJobManager();
			String title;
			IStatus errStatus;
			if (jobManager.find(plugin).length == 1) {
				// it's me alone there
				errStatus = status;
			} else {
				// o-ho, we have multiple errors waiting to be displayed...
				title = Messages.getString("MercurialEclipsePlugin.unexpectedErrors"); //$NON-NLS-1$
				String message = Messages
						.getString("MercurialEclipsePlugin.unexpectedErrorsOccured"); //$NON-NLS-1$
				// get the latest state
				Job[] jobs = jobManager.find(plugin);
				// discard all waiting now (we are not affected)
				jobManager.cancel(plugin);
				List<IStatus> stati = new ArrayList<IStatus>();
				for (Job job : jobs) {
					if (job instanceof ErrorJob) {
						ErrorJob errorJob = (ErrorJob) job;
						stati.add(errorJob.status);
					}
				}
				IStatus[] array = stati.toArray(new IStatus[stati.size()]);
				errStatus = new MultiStatus(title, 0, array, message, null);
			}
			StatusManager.getManager().handle(errStatus, StatusManager.SHOW);
			return super.runSafe(monitor);
		}

		@Override
		public boolean belongsTo(Object family) {
			return plugin == family;
		}
	}

	/**
	 * Find the object associated with the given object that is adapted to
	 * the provided class.
	 *
	 * @param anyObject might be null
	 * @param clazz class to get the adapter for
	 * @return adapted object or null if no adapter provided or the given object is null
	 */
	public static <V> V getAdapter(Object anyObject, Class<V> clazz) {
		if (clazz.isInstance(anyObject)) {
			return clazz.cast(anyObject);
		}
		if (anyObject instanceof IAdaptable) {
			IAdaptable a = (IAdaptable) anyObject;
			return clazz.cast(a.getAdapter(clazz));
		}
		return null;
	}

	/**
	 * Unwrap and throw as a CoreException. Note: Never returns
	 * @param e The exception to use
	 * @throws CoreException
	 */
	public static void rethrow(Throwable e) throws CoreException {
		if (e instanceof CoreException) {
			throw (CoreException)e;
		} else if (e instanceof InvocationTargetException) {
			rethrow(((InvocationTargetException) e).getTargetException());
		}

		throw new HgException(e.getLocalizedMessage(), e);
	}
}