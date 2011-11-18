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
package com.vectrace.MercurialEclipse.synchronize.cs;

import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.mapping.ISynchronizationScopeParticipant;
import org.eclipse.team.core.mapping.ISynchronizationScopeParticipantFactory;

import com.vectrace.MercurialEclipse.synchronize.MercurialSynchronizeSubscriber;
import com.vectrace.MercurialEclipse.synchronize.RepositorySynchronizationScope;

/**
 * @author Andrei
 *
 */
public class HgChangeSetModelProvider extends ModelProvider {

	public static final String ID = "com.vectrace.MercurialEclipse.changeSetModel";
	private boolean participantCreated;
	private MercurialSynchronizeSubscriber subscriber;

	public HgChangeSetModelProvider() {
		super();
	}

	public boolean isParticipantCreated() {
		return participantCreated;
	}

	public MercurialSynchronizeSubscriber getSubscriber() {
		return subscriber;
	}

	public static class HgModelScopeParticipantFactory implements
			ISynchronizationScopeParticipantFactory, IAdapterFactory {

		public HgModelScopeParticipantFactory() {

		}

		public ISynchronizationScopeParticipant createParticipant(ModelProvider provider1,
				ISynchronizationScope scope) {
			HgChangeSetModelProvider modelProvider = (HgChangeSetModelProvider) provider1;
			RepositorySynchronizationScope rscope = (RepositorySynchronizationScope) scope;
			MercurialSynchronizeSubscriber subscriber = rscope.getSubscriber();
			modelProvider.participantCreated = true;
			modelProvider.subscriber = subscriber;
			return subscriber.getParticipant();
		}

		@SuppressWarnings("unchecked")
		public Object getAdapter(Object adaptableObject, Class adapterType) {
			if (adaptableObject instanceof ModelProvider) {
				ModelProvider provider1 = (ModelProvider) adaptableObject;
				if (provider1.getDescriptor().getId().equals(ID)) {
//					if (adapterType == IResourceMappingMerger.class) {
//						return new DefaultResourceMappingMerger((ModelProvider)adaptableObject);
//					}
					if (adapterType == ISynchronizationScopeParticipantFactory.class) {
						return this;
					}
				}
			}
			return null;
		}

		@SuppressWarnings("unchecked")
		public Class[] getAdapterList() {
			return new Class[] {
//					IResourceMappingMerger.class,
//					ISynchronizationCompareAdapter.class,
					ISynchronizationScopeParticipantFactory.class
				};
		}

	}

}
