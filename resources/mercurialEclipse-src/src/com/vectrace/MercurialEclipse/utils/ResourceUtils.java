/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bastian	               - implementation
 *     Andrei Loskutov         - bugfixes
 *     Zsolt Koppany (Intland)
 *******************************************************************************/
package com.vectrace.MercurialEclipse.utils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.ResourceUtil;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileFromChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgResource;
import com.vectrace.MercurialEclipse.model.PathFromChangeSet;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.MercurialRootCache;

/**
 * @author bastian
 * @author Andrei Loskutov
 */
public final class ResourceUtils {

	public static final IPath[] NO_PATHS = new Path[0];
	private static final File TMP_ROOT = new File(System.getProperty("java.io.tmpdir"));
	private static long tmpFileSuffix;

	private ResourceUtils() {
		// hide constructor of utility class.
	}

	public static File getSystemTempDirectory() {
		return TMP_ROOT;
	}

	/**
	 * @return a newly created temp directory which is located inside the default temp directory
	 */
	public static File createNewTempDirectory() {
		File tmp = getSystemTempDirectory();
		File newTemp = null;
		while (!(newTemp = new File(tmp, "hgTemp_" + tmpFileSuffix)).mkdir()) {
			tmpFileSuffix++;
		}
		return newTemp;
	}

	/**
	 * If "recursive" is false, then this is a single file/directory delete operation. Directory
	 * should be empty before it can be deleted. If "recursive" is true, then all children will be
	 * deleted too.
	 *
	 * @param source
	 * @return true if source was successfully deleted or if it was not existing
	 */
	public static boolean delete(File source, boolean recursive) {
		if (source == null || !source.exists()) {
			return true;
		}
		if (recursive) {
			if (source.isDirectory()) {
				for (File file : source.listFiles()) {
					boolean ok = delete(file, true);
					if (!ok) {
						return false;
					}
				}
			}
		}
		boolean result = source.delete();
		if (!result && !source.isDirectory()) {
			MercurialEclipsePlugin.logWarning("Could not delete file '" + source + "'", null);
		}
		return result;
	}

	/**
	 * Moves contents of one directory to another and deletes source directory if all files were
	 * successfully moved to destination. If any target file with the same relative path exists in the
	 * destination directory, it will be NOT overridden, and kept in the source directory.
	 *
	 * @param sourceDir
	 *            - must already exist and be a directory
	 * @param destinationDir
	 *            - must already exist and be a directory
	 * @return true if source was successfully moved to destination.
	 */
	public static boolean move(File sourceDir, File destinationDir) {
		File[] files = sourceDir.listFiles();
		if(files == null) {
			// can't be ok
			return false;
		}
		Set<File> fileSet = new LinkedHashSet<File>(Arrays.asList(files));
		boolean result = true;
		while (!fileSet.isEmpty()) {
			File next = fileSet.iterator().next();
			String relative = toRelative(sourceDir, next);
			File dest = new File(destinationDir, relative);
			if (!dest.exists()) {
				result &= next.renameTo(dest);
			} else if(next.isDirectory()){
				files = next.listFiles();
				if(files != null) {
					fileSet.addAll(Arrays.asList(files));
				}
			} else {
				// file exists in target
				result = false;
			}
			fileSet.remove(next);
		}
		try {
			if(result && !sourceDir.getCanonicalFile().equals(destinationDir.getCanonicalFile())) {
				return ResourceUtils.delete(sourceDir, true);
			}
		} catch (IOException e) {
			MercurialEclipsePlugin.logError(e);
		}
		return false;
	}

