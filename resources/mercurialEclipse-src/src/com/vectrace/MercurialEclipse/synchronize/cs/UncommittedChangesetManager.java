/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.cs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.IPreferenceStore;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.IUncommittedChangesetManager;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.utils.StringUtils;

/**
 * @author Andrei
 */
public class UncommittedChangesetManager implements IUncommittedChangesetManager {

	private static final String PATH_NAME_SEPARATOR = "=";

	private static final String MAPPINGS_SEPARATOR = ";";

	private static final String KEY_FILES_PER_PROJECT_PREFIX = "projectFiles/";
	private static final String KEY_CS_COMMENT_PREFIX = "changesetComment/";
	private static final String KEY_CS_DEFAULT = "changesetIsDefault/";
	private static final String KEY_CS_LIST = "changesets";

	private boolean loadingFromPrefs;

	private WorkingChangeSet defaultChangeset;
	private IProject[] projects;

	private final HgChangeSetContentProvider provider;

	private final UncommittedChangesetGroup group;

	public UncommittedChangesetManager(HgChangeSetContentProvider provider) {
		this.provider = provider;
		group = new UncommittedChangesetGroup(this);
//		defaultChangeset = createDefaultChangeset();
	}

	protected WorkingChangeSet createDefaultChangeset() {
		WorkingChangeSet changeset = new WorkingChangeSet("Default Changeset", group);
		changeset.setDefault(true);
		changeset.setComment("(no commit message)");
		return changeset;
	}

	public UncommittedChangesetGroup getUncommittedGroup(){
		return group;
	}

	private void loadSavedChangesets(){
		Set<WorkingChangeSet> sets = new HashSet<WorkingChangeSet>();
		loadfromPreferences(sets);
		assignRemainingFiles();
	}

	private void assignRemainingFiles() {
		if(projects == null) {
			return;
		}
		group.update(null, null);
	}

	public void storeChangesets(){
		if(loadingFromPrefs) {
			return;
		}
		IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();
		Set<ChangeSet> changesets = group.getChangesets();
		Map<IProject, Map<IFile, String>> projectMap = new HashMap<IProject, Map<IFile,String>>();
		StringBuilder changesetNames = new StringBuilder();
		for (ChangeSet changeSet : changesets) {
			String name = changeSet.getName();
			String comment = changeSet.getComment();
			changesetNames.append(name).append(MAPPINGS_SEPARATOR);
			store.putValue(KEY_CS_COMMENT_PREFIX + name, comment);
			if(((WorkingChangeSet)changeSet).isDefault()) {
				store.putValue(KEY_CS_DEFAULT, name);
			}
			Set<IFile> files = changeSet.getFiles();
			for (IFile file : files) {
				IProject project = file.getProject();
				Map<IFile, String> fileToChangeset = projectMap.get(project);
				if(fileToChangeset == null){
					fileToChangeset = new HashMap<IFile, String>();
					projectMap.put(project, fileToChangeset);
				}
				fileToChangeset.put(file, name);
			}
		}
		store.putValue(KEY_CS_LIST, changesetNames.toString());

		Set<Entry<IProject,Map<IFile,String>>> entrySet = projectMap.entrySet();
		for (Entry<IProject, Map<IFile, String>> entry : entrySet) {
			IProject project = entry.getKey();
			Map<IFile, String> fileToChangeset = entry.getValue();
			store.putValue(KEY_FILES_PER_PROJECT_PREFIX + project.getName(), encode(fileToChangeset));
		}
	}

	public WorkingChangeSet getDefaultChangeset(){
		if(defaultChangeset == null) {
			defaultChangeset = createDefaultChangeset();
		}
		return defaultChangeset;
	}

	private void loadfromPreferences(Set<WorkingChangeSet> sets) {
		loadingFromPrefs = true;
		try {
			IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();
			String changesets = store.getString(KEY_CS_LIST);
			String defName = store.getString(KEY_CS_DEFAULT);
			if(!StringUtils.isEmpty(changesets)){
				String[] names = changesets.split(MAPPINGS_SEPARATOR);
				for (String name : names) {
					if(!StringUtils.isEmpty(name)) {
						WorkingChangeSet changeset = new WorkingChangeSet(name, group);
						sets.add(changeset);
					}
				}
			}
			for (WorkingChangeSet changeSet : sets) {
				String comment = store.getString(KEY_CS_COMMENT_PREFIX + changeSet.getName());
				changeSet.setComment(comment);
				if(changeSet.getName().equals(defName)) {
					makeDefault(changeSet);
				}
			}
			if(projects == null){
				return;
			}
			for (IProject project : projects) {
				String filesStr = store.getString(KEY_FILES_PER_PROJECT_PREFIX + project.getName());
				if(StringUtils.isEmpty(filesStr)){
					continue;
				}
				Map<IFile, String> fileToChangeset = decode(filesStr, project);
				Set<Entry<IFile,String>> entrySet = fileToChangeset.entrySet();
				for (Entry<IFile, String> entry : entrySet) {
					String name = entry.getValue();
					WorkingChangeSet changeset = getChangeset(name, sets);
					if(changeset == null){
						continue;
						//					changeset = new WorkingChangeSet(name, group);
						//					sets.add(changeset);
					}
					changeset.add(entry.getKey());
				}
			}
		} finally {
			loadingFromPrefs = false;
		}
	}

	private static WorkingChangeSet getChangeset(String name, Set<WorkingChangeSet> sets){
		for (WorkingChangeSet set : sets) {
			if(name.equals(set.getName())){
				return set;
			}
		}
		return null;
	}

	/**
	 * @param filesStr non null string encoded like "(project_rel_path=changeset_name;)*"
	 * @param project
	 * @return
	 */
	private static Map<IFile, String> decode(String filesStr, IProject project) {
		Map<IFile, String> fileToChangeset = new HashMap<IFile, String>();
		String[] mappings = filesStr.split(MAPPINGS_SEPARATOR);
		for (String mapping : mappings) {
			if(StringUtils.isEmpty(mapping)){
				continue;
			}
			String[] pathAndName = mapping.split(PATH_NAME_SEPARATOR);
			if(pathAndName.length != 2){
				continue;
			}
			IFile file = project.getFile(new Path(pathAndName[0]));
			if(file != null) {
				fileToChangeset.put(file, pathAndName[1]);
			}
		}
		return fileToChangeset;
	}

	private static String encode(Map<IFile, String> fileToChangeset){
		Set<Entry<IFile,String>> entrySet = fileToChangeset.entrySet();
		StringBuilder sb = new StringBuilder();
		for (Entry<IFile, String> entry : entrySet) {
			IFile file = entry.getKey();
			String changesetName = entry.getValue();
			sb.append(file.getProjectRelativePath()).append(PATH_NAME_SEPARATOR).append(changesetName);
			sb.append(MAPPINGS_SEPARATOR);
		}
		return sb.toString();
	}

	public void setProjects(IProject[] projects) {
		this.projects = projects;
		loadSavedChangesets();
		if(projects != null){
			assignRemainingFiles();
		}
		MercurialStatusCache.getInstance().addObserver(group);
	}

	public IProject[] getProjects() {
		return projects;
	}

	public void makeDefault(WorkingChangeSet set) {
		if(set == null || !group.getChangesets().contains(set)){
			return;
		}
		if(defaultChangeset != null) {
			defaultChangeset.setDefault(false);
		}
		defaultChangeset = set;
		defaultChangeset.setDefault(true);
		group.changesetChanged(set);
	}
}
