/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * stefanc	implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import junit.framework.TestCase;

import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author stefanc
 *
 */
public class AbstractCommandTest extends TestCase {

	@Override
	public final void setUp() throws Exception {
		File where = getRepository();
		if(where.exists()) {
			deleteRepository();
			if(where.exists()) {
				throw new IllegalStateException(where.getAbsolutePath() + " must not exist");
			}
		}
		where.deleteOnExit();
		createEmptyRepository(new File("test/empty-test-repo.zip"), where);
		TestConfiguration cfg = new TestConfiguration();
		HgClients.initialize(cfg, cfg, cfg);
	}

	private void createEmptyRepository(File zip, File dest) {
		try {
			ZipInputStream zipped = new ZipInputStream(new FileInputStream(zip));
			ZipEntry entry;
			while(null != (entry = zipped.getNextEntry())) {
				File file = new File(dest.getAbsolutePath() + File.separator + entry.getName());
				if(entry.isDirectory()) {
					file.mkdirs();
				}
			}
			zipped.close();
			zipped = new ZipInputStream(new FileInputStream(zip));
			while(null != (entry = zipped.getNextEntry())) {
				File file = new File(dest.getAbsolutePath() + File.separator + entry.getName());
				if(!entry.isDirectory()) {
					file.createNewFile();
					FileOutputStream out = new FileOutputStream(file);
					byte[] buff = new byte[1024];
					int read = 0;
					while(-1 != (read = zipped.read(buff))) {
						out.write(buff, 0, read);
					}
					out.close();
				}
			}
		} catch(Exception e) {
			System.err.println("Failed creating repository");
			e.printStackTrace(System.err);
			dest.delete();
		}
	}

	public final HgRoot getRepository() throws IOException {

		String testRepoRoot = System.getProperty("java.io.tmpdir") + File.separator + "test/repo";
		HgRoot where = new HgRoot(testRepoRoot);
		return where;
	}

	@Override
	public final void tearDown() throws Exception {
		try {
			super.tearDown();
			deleteRepository();
		} catch(Exception ex) {
			ex.printStackTrace(System.err);
			throw ex;
		}
	}

	private void deleteRepository() throws IOException {
		File repository = getRepository();
		delTree(repository);
		assertFalse("Unable to delete test repository", repository.exists());
	}

	private void delTree(File dir) {
		File[] sub = dir.listFiles();
		for (File file : sub) {
			if(file.isDirectory()) {
				delTree(file);
			}
			assertTrue(!file.exists() || file.delete());
		}
		assertTrue(!dir.exists() || dir.delete());
	}

	public void testCreateRepo() throws Exception {
		assertTrue(getRepository().exists());

	}

	protected void addToRepository(File newFile) throws InterruptedException,
	IOException {
		Runtime runtime = Runtime.getRuntime();
		runtime.exec("hg add " + newFile.getCanonicalPath(), null, getRepository()).waitFor();
	}

}
