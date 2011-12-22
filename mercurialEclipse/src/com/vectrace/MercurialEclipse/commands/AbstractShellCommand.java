/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch           - implementation (with lots of stuff pulled up from HgCommand)
 *     Andrei Loskutov           - bug fixes
 *     Adam Berkes (Intland)     - bug fixes/restructure
 *     Zsolt Koppany (Intland)   - enhancements
 *     Philip Graf               - use default timeout from preferences
 *     John Peberdy              - refactoring
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import static com.vectrace.MercurialEclipse.MercurialEclipsePlugin.*;
import static com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants.*;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.HgFeatures;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author bastian
 */
public abstract class AbstractShellCommand extends AbstractClient {

	private static final int BUFFER_SIZE = 32768;

	/**
	 * File encoding to use. If not specified falls back to {@link HgRoot}'s encoding.
	 */
	private String encoding;

	/**
	 * Should not be used by any command except commands needed for the initialization of hg
	 * (debuginstall and version)
	 *
	 * @see MercurialEclipsePlugin#checkHgInstallation()
	 */
	protected boolean isInitialCommand;

	/**
	 * should not be used by any code except initialization of hg
	 * @see MercurialEclipsePlugin#checkHgInstallation()
	 */
	protected static final CountDownLatch startSignal = new CountDownLatch(1);

	/**
	 * See http://msdn.microsoft.com/en-us/library/ms682425(VS.85).aspx The maximum command line
	 * length for the CreateProcess function is 32767 characters. This limitation comes from the
	 * UNICODE_STRING structure.
	 * <p>
	 * See also http://support.microsoft.com/kb/830473: On computers running Microsoft Windows XP or
	 * later, the maximum length of the string that you can use at the command prompt is 8191
	 * characters. On computers running Microsoft Windows 2000 or Windows NT 4.0, the maximum length
	 * of the string that you can use at the command prompt is 2047 characters.
	 * <p>
	 * So here we simply allow maximal 100 file paths to be used in one command. Why 100? Why not?
	 * TODO The right way would be to construct the command and then check if the full command line
	 * size is >= 32767.
	 *
	 * @deprecated for the new features please check {@link HgFeatures#LISTFILE} enablement
	 */
	@Deprecated
	public static final int MAX_PARAMS = 100;