	/**
	 * Converts given path to the relative
	 *
	 * @param parent
	 *            parent path, non null
	 * @param child
	 *            a possible child path, non null
	 * @return a parent relative path of a given child file, if the given child file is located
	 *         under given parent, otherwise the given child path. If the given child path matches
	 *         the parent, returns an empty string
	 */
	public static String toRelative(File parent, File child) {
		// first try with the unresolved path. In most cases it's enough
		String fullPath = child.getAbsolutePath();
		String parentpath = parent.getPath();
		if (!fullPath.startsWith(parentpath)) {
			try {
				// ok, now try to resolve all the links etc. this takes A LOT of time...
				fullPath = child.getCanonicalPath();
				if (!fullPath.startsWith(parentpath)) {
					return child.getPath();
				}
			} catch (IOException e) {
				MercurialEclipsePlugin.logError(e);
				return child.getPath();
			}
		}
		if(fullPath.equals(parentpath)){
			return Path.EMPTY.toOSString();
		}
		// +1 is to remove the file separator / at the start of the relative path
		return fullPath.substring(parentpath.length() + 1);
	}

	/**
	 * Checks which editor is active an determines the IResource that is edited.
	 */
	public static IFile getActiveResourceFromEditor() {
		IEditorPart editorPart = MercurialEclipsePlugin.getActivePage().getActiveEditor();

		if (editorPart != null) {
			IFileEditorInput input = (IFileEditorInput) editorPart.getEditorInput();
			IFile file = ResourceUtil.getFile(input);
			return file;
		}
		return null;
	}

	/**
	 * @param resource
	 *            a handle to possibly non-existing resource
	 * @return a (file) path representing given resource, never null. May return an "empty" file.
	 */
	public static File getFileHandle(IResource resource) {
		return getPath(resource).toFile();
	}

	/**
	 * Tries to determine the encoding for a file resource. Returns null, if the encoding cannot be
	 * determined.
	 */
	public static String getFileEncoding(IFile resource){
		try{
			String charset = resource.getCharset(true);
			if(charset != null) {
				new String(new byte[]{}, charset); //test that JVM has the charset available
			}
			return charset;
		} catch (CoreException e) {
			//cannot determine the file charset
			return null;
		} catch (UnsupportedEncodingException e) {
			//unknown encoding, ignore the request
			return null;
		}
	}

	/**
	 * @param path
	 *            a path to possibly non-existing or not mapped resource
	 * @return a (file) representing given resource, may return null if the resource is not in the
	 *         workspace
	 */
	public static IFile getFileHandle(IPath path) {
		if(path == null) {
			return null;
		}
		return (IFile) getHandle(path, true);
	}

	private static IResource getHandle(final IPath origPath, boolean isFile) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		// origPath may be canonical but not necessarily.
		// Eclipse allows a project to be symlinked or an arbitrary folder under a project
		// to be symlinked. Also Eclipse allows a subtree of one project to exist as another
		// project.
		// Mercurial doesn't follow symbolic links so if path is canonical it is sufficient to find
		// a containing hg root. hg roots are always canonical.
		// There is an unresolvable ambiguity when a project with a sub repo is imported
		// as a project. Such cases are unsupported for now.
		// If one of the candidate resources is under a Mercurial managed project it must
		// be returned.
		IPath[] paths = NO_PATHS;
		IResource best = null;

