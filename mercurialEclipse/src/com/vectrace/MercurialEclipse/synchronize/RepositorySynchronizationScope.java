/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.mapping.ISynchronizationScopeChangeListener;
import org.eclipse.team.internal.core.mapping.AbstractResourceMappingScope;
import org.eclipse.team.internal.ui.Utils;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.synchronize.cs.HgChangeSetModelProvider;

/**
 * @author Andrei
 */
public class RepositorySynchronizationScope extends AbstractResourceMappingScope {

	private final IProject[] roots;
	private final ListenerList listeners;
	private final IHgRepositoryLocation repo;
	private MercurialSynchronizeSubscriber subscriber;
	private HgChangeSetModelProvider provider;

	public RepositorySynchronizationScope(IHgRepositoryLocation repo, IProject[] roots) {
		Assert.isNotNull(repo);
		this.repo = repo;
		if(roots != null) {
			this.roots = roots;
		} else {
			Set<IProject> projects = MercurialEclipsePlugin.getRepoManager().getAllRepoLocationProjects(repo);
			this.roots = projects.toArray(new IProject[projects.size()]);
		}
		listeners = new ListenerList(ListenerList.IDENTITY);
	}

	@Override
	public void addScopeChangeListener(ISynchronizationScopeChangeListener listener) {
		listeners.add(listener);
	}

	public ISynchronizationScope asInputScope() {
		return this;
	}

	@Override
	public boolean contains(IResource resource) {
		ResourceTraversal[] traversals = getTraversals();
		if(traversals == null){
			return false;
		}
		for (ResourceTraversal traversal : traversals) {
			if (traversal.contains(resource)) {
				return true;
			}
		}
		return false;
	}

	public ResourceMappingContext getContext() {
		// TODO unclear
		return ResourceMappingContext.LOCAL_CONTEXT;
	}

	public ResourceMapping[] getInputMappings() {
		return Utils.getResourceMappings(getRoots());
	}

	@Override
	public ResourceMapping getMapping(Object modelObject) {
		ResourceMapping[] mappings = getMappings();
		for (ResourceMapping mapping : mappings) {
			if (mapping.getModelObject().equals(modelObject)) {
				return mapping;
			}
		}
		return null;
	}

	public ResourceMapping[] getMappings() {
		return getInputMappings();
	}

	@Override
	public ResourceMapping[] getMappings(String modelProviderId) {
		if(!isSupportedModelProvider(modelProviderId)){
			return null;
		}
		Set<ResourceMapping> result = new HashSet<ResourceMapping>();
		ResourceMapping[] mappings = getMappings();
		for (ResourceMapping mapping : mappings) {
			if (mapping.getModelProviderId().equals(modelProviderId)) {
				result.add(mapping);
			}
		}
		return result.toArray(new ResourceMapping[result.size()]);
	}

	private boolean isSupportedModelProvider(String modelProviderId) {
		return ModelProvider.RESOURCE_MODEL_PROVIDER_ID.equals(modelProviderId)
			|| HgChangeSetModelProvider.ID.equals(modelProviderId);
	}

	@Override
	public ModelProvider[] getModelProviders() {
		Set<ModelProvider> result = new HashSet<ModelProvider>();

		ResourceMapping[] mappings = getMappings();
		for (ResourceMapping mapping : mappings) {
			ModelProvider modelProvider = mapping.getModelProvider();
			if (modelProvider != null && isSupportedModelProvider(modelProvider.getId())) {
				result.add(modelProvider);
			}
		}
		result.add(getChangesetProvider());
		return result.toArray(new ModelProvider[result.size()]);
	}

	public HgChangeSetModelProvider getChangesetProvider() {
		if (provider == null) {
			try {
				provider = (HgChangeSetModelProvider) ModelProvider.getModelProviderDescriptor(HgChangeSetModelProvider.ID)
						.getModelProvider();
			} catch (CoreException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}
		return provider;
	}

	public IProject[] getProjects() {
		Set<IProject> projects = new HashSet<IProject>();
		for (IProject res : roots) {
			projects.add(res);
		}
		return projects.toArray(new IProject[projects.size()]);
	}

	@Override
	public IProject[] getRoots() {
		return roots;
	}

	public ResourceTraversal[] getTraversals() {
		return new ResourceTraversal[] {
				new ResourceTraversal(getRoots(), IResource.DEPTH_INFINITE, IContainer.EXCLUDE_DERIVED) };
	}

	public ResourceTraversal[] getTraversals(ResourceMapping mapping) {
		try {
			return mapping.getTraversals(getContext(), new NullProgressMonitor());
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
			return null;
		}
	}

	public boolean hasAdditionalMappings() {
		return false;
	}

	public boolean hasAdditonalResources() {
		return false;
	}

	public void refresh(ResourceMapping[] mappings) {
		if(!listeners.isEmpty()){
			Object[] objects = listeners.getListeners();
			for (Object object : objects) {
				((ISynchronizationScopeChangeListener) object).scopeChanged(this, mappings, getTraversals());
			}
		}
	}

	@Override
	public void removeScopeChangeListener(ISynchronizationScopeChangeListener listener) {
		listeners.remove(listener);
	}

	public IHgRepositoryLocation getRepositoryLocation() {
		return repo;
	}

	public void setSubscriber(MercurialSynchronizeSubscriber mercurialSynchronizeSubscriber) {
		this.subscriber = mercurialSynchronizeSubscriber;
	}

	public MercurialSynchronizeSubscriber getSubscriber() {
		return subscriber;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RepositorySynchronizationScope [");
		if (repo != null) {
			builder.append("repo=");
			builder.append(repo);
			builder.append(", ");
		}
		if (roots != null) {
			builder.append("roots=");
			builder.append(Arrays.toString(roots));
		}
		builder.append("]");
		return builder.toString();
	}


}