	static {
		// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=298795
		// we must run this stupid code in the UI thread
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				PlatformUI.getWorkbench().getProgressService().registerIconForFamily(
						getImageDescriptor("mercurialeclipse.png"),
						AbstractShellCommand.class);
			}
		});
	}

	/**
	 * This rule disallows hg commands run in parallel if the hg root is specified.
	 * If the hg root is not set, then this rule allows parallel job execution.
	 */
	public static class DefaultExecutionRule implements ISchedulingRule {
		protected volatile HgRoot hgRoot;

		public DefaultExecutionRule() {
			super();
		}

		public boolean isConflicting(ISchedulingRule rule) {
			return contains(rule);
		}

		public boolean contains(ISchedulingRule rule) {
			if(this == rule){
				return true;
			}
			if(!(rule instanceof DefaultExecutionRule)){
				return false;
			}
			DefaultExecutionRule rule2 = (DefaultExecutionRule) rule;
			return hgRoot != null && hgRoot.equals(rule2.hgRoot);
		}
	}

	/**
	 * This rule disallows hg commands run in parallel on the same hg root
	 */
	public static class ExclusiveExecutionRule extends DefaultExecutionRule {
		/**
		 * @param hgRoot must be not null
		 */
		public ExclusiveExecutionRule(HgRoot hgRoot) {
			super();
			Assert.isNotNull(hgRoot);
			this.hgRoot = hgRoot;
		}
	}

	// should not extend threads directly, should use thread pools or jobs.
	// In case many threads created at same time, VM can crash or at least get OOM
	class ProzessWrapper extends Job {

		private final OutputStream output;
		private final ProcessBuilder builder;
		private final DefaultExecutionRule execRule;
		volatile IProgressMonitor monitor2;
		volatile boolean started;
		private Process process;
		long startTime;
		int exitCode = -1;
		Throwable error;

		public ProzessWrapper(String name, ProcessBuilder builder, OutputStream output) {
			super(name);
			execRule = getExecutionRule();
			setRule(execRule);
			this.builder = builder;
			this.output = output;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			if(debugExecTime) {
				startTime = System.currentTimeMillis();
			} else {
				startTime = 0;
			}
			started = true;
			monitor2 = monitor;
			waitForHgInitDone();
			InputStream stream = null;
			try {
				process = builder.start();
				stream = process.getInputStream();
				int length;
				byte[] buffer = new byte[BUFFER_SIZE];
				while ((length = stream.read(buffer)) != -1) {
					output.write(buffer, 0, length);
					if(monitor.isCanceled()){
						break;
					}
				}
				exitCode = process.waitFor();
			} catch (IOException e) {
				if (!monitor.isCanceled()) {
					error = e;
				}
				return Status.CANCEL_STATUS;
			} catch (InterruptedException e) {
				if (!monitor.isCanceled()) {
					error = e;
				}
				return Status.CANCEL_STATUS;
			} finally {
				if(stream != null) {
					try {
						stream.close();
					} catch (IOException e) {
						HgClients.logError(e);
					}
				}
				try {
					output.close();
				} catch (IOException e) {
					HgClients.logError(e);
				}
				monitor.done();
				monitor2 = null;
				if(process != null) {
					process.destroy();
				}
			}
			return monitor.isCanceled()? Status.CANCEL_STATUS : Status.OK_STATUS;
		}

		/**
		 * Waits until the gate is open (hg installation is checked etc)
		 */
		private void waitForHgInitDone() {
			if(!isInitialCommand) {
				try {
					startSignal.await();
				} catch (InterruptedException e1) {
					MercurialEclipsePlugin.logError(e1);
				}
			}
		}

		private boolean isAlive() {
			// job is either not started yet (is scheduled and waiting), or it is not finished or cancelled yet
			return (!started && getResult() == null) || (monitor2 != null);
		}

		@Override
		public boolean belongsTo(Object family) {
			return AbstractShellCommand.class == family;
		}

		@Override
		protected void canceling() {
			super.canceling();
			if(process != null) {
				process.destroy();
			}
			// remove exclusive lock on the hg root
			execRule.hgRoot = null;
		}
	}

	protected String command;

	/**
	 * Calculated commands. See {@link #getCommands()}
	 */
	private List<String> commands;

	/**
	 * Whether files should be preceded by "--" on the command line.
	 * @see #files
	 */
	private final boolean escapeFiles;

	protected List<String> options;

	/**
	 * The working directory. May be null for default working directory.
	 */
	protected final File workingDir;

	protected final List<String> files;
	private String timeoutConstant;
	private ProzessWrapper processWrapper;
	private boolean showOnConsole;
	private final boolean debugMode;
	private final boolean isDebugging;
	private final boolean debugExecTime;

	/**
	 * Though this command might not invoke hg, it might get encoding information from it. May be
	 * null.
	 */
	protected final HgRoot hgRoot;

	private DefaultExecutionRule executionRule;

	/**
	 * Human readable name for this operation
	 */
	private final String uiName;

	/**
	 * @param uiName
	 *            Human readable name for this command
	 * @param hgRoot
	 *            Though this command might not invoke hg, it might get encoding information from
	 *            it. May be null.
	 */
	protected AbstractShellCommand(String uiName, HgRoot hgRoot, File workingDir, boolean escapeFiles) {
		super();
		this.hgRoot = hgRoot;
		this.workingDir = workingDir;
		this.escapeFiles = escapeFiles;
		this.uiName = uiName;
		options = new ArrayList<String>();
		files = new ArrayList<String>();
		showOnConsole = true;
		isDebugging = Boolean.valueOf(Platform.getDebugOption(MercurialEclipsePlugin.ID + "/debug/commands")).booleanValue();
		debugMode = Boolean.valueOf(HgClients.getPreference(PREF_CONSOLE_DEBUG, "false")).booleanValue(); //$NON-NLS-1$
		debugExecTime = Boolean.valueOf(HgClients.getPreference(PREF_CONSOLE_DEBUG_TIME, "false")).booleanValue(); //$NON-NLS-1$
		timeoutConstant = MercurialPreferenceConstants.DEFAULT_TIMEOUT;

		Assert.isNotNull(uiName);
	}

	protected AbstractShellCommand(String uiName, HgRoot hgRoot, List<String> commands, File workingDir, boolean escapeFiles) {
		this(uiName, hgRoot, workingDir, escapeFiles);

		this.commands = commands;
	}

	/**
	 * Should not be called by any code except for hg initialization job
	 * Opens the command execution gate after hg installation is checked etc
	 * @see MercurialEclipsePlugin#checkHgInstallation()
	 */
	public static void hgInitDone() {
		startSignal.countDown();
	}

	/**
	 * Per default, a non-exclusive rule is created
	 * @return rule for hg job execution, never null
	 */
	protected DefaultExecutionRule getExecutionRule() {
		if(executionRule == null) {
			executionRule = new DefaultExecutionRule();
		}
		return executionRule;
	}

	public void setExecutionRule(DefaultExecutionRule rule){
		executionRule = rule;
	}

	public void addOptions(String... optionsToAdd) {
		for (String option : optionsToAdd) {
			options.add(option);
		}
	}

	public byte[] executeToBytes() throws HgException {
		return executeToBytes(getTimeOut());
	}

	public byte[] executeToBytes(int timeout) throws HgException {
		return executeToBytes(timeout, true);
	}

	/**
	 * Execute a command.
	 *
	 * @param timeout
	 *            -1 if no timeout, else the timeout in ms.
	 */
	public byte[] executeToBytes(int timeout, boolean expectPositiveReturnValue) throws HgException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(BUFFER_SIZE);
		if (executeToStream(bos, timeout, expectPositiveReturnValue)) {
			return bos.toByteArray();
		}
		return null;
	}

	protected final boolean executeToStream(OutputStream output, int timeout, boolean expectPositiveReturnValue)
			throws HgException {

		List<String> cmd = getCommands();

		String jobName = obfuscateLoginData(getCommandInvoked(cmd));

		File tmpFile = setupTmpFile(cmd);

		ProcessBuilder builder = setupProcess(cmd);

		try {

			// I see sometimes that hg has errors if it runs in parallel
			// using a job with exclusive rule here serializes all hg access from plugin.
			processWrapper = createProcessWrapper(output, uiName, builder);

			logConsoleCommandInvoked(jobName);

			// will start hg command as soon as job manager allows us to do it
			processWrapper.schedule();

			try {
				waitForConsumer(timeout);
			} catch (InterruptedException e) {
				processWrapper.cancel();
				throw new HgException("Process cancelled: " + jobName, e);
			}

			if (processWrapper.isAlive()) {
				// command timeout
				processWrapper.cancel();
				throw new HgException("Process timeout: " + jobName);
			}

			IStatus result = processWrapper.getResult();
			if (processWrapper.process == null) {
				// process is either not started or we failed to create it
				if(result == null){
					// was not started at all => timeout?
					throw new HgException("Process timeout: " + jobName);
				}
				if(processWrapper.error == null && result == Status.CANCEL_STATUS) {
					throw new HgException(HgException.OPERATION_CANCELLED,
							"Process cancelled: " + jobName, null);
				}
				throw new HgException("Process start failed: " + jobName, processWrapper.error);
			}

			final String msg = getMessage(output);
			final int exitCode = processWrapper.exitCode;
			long timeInMillis = debugExecTime? System.currentTimeMillis() - processWrapper.startTime : 0;
			// everything fine
			if (exitCode != 0 && expectPositiveReturnValue) {
				Throwable rootCause = result != null ? result.getException() : null;
				final HgException hgex = new HgException(exitCode,
						msg, jobName, rootCause);
				logConsoleCompleted(timeInMillis, msg, exitCode, hgex);
				throw hgex;
			}
			if (debugExecTime || debugMode) {
				logConsoleCompleted(timeInMillis, msg, exitCode, null);
			}

			return true;
		} finally {
			if (tmpFile != null && tmpFile.isFile()) {
				tmpFile.delete();
			}
		}
	}

	protected ProzessWrapper createProcessWrapper(OutputStream output, String jobName, ProcessBuilder builder) {
		return new ProzessWrapper(jobName, builder, output);
	}

	/**
	 * @param cmd hg commands to run
	 * @return temporary file path used to write {@link HgFeatures#LISTFILE} arguments
	 */
	private static File setupTmpFile(List<String> cmd) {
		for (String line : cmd) {
			if (line.startsWith(HgFeatures.LISTFILE.getHgCmd())) {
				return new File(line.substring(HgFeatures.LISTFILE.getHgCmd().length()));
			}
		}
		return null;
	}

	private ProcessBuilder setupProcess(List<String> cmd) {
		ProcessBuilder builder = new ProcessBuilder(cmd);

		// set locale to english have deterministic output
		Map<String, String> env = builder.environment();
		// From wiki: http://mercurial.selenic.com/wiki/UpgradeNotes
		// "If your tools look for particular English messages in Mercurial output,
		// they should disable translations with LC_ALL=C"
		env.put("LC_ALL", "C"); //$NON-NLS-1$ //$NON-NLS-2$
		env.put("LANG", "C"); //$NON-NLS-1$ //$NON-NLS-2$
		env.put("LANGUAGE", "C"); //$NON-NLS-1$ //$NON-NLS-2$

		// HGPLAIN normalizes output in Mercurial 1.5+
		env.put("HGPLAIN", "set by MercurialEclipse"); //$NON-NLS-1$ //$NON-NLS-2$
		String charset = setupEncoding(cmd);
		if (charset != null) {
			env.put("HGENCODING", charset); //$NON-NLS-1$
		}

		env.put("HGE_RUNDIR", getRunDir());

		// removing to allow using eclipse merge editor
		builder.environment().remove("HGMERGE");

		builder.redirectErrorStream(true); // makes my life easier
		if (workingDir != null) {
			builder.directory(workingDir);
		}
		return builder;
	}

	private String setupEncoding(List<String> cmd) {
		if(hgRoot == null){
			return null;
		}
		String charset = hgRoot.getEncoding();
		// Enforce strict command line encoding
		cmd.add(1, charset);
		cmd.add(1, "--encoding");
		// Enforce fallback encoding for UI (command output)
		// Note: base encoding is UTF-8 for mercurial, fallback is only take into account
		// if actual platfrom don't support it.
		cmd.add(1, "ui.fallbackencoding=" + hgRoot.getFallbackencoding().name()); //$NON-NLS-1$
		cmd.add(1, "--config"); //$NON-NLS-1$
		return charset;
	}

	private void waitForConsumer(int timeout) throws InterruptedException {
		if (timeout <= 0) {
			timeout = 1;
		}
		long start = System.currentTimeMillis();
		long now = 0;
		while (processWrapper.isAlive()) {
			long delay = timeout - now;
			if (delay <= 0) {
				break;
			}
			synchronized (this){
				wait(10);
			}
			now = System.currentTimeMillis() - start;
		}
	}

	protected void logConsoleCommandInvoked(final String commandInvoked) {
		if(isDebugging){
			System.out.println(commandInvoked);
		}
		if (showOnConsole) {
			getConsole().commandInvoked(commandInvoked);
		}
	}

	private void logConsoleCompleted(final long timeInMillis, final String msg, final int exitCode, final HgException hgex) {
		if(isDebugging){
			System.out.println(msg);
			if(hgex != null){
				hgex.printStackTrace();
			}
		}
		if (showOnConsole) {
			getConsole().commandCompleted(exitCode, timeInMillis, msg, hgex);
		}
	}

	private String getMessage(OutputStream output) {
		String msg = null;
		if (output instanceof FileOutputStream) {
			return null;
		} else if (output instanceof ByteArrayOutputStream) {
			ByteArrayOutputStream baos = (ByteArrayOutputStream) output;
			try {
				msg = baos.toString(getEncoding());
			} catch (UnsupportedEncodingException e) {
				logError(e);
				msg = baos.toString();
			}
			if(msg != null){
				msg = msg.trim();
			}
		}
		return msg;
	}

	/**
	 * Sets the command output charset if the charset is available in the VM.
	 */
	public void setEncoding(String charset) {
		encoding = charset;
	}
	/**
	 * @return never returns null
	 */
	private String getEncoding() {
		if(encoding == null){
			if (hgRoot != null) {
				encoding = hgRoot.getEncoding();
			} else {
				encoding = getDefaultEncoding();
			}
		}
		return encoding;
	}

	public String executeToString() throws HgException {
		return executeToString(true);
	}

	public String executeToString(boolean expectPositiveReturnValue) throws HgException {
		byte[] bytes = executeToBytes(getTimeOut(), expectPositiveReturnValue);
		if (bytes != null && bytes.length > 0) {
			try {
				return new String(bytes, getEncoding());
			} catch (UnsupportedEncodingException e) {
				throw new HgException(e.getLocalizedMessage(), e);
			}
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * Executes a command and writes its output to a file.
	 *
	 * @param file
	 *            The file to which the output is written.
	 * @param expectPositiveReturnValue
	 *            If set to {@code true}, an {@code HgException} will be thrown if the command's
	 *            exit code is not zero.
	 * @return Returns {@code true} iff the command was executed successfully.
	 * @throws HgException
	 *             Thrown when the command could not be executed successfully.
	 */
	public boolean executeToFile(File file, boolean expectPositiveReturnValue) throws HgException {
		int timeout = HgClients.getTimeOut(MercurialPreferenceConstants.DEFAULT_TIMEOUT);
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file, false);
			return executeToStream(fos, timeout, expectPositiveReturnValue);
		} catch (FileNotFoundException e) {
			throw new HgException(e.getMessage(), e);
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					throw new HgException(e.getMessage(), e);
				}
			}
		}
	}

	private List<String> getCommands() {
		if (commands != null) {
			return commands;
		}
		List<String> result = new ArrayList<String>();
		result.add(getExecutable());
		result.add(command);
		result.addAll(options);

		String listFilesFile = null;
		if (HgFeatures.LISTFILE.isEnabled() && getCommandLineLength(files) > 8000) {
			listFilesFile = createListFilesFile(files);
		}
		if(listFilesFile != null){
			result.add(listFilesFile);
		} else {
			if (escapeFiles && !files.isEmpty()) {
				result.add("--"); //$NON-NLS-1$
			}
			result.addAll(files);
		}
		customizeCommands(result);

		return commands = result;
	}

	private static String createListFilesFile(List<String> paths) {
		BufferedWriter bw = null;
		try {
			File listFile = File.createTempFile("listfile_", "txt");
			bw = new BufferedWriter(new FileWriter(listFile));
			for (String file : paths) {
				bw.write(file);
				bw.newLine();
			}
			bw.flush();
			return HgFeatures.LISTFILE.getHgCmd() + listFile.getAbsolutePath();
		} catch (IOException ioe) {
			MercurialEclipsePlugin.logError(ioe);
			return null;
		} finally {
			if(bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
					MercurialEclipsePlugin.logError(e);
				}
			}
		}
	}

	private static int getCommandLineLength(List<String> cmds) {
		int length = 0;
		for (String f : cmds) {
			length += f.getBytes().length;
		}
		return length;
	}


	/**
	 * Can be used after execution to get a list of paths needed to be updated
	 * @return a copy of file paths affected by this command, if any. Never returns null,
	 * but may return empty list. The elements of the set are absolute file paths.
	 */
	public Set<String> getAffectedFiles(){
		Set<String> fileSet = new HashSet<String>();
		fileSet.addAll(files);
		return fileSet;
	}

	/**
	 * Template method to customize the commands to execute
	 * @param cmd The list of commands to execute.
	 */
	protected void customizeCommands(List<String> cmd) {
	}

	protected abstract String getExecutable();

	private String getRunDir()
	{
		String sDir = getExecutable();
		int i;

		if (sDir != null)
		{
			i = Math.max(sDir.lastIndexOf('\\'), sDir.lastIndexOf('/'));

			if (i >= 0)
			{
				return sDir.substring(0, i);
			}
		}

		return "";
	}

	/**
	 * Add a file. Need not be canonical, but will try transform to canonical.
	 *
	 * @param myfiles The files to add
	 */
	public void addFiles(Collection<File> myfiles) {
		for (File file : myfiles) {
			addFile(file);
		}
	}

	/**
	 * Add a file. Need not be canonical, but will try transform to canonical.
	 *
	 * @param file The file to add
	 */
	public void addFile(File file) {
		String sfile;
		try {
			sfile = file.getCanonicalPath();
		} catch (IOException e) {
			MercurialEclipsePlugin.logError(e);
			sfile = file.getAbsolutePath();
		}

		files.add(sfile);
	}

	public void addFiles(IResource... resources) {
		for (IResource resource : resources) {
			addResource(resource);
		}
	}

	public void addFiles(List<? extends IResource> resources) {
		for (IResource resource : resources) {
			addResource(resource);
		}
	}

	/**
	 * Add all resources that are of type IResource.FILE
	 */
	public void addFilesWithoutFolders(List<? extends IResource> resources) {
		for (IResource resource : resources) {
			if (resource.getType() == IResource.FILE) {
				addResource(resource);
			}
		}
	}

	private void addResource(IResource resource) {
		// TODO This can be done faster without any file system calls by saving uncanonicalized hg
		// root locations (?).
		// files.add(resource.getLocation().toOSString());
		IPath location = ResourceUtils.getPath(resource);
		if(!location.isEmpty()) {
			addFile(location.toFile());
		}
	}

	public void setUsePreferenceTimeout(String cloneTimeout) {
		this.timeoutConstant = cloneTimeout;
	}

	public void terminate() {
		if (processWrapper != null) {
			processWrapper.cancel();
		}
	}

	private static IConsole getConsole() {
		return HgClients.getConsole();
	}

	public void setShowOnConsole(boolean b) {
		showOnConsole = b;
	}

	private String getCommandInvoked(List<String> cmd) {
		if(cmd.isEmpty()){
			// paranoia
			return "<empty command>";
		}
		StringBuilder sb = new StringBuilder();
		if(workingDir != null){
			sb.append(workingDir);
			sb.append(':');
		}
		String exec = cmd.get(0);
		exec = exec.replace('\\', '/');
		int lastSep = exec.lastIndexOf('/');
		if(lastSep <= 0){
			sb.append(exec);
		} else {
			// just the exec. name, not the full path
			if(exec.endsWith(".exe")){
				sb.append(exec.substring(lastSep + 1, exec.length() - 4));
			} else {
				sb.append(exec.substring(lastSep + 1));
			}
		}
		for (int i = 1; i < cmd.size(); i++) {
			sb.append(" ").append(cmd.get(i));
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append(" [");
		if (command != null) {
			builder.append("command=");
			builder.append(command);
			builder.append(", ");
		}
		if (commands != null) {
			builder.append("commands=");
			builder.append(commands);
			builder.append(", ");
		}
		if (options != null) {
			builder.append("options=");
			builder.append(options);
			builder.append(", ");
		}
		if (workingDir != null) {
			builder.append("workingDir=");
			builder.append(workingDir);
			builder.append(", ");
		}
		if (files != null) {
			builder.append("files=");
			builder.append(files);
			builder.append(", ");
		}
		builder.append("escapeFiles=");
		builder.append(escapeFiles);
		builder.append(", ");
		if (processWrapper != null) {
			builder.append("processWrapper=");
			builder.append(processWrapper);
			builder.append(", ");
		}
		builder.append("showOnConsole=");
		builder.append(showOnConsole);
		builder.append(", ");
		if (timeoutConstant != null) {
			builder.append("timeoutConstant=");
			builder.append(timeoutConstant);
		}
		builder.append("]");
		return builder.toString();
	}

	private int getTimeOut() {
		int timeout;
		if (timeoutConstant == null) {
			timeoutConstant = MercurialPreferenceConstants.DEFAULT_TIMEOUT;
		}
		timeout = HgClients.getTimeOut(timeoutConstant);
		return timeout;
	}
}
