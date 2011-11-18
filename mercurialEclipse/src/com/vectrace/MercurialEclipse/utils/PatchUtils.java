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
package com.vectrace.MercurialEclipse.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.compare.patch.ApplyPatchOperation;
import org.eclipse.compare.patch.IFilePatch;
import org.eclipse.compare.patch.IFilePatchResult;
import org.eclipse.compare.patch.PatchConfiguration;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * @author bastian
 *
 */
public final class PatchUtils {

	public static final Pattern DIFF_START_PATTERN = Pattern.compile("^diff -r ", Pattern.MULTILINE);

	private PatchUtils() {
		// hide constructor of utility class.
	}

	/**
	 * @param outgoingPatch
	 * @return
	 * @throws HgException
	 */
	public static IFilePatch[] getFilePatches(String outgoingPatch)
			throws HgException {
		if (outgoingPatch == null) {
			return new IFilePatch[0];
		}
		Matcher matcher = PatchUtils.DIFF_START_PATTERN.matcher(outgoingPatch);
		if (matcher.find()) {
			final String strippedPatch = outgoingPatch.substring(matcher
					.start(), outgoingPatch.length());
			try {
				return PatchUtils.createPatches(strippedPatch);
			} catch (CoreException e) {
				throw new HgException(e);
			}
		}
		return new IFilePatch[0];
	}

	public static IFilePatch[] createPatches(final String patch)
			throws CoreException {
		return ApplyPatchOperation.parsePatch(new IStorage() {
			public InputStream getContents() throws CoreException {
				return new ByteArrayInputStream(patch.getBytes());
			}

			public IPath getFullPath() {
				return null;
			}

			public String getName() {
				return null;
			}

			public boolean isReadOnly() {
				return true;
			}

			public Object getAdapter(
					@SuppressWarnings("unchecked") Class adapter) {
				return null;
			}
		});
	}

	/**
	 * Gets an input stream containing the patched contents of the given file.
	 * It patches doesn't contain the file, the file contents are returned.
	 *
	 * @param file
	 *            the file to be patched
	 * @param patches
	 *            the result of a parsed patch string
	 * @param reversed
	 *            if true, a reverse patch will be done
	 * @return
	 * @throws CoreException
	 */
	public static InputStream getPatchedContents(IFile file,
			IFilePatch[] patches, boolean reversed) throws CoreException {
		for (IFilePatch patch : patches) {
			String[] headerWords = patch.getHeader().split(" ");
			String patchPath = headerWords[headerWords.length - 1].trim();
			if (file.getFullPath().toString().endsWith(patchPath)) {
				PatchConfiguration configuration = new PatchConfiguration();
				configuration.setReversed(reversed);
				IFilePatchResult patchResult = patch.apply(file, configuration,
						null);
				return patchResult.getPatchedContents();
			}
		}
		/* If there's no patch, we just return no differences */
		return file.getContents();
	}

	public static byte[] getPatchedContentsAsBytes(IFile file,
			IFilePatch[] patches, boolean reversed) throws CoreException {

		InputStream stream = getPatchedContents(file, patches, reversed);
		ByteArrayOutputStream bos = null;
		try {
			bos = new ByteArrayOutputStream();
			byte[] tmpBytes = new byte[8192];
			int read;
			while((read = stream.read(tmpBytes)) > 0){
				bos.write(tmpBytes, 0, read);
			}
		} catch (IOException e) {
			MercurialEclipsePlugin.logError(e);
			return new byte[0];
		} finally {
			try {
				stream.close();
			} catch (IOException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}
		return bos.toByteArray();
	}


}