		loop: for (int i = 0;; i++) {
			IPath path;

			switch (i) {
			case 0:
				path = origPath;
				break;
			case 1:
				// Only query the root cache if the plain path didn't find a definite match.
				paths = MercurialRootCache.getInstance().uncanonicalize(origPath);
				//$FALL-THROUGH$
			default:
				if (i - 1 >= paths.length) {
					break loop;
				}
				path = paths[i - 1];
			}

			URI uri = URIUtil.toURI(path.makeAbsolute());
			IResource[] resources = isFile ? root.findFilesForLocationURI(uri) : root
					.findContainersForLocationURI(uri);
			if (resources.length > 0) {
				if (resources.length == 1) {
					best = resources[0];
				} else {
					// try to find the first file contained in a hg root and managed by our team
					// provider
					for (IResource resource : resources) {
						if (MercurialTeamProvider.isHgTeamProviderFor(resource.getProject())) {
							return resource;
						}
					}
				}
			} else {
				best = ifNull(isFile ? root.getFileForLocation(path) : root
						.getContainerForLocation(path), best);
			}

			// Is best a definite match?
			if (best != null && MercurialTeamProvider.isHgTeamProviderFor(best.getProject())) {
				return best;
			}
		}
		if(best == null) {
			Collection<HgRoot> roots = MercurialRootCache.getInstance().getKnownHgRoots();
			for (HgRoot hgRoot : roots) {
				if(!hgRoot.getIPath().isPrefixOf(origPath)) {
					continue;
				}
				IPath relative = hgRoot.toRelative(origPath);
				if(relative.isEmpty()) {
					if(!isFile) {
						// same folder as root
						return hgRoot.getResource();
					}
					// requested is file => some error!
					return null;
				}
				best = hgRoot.getResource().findMember(relative);

				if(best != null) {
					if(isFile && best.getType() == IResource.FILE
							|| ! isFile && best.getType() != IResource.FILE) {
						return best;
					}
					if(isFile) {
						return hgRoot.getResource().getFile(relative);
					}
					return hgRoot.getResource().getFolder(relative);
				}
			}
		}
		return best;
	}

	private static IResource ifNull(IResource a, IResource b) {
		return a == null ? b : a;
	}

	/**
	 * @param resource
	 *            a handle to possibly non-existing resource
	 * @return a (file) path representing given resource, might be {@link Path#EMPTY} in case the
	 *         resource location and project location are both unknown. {@link Path#EMPTY} return
	 *         value will be logged as error unless virtual.
	 */
	public static IPath getPath(IResource resource) {
		IPath path = resource.getLocation();
		if (path == null) {
			// file was removed
			IProject project = resource.getProject();
			IPath projectLocation = project.getLocation();
			if (projectLocation == null) {
				// project removed too, there is no way to correctly determine the right
				// location in case project is not located under workspace or project name doesn't
				// match project root folder name
				String message = "Failed to resolve location for resource (project deleted): " + resource;
				MercurialEclipsePlugin.logWarning(message, new IllegalStateException(message));
				return Path.EMPTY;
			}

			URI locationURI = resource.getLocationURI();
			if(isVirtual(locationURI)) {
				// path is null for virtual folders => we can't do anything here
				return Path.EMPTY;
			}
			path = projectLocation.append(resource.getFullPath().removeFirstSegments(1));
		}
		return path;
	}

	/**
	 * see issue 12500: we should check whether the resource is virtual
	 * <p>
	 * TODO as soon as 3.5 is not supported, use resource.isVirtual() call
	 *
	 * @param locationURI
	 *            may be null
	 * @return true if the given path is null OR is virtual location
	 */
	public static boolean isVirtual(URI locationURI) {
		return locationURI == null || "virtual".equals(locationURI.getScheme());
	}

	/**
	 * @param linked non null linked resource
	 * @return may return null if the link target is not inside workspace
	 */
	public static IResource getRealLocation(IResource linked) {
		IPath path = getPath(linked);
		if(path.isEmpty()) {
			return null;
		}
		IResource handle = getHandle(path, linked.getType() == IResource.FILE);
		if(handle == null || handle.isLinked()) {
			return null;
		}
		return handle;
	}

	/**
	 * Converts a {@link java.io.File} to a workspace resource
	 */
	public static IResource convert(File file) throws HgException {
		String canonicalPath;
		try {
			canonicalPath = file.getCanonicalPath();
		} catch (IOException e) {
			throw new HgException(e.getLocalizedMessage(), e);
		}
		return getHandle(new Path(canonicalPath), !file.isDirectory());
	}

	/**
	 * For a given path, tries to find out first <b>existing</b> parent directory
	 *
	 * @param path
	 *            may be null
	 * @return may return null
	 */
	public static File getFirstExistingDirectory(File path) {
		while (path != null && !path.isDirectory()) {
			path = path.getParentFile();
		}
		return path;
	}

	/**
	 * For a given path, tries to find out first <b>existing</b> parent directory
	 *
	 * @param res
	 *            may be null
	 * @return may return null
	 */
	public static IContainer getFirstExistingDirectory(IResource res) {
		if (res == null) {
			return null;
		}
		IContainer parent = res instanceof IContainer ? (IContainer) res : res.getParent();
		if (parent instanceof IWorkspaceRoot) {
			return null;
		}
		while (parent != null && !parent.exists()) {
			parent = parent.getParent();
			if (parent instanceof IWorkspaceRoot) {
				return null;
			}
		}
		return parent;
	}

	/**
	 * @param resources
	 *            non null
	 * @return never null
	 */
	public static Map<IProject, List<IResource>> groupByProject(Collection<IResource> resources) {
		Map<IProject, List<IResource>> result = new HashMap<IProject, List<IResource>>();
		for (IResource resource : resources) {
			IProject root = resource.getProject();
			List<IResource> list = result.get(root);
			if (list == null) {
				list = new ArrayList<IResource>();
				result.put(root, list);
			}
			list.add(resource);
		}
		return result;
	}

	/**
	 * @return never null, a list with all projects contained by given hg root directory
	 */
	public static Set<IProject> getProjects(HgRoot root) {
		Set<IProject> set = new HashSet<IProject>();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects) {
			if (!project.isAccessible()) {
				continue;
			}
			HgRoot proot = MercurialRootCache.getInstance().hasHgRoot(project, true);
			if (proot == null) {
				continue;
			}
			if (root.equals(proot)) {
				set.add(project);
			}
		}
		return set;
	}

	/**
	 * @param resources
	 *            non null
	 * @return never null
	 */
	public static Map<HgRoot, List<IResource>> groupByRoot(Collection<? extends IResource> resources) {
		Map<HgRoot, List<IResource>> result = new HashMap<HgRoot, List<IResource>>();
		if (resources != null) {
			for (IResource resource : resources) {
				HgRoot root = MercurialRootCache.getInstance().hasHgRoot(resource, true);
				if (root == null) {
					continue;
				}
				List<IResource> list = result.get(root);
				if (list == null) {
					list = new ArrayList<IResource>();
					result.put(root, list);
				}
				list.add(resource);
			}
		}
		return result;
	}

	public static void collectAllResources(IContainer root, Set<IResource> children) {
		IResource[] members;
		try {
			members = root.members();
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
			return;
		}
		children.add(root);
		for (IResource res : members) {
			if (res instanceof IFolder && !res.equals(root)) {
				collectAllResources((IFolder) res, children);
			} else {
				children.add(res);
			}
		}
	}

	/**
	 * Converts a HgRoot relative path to a project relative IResource. The specified hgRoot
	 * can be higher than, deeper than, or at project level in the directory hierarchy.
	 * @param hgRoot
	 *            non null
	 * @param project
	 *            non null
	 * @param repoRelPath
	 *            path <b>relative</b> to the hg root
	 * @return may return null, if the path is not found in the project
	 */
	public static IResource convertRepoRelPath(HgRoot hgRoot, IProject project, String repoRelPath) {
		// determine absolute path
		IPath path = hgRoot.toAbsolute(repoRelPath);

		// determine project relative path
		int equalSegments = path.matchingFirstSegments(getPath(project));
		path = path.removeFirstSegments(equalSegments);
		return project.findMember(path);
	}

	public static Set<IResource> getMembers(IResource r) {
		return getMembers(r, true);
	}

	public static Set<IResource> getMembers(IResource r, boolean withLinks) {
		HashSet<IResource> set = new HashSet<IResource>();
		if (r instanceof IContainer && r.isAccessible()) {
			IContainer cont = (IContainer) r;
			try {
				IResource[] members = cont.members();
				if (members != null) {
					for (IResource member : members) {
						if(!withLinks && member.isLinked()) {
							continue;
						}
						if (member instanceof IContainer) {
							set.addAll(getMembers(member));
						} else {
							set.add(member);
						}
					}
				}
			} catch (CoreException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}
		set.add(r);
		return set;
	}

	/**
	 * @param o
	 *            some object which is or can be adapted to resource
	 * @return given object as resource, may return null
	 */
	public static IResource getResource(Object o) {
		if(o == null) {
			return null;
		}
		if (o instanceof IResource) {
			return (IResource) o;
		}
		if (o instanceof ChangeSet) {
			ChangeSet changeSet = (ChangeSet) o;
			Set<IFile> files = changeSet.getFiles();
			if (files.size() > 0) {
				IFile file = files.iterator().next();
				if (files.size() == 1) {
					return file;
				}
				return file.getProject();
			}
			return null;
		}
		if (o instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) o;
			IResource adapter = (IResource) adaptable.getAdapter(IResource.class);
			if (adapter != null) {
				return adapter;
			}
			adapter = (IResource) adaptable.getAdapter(IFile.class);
			if (adapter != null) {
				return adapter;
			}
		}
		return (IResource) Platform.getAdapterManager().getAdapter(o, IResource.class);
	}

	public static void touch(final IResource res) {
		Job job = new Job("Refresh for: " + res.getName()) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				// triggers the decoration update
				try {
					if (res.isAccessible()) {
						res.touch(monitor);
					}
				} catch (CoreException e) {
					MercurialEclipsePlugin.logError(e);
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	/**
	 * @param selection may be null
	 * @return never null , may be empty list containing all resources from given selection
	 */
	public static List<IResource> getResources(IStructuredSelection selection) {
		List<IResource> resources = new ArrayList<IResource>();
		List<?> list = selection.toList();
		for (Object object : list) {
			if(object instanceof ChangeSet) {
				Set<IFile> files = ((ChangeSet)object).getFiles();
				for (IFile file : files) {
					if(!resources.contains(file)) {
						resources.add(file);
					}
				}
			} else if(object instanceof PathFromChangeSet) {
				PathFromChangeSet pathFromChangeSet = (PathFromChangeSet) object;
				Set<FileFromChangeSet> files = pathFromChangeSet.getFiles();
				for (FileFromChangeSet ffc : files) {
					IFile file = ffc.getFile();
					if(file != null && !resources.contains(file)) {
						resources.add(file);
					}
				}

			} else {
				IResource resource = getResource(object);
				if(resource != null && !resources.contains(resource)){
					resources.add(resource);
				}
			}
		}
		return resources;
	}

	/**
	 * This is optimized version of {@link IPath#isPrefixOf(IPath)} (30-50% faster). Main difference is
	 * that we prefer the cheap operations first and check path segments starting from the
	 * end of the first path (with the assumption that paths starts in most cases
	 * with common paths segments => so we postpone redundant comparisons).
	 * @param first non null
	 * @param second non null
	 * @return true if the first path is prefix of the second
	 */
	public static boolean isPrefixOf(IPath first, IPath second) {
		int len = first.segmentCount();
		if (len > second.segmentCount()) {
			return false;
		}
		for (int i = len - 1; i >= 0; i--) {
			if (!first.segment(i).equals(second.segment(i))) {
				return false;
			}
		}
		return sameDevice(first, second);
	}

	private static boolean sameDevice(IPath first, IPath second) {
		String device = first.getDevice();
		if (device == null) {
			if (second.getDevice() != null) {
				return false;
			}
		} else {
			if (!device.equalsIgnoreCase(second.getDevice())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Opens appropriate editor
	 * @param file must be not null
	 * @param activePage can be null
	 * @return
	 */
	public static IEditorPart openEditor(IWorkbenchPage activePage, IFile file) {
		if(activePage == null) {
			activePage = MercurialEclipsePlugin.getActivePage();
		}
		try {
			if (file instanceof IHgResource) {
				return IDE.openEditor(activePage, file.getLocationURI(),
						EditorsUI.DEFAULT_TEXT_EDITOR_ID, true);
			}
			return IDE.openEditor(activePage, file);
		} catch (PartInitException e) {
			MercurialEclipsePlugin.logError(e);
			return null;
		}
	}
}
