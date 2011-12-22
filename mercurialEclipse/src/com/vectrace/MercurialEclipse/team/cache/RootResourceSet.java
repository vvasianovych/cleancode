/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * lordofthepigs	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.model.HgRoot;

final class RootResourceSet<T extends IResource> {

	private final Map<HgRoot, Set<T>> resources = new HashMap<HgRoot, Set<T>>();

	RootResourceSet(){
		super();
	}

	public void add(HgRoot root, T resource){
		if(root == null){
			throw new IllegalArgumentException("HgRoot is null");
		}
		Set<T> set = resources.get(root);
		if(set == null){
			set = new HashSet<T>();
			resources.put(root, set);
		}
		set.add(resource);
	}

	public void addAll(RootResourceSet<T> that){
		for(Map.Entry<HgRoot, Set<T>> entry : that.resources.entrySet()){
			if(resources.containsKey(entry.getKey())){
				resources.get(entry.getKey()).addAll(entry.getValue());
			}else{
				resources.put(entry.getKey(), entry.getValue());
			}
		}
	}

	public boolean contains(Object resource){
		for(Set<T> set : this.resources.values()){
			if(set.contains(resource)){
				return true;
			}
		}

		return false;
	}

	public int size(){
		int size = 0;

		for(Set<T> set :resources.values()){
			size += set.size();
		}

		return size;
	}

	public boolean isEmpty(){
		return this.size() == 0;
	}

	public void clear(){
		this.resources.clear();
	}

	public HgRoot rootOf(T res){
		for(Map.Entry<HgRoot, Set<T>> entry : this.resources.entrySet()){
			if(entry.getValue().contains(res)){
				return entry.getKey();
			}
		}
		return null;
	}

	public Set<Map.Entry<HgRoot, Set<T>>> entrySet(){
		return this.resources.entrySet();
	}

	public Set<HgRoot> roots(){
		return this.resources.keySet();
	}

	public Set<T> getResources(HgRoot root){
		return this.resources.get(root);
	}

	public boolean remove(Object res){
		Iterator<Set<T>> iter = this.resources.values().iterator();
		while(iter.hasNext()){
			Set<T> set = iter.next();
			if(set.remove(res)){
				if(set.size() == 0){
					// no more resources under this root, remove the root itself
					iter.remove();
				}
				return true;
			}
		}
		return false;
	}

	public Iterator<T> resourceIterator(){
		return new ResourceIterator();
	}

	@Override
	public int hashCode(){
		return 23 * this.resources.hashCode();
	}

	@Override
	public boolean equals(Object o){
		if(o == null || !o.getClass().equals(this.getClass())){
			return false;
		}
		RootResourceSet<?> that = (RootResourceSet<?>)o;
		return this.resources.equals(that.resources);
	}

	private class ResourceIterator implements Iterator<T> {

		private final Iterator<Set<T>> rootIterator;
		private Set<T> currentSet;
		private Iterator<T> setIterator;

		public ResourceIterator(){
			rootIterator = resources.values().iterator();
		}

		public boolean hasNext() {
			if(setIterator != null && setIterator.hasNext()){
				return true;
			}
			return rootIterator.hasNext();
		}

		public T next() {
			if(setIterator != null && setIterator.hasNext()){
				return setIterator.next();
			}
			currentSet = rootIterator.next();
			setIterator = currentSet.iterator();
			return setIterator.next();
		}

		public void remove() {
			if(setIterator != null){
				setIterator.remove();
				if(currentSet.size() == 0){
					rootIterator.remove();
				}
			}
		}
	}
}