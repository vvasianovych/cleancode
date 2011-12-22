/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.TestConfiguration;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * Base class for test cases
 *
 * @author bastian
 */
public abstract class AbstractMercurialTestCase extends TestCase {
	public static final List<String> CLONE_REPO_TEST1_CMD = new ArrayList<String>();
	public static final List<String> CLONE_REPO_TEST2_CMD = new ArrayList<String>();
	public static final String TEST1_LOCAL_NAME = "mercurialeclipse_tests_TEST1";
	public static final String TEST2_LOCAL_NAME = "mercurialeclipse_tests_TEST2";
	public static final String TEST1_REPO = "http://freehg.org/u/bastiand/mercurialeclipse_test1/";
	public static final String TEST2_REPO = "http://freehg.org/u/bastiand/mercurialeclipse_test2/";

	static {
		CLONE_REPO_TEST1_CMD.add("hg");
		CLONE_REPO_TEST1_CMD.add("clone");
		CLONE_REPO_TEST1_CMD.add(TEST1_REPO);
		CLONE_REPO_TEST1_CMD.add(TEST1_LOCAL_NAME);

		CLONE_REPO_TEST2_CMD.add("hg");
		CLONE_REPO_TEST2_CMD.add("clone");
		CLONE_REPO_TEST2_CMD.add(TEST2_REPO);
		CLONE_REPO_TEST2_CMD.add(TEST2_LOCAL_NAME);
	}

	/**
	 *
	 */
	public AbstractMercurialTestCase() {
	}

	/**
	 * @param name
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public AbstractMercurialTestCase(String name) throws IOException,
			InterruptedException {
		super(name);
	}

	public static boolean deleteDirectory(File path) {
		return ResourceUtils.delete(path, true);
	}


	public static boolean move(File srcDir, File destDir){
		boolean ok = srcDir.renameTo(destDir);
		if(ok){
			return true;
		}
		// we have different file systems here, so stupid Sun JDK can't move files around
		// we have to copy & move files manually... How stupid is this???
		ok = copyDir(srcDir, destDir);
		if(!ok){
			return false;
		}
		return ResourceUtils.delete(srcDir, true);
	}

	/**
	 * Copies recursively source to destination directory
	 * @param sourceDir must exist
	 * @param destDir may not exist
	 * @return true if everything was copied, false if at least one child was not copied
	 */
	public static boolean copyDir(File sourceDir, File destDir) {
		if(!destDir.exists()) {
			boolean ok = destDir.mkdirs();
			if(!ok && !destDir.exists()){
				MercurialEclipsePlugin.logError("Could not create directory '" + destDir, null);
				return false;
			}
		}

		File[] children = sourceDir.listFiles();
		for(File source : children) {
			String name = source.getName();
			File dest = new File(destDir, name);
			if(source.isDirectory()) {
				boolean ok = copyDir(source, dest);
				if(!ok){
					return false;
				}
			}
			boolean ok = copyFile(source, dest);
			if(!ok){
				return false;
			}
		}
		return true;
	}

	/**
	 * Single file copy operation.
	 * @param source - should be file only
	 * @param destination - should be already created
	 * @return true if source was successfully copied
	 */
	public static boolean copyFile(File source, File destination) {
		if (source == null || destination == null) {
			MercurialEclipsePlugin.logError("Could not copy file '" + source + "' to '"
					+ destination + "'", null);
			return false;
		}

		/*
		 * prevent from overhead on identical files - this works fine
		 * <b>only</b> if source and destination are on the same partition (=> the
		 * same filesystem). If both files are on different partitions, then
		 * 1) the file size could differ because of different chunk size
		 * 2) the file time could differ because of different timestamp
		 * formats on different file systems (e.g. NTFS and FAT)
		 */
		if (destination.lastModified() == source.lastModified()
				&& destination.length() == source.length()) {
			return true;
		}

		boolean success = true;
		FileInputStream fin = null; // Streams to the two files.
		FileOutputStream fout = null; // These are closed in the finally block.
		try {
			// Open a stream to the input file and get a channel from it
			fin = new FileInputStream(source);
			FileChannel in = fin.getChannel();

			// Now get the output channel
			FileChannel out;

			fout = new FileOutputStream(destination); // open file stream
			out = fout.getChannel(); // get its channel

			// Query the size of the input file
			long numbytes = in.size();

			// Bulk-transfer all bytes from one channel to the other.
			// This is a special feature of FileChannel channels.
			// See also FileChannel.transferFrom( )

			// TransferTo does not work under certain Linux kernel's
			// with java 1.4.2, see bug
			// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5056395
			// in.transferTo(0, numbytes, out);
			out.transferFrom(in, 0, numbytes);
		} catch (IOException e) {
			MercurialEclipsePlugin.logError("Could not copy file '" + source + "' to '"
					+ destination + "'", e);
			success = false;
		} finally {
			// Always close input and output streams. Doing this closes
			// the channels associated with them as well.

			if (fin != null) {
				try {
					fin.close();
				} catch (IOException e) {
					MercurialEclipsePlugin.logError("Could not close file stream for file '"
							+ source + "'", e);
					success = false;
				}
			}
			if (fout != null) {
				try {
					fout.close();
					boolean modified = destination.setLastModified(source.lastModified());
					if(!modified){
						MercurialEclipsePlugin.logError("Could not update last modified stamp for file '"
								+ destination + "'", null);
					}
				} catch (IOException e) {
					MercurialEclipsePlugin.logError("Could not close file stream for file '"
							+ destination + "'", e);
					success = false;
				}
			}
		}
		return success;
	}


	/**
	 * @throws IOException
	 * @throws InterruptedException
	 */
	protected String executeCommand(List<String> cmd) throws IOException,
			InterruptedException {
		ProcessBuilder builder = new ProcessBuilder(cmd);
		builder.redirectErrorStream(true);
		Process process = builder.start();
		LineNumberReader err = null;
		StringBuilder result = new StringBuilder("");
		try {
			err = new LineNumberReader(new InputStreamReader(

				process.getInputStream()));
		int ret = process.waitFor();
		String line = err.readLine();
		while (line != null) {
			result.append("\n");
			result.append(line);
			line = err.readLine();
		}
		if (ret != 0) {
			throw new RuntimeException(
					"Cannot clone test repository. Err-Output:".concat(result.toString()));
		}
		} finally {
			if (err != null) {
				err.close();
			}
		}
		return result.toString();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		TestConfiguration cfg = new TestConfiguration();
		HgClients.initialize(cfg, cfg, cfg);
		// clean up test1 repo
		deleteDirectory(new File(TEST1_LOCAL_NAME));
		// clean up test2 repo
		deleteDirectory(new File(TEST2_LOCAL_NAME));
		// set up test repository 1
		String result = executeCommand(CLONE_REPO_TEST1_CMD);
		System.out.println(result);
		// set up test repository 2
		result = executeCommand(CLONE_REPO_TEST2_CMD);
		System.out.println(result);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		// clean up test1 repo
		deleteDirectory(new File(TEST1_REPO));
		// clean up test2 repo
		deleteDirectory(new File(TEST2_REPO));
	}

}
