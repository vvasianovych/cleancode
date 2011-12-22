/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov	- implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IPath;

import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author Andrei
 */
public class PathsSet {
	private static final int MAX_SEGMENTS = 20;
	private static final int MAX_NAME = 60;
	/**
	 * first index is the (truncated) segments size,
	 * second index is the (truncated) file name size
	 */
	private final Set<IPath> [][] pathsMap;
	private final int initialSize;
	private final float initialLoad;
	private int size;

	@SuppressWarnings("unchecked")
	public PathsSet(int initialSize, float initialLoad) {
		this.initialSize = initialSize;
		this.initialLoad = initialLoad;
		pathsMap = new Set[MAX_SEGMENTS][MAX_NAME];
	}

	public int size(){
		return size;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public List<IPath> getChildren(IPath parent){
		List<IPath> result = null;
		int segmentCount = parent.segmentCount();
		// empty or root paths shouldn't be tracked.
		if(segmentCount == 0) {
			return null;
		}
		int pathKey = categorize(parent.lastSegment());
		int startSegment = categorize(segmentCount) + 1;
		if(startSegment == MAX_SEGMENTS){
			startSegment --;
		}
		synchronized (pathsMap) {
			for (int i = startSegment; i < MAX_SEGMENTS; i++) {
				for (int j = 0; j < MAX_NAME; j++) {
					Set<IPath> possibleChildren = pathsMap[i][j];
					if (possibleChildren == null) {
						continue;
					}
					for (IPath path : possibleChildren) {
						if (pathKey == categorize(path.segment(segmentCount - 1))
								&& (ResourceUtils.isPrefixOf(parent, path))) {
							if (result == null) {
								result = new ArrayList<IPath>();
							}
							result.add(path);
						}
					}
				}
			}
		}
		return result;
	}

	public List<IPath> getDirectChildren(IPath parent) {
		List<IPath> result = null;
		int segmentCount = parent.segmentCount();
		// empty or root paths shouldn't be tracked.
		if(segmentCount == 0) {
			return null;
		}
		int pathKey = categorize(parent.lastSegment());
		int startSegment = categorize(segmentCount) + 1;
		if(startSegment == MAX_SEGMENTS){
			startSegment --;
		}
		synchronized (pathsMap) {
			for (int j = 0; j < MAX_NAME; j++) {
				Set<IPath> possibleChildren = pathsMap[startSegment][j];
				if (possibleChildren == null) {
					continue;
				}
				for (IPath path : possibleChildren) {
					if (pathKey == categorize(path.segment(segmentCount - 1))
							&& (ResourceUtils.isPrefixOf(parent, path))) {
						if (result == null) {
							result = new ArrayList<IPath>();
						}
						result.add(path);
					}
				}
			}
		}
		return result;
	}

	public void add(IPath path){
		int sizeKey = categorize(path.segmentCount());
		int pathKey = categorize(path.lastSegment());
		synchronized (pathsMap){
			Set<IPath> paths = pathsMap[sizeKey][pathKey];
			if(paths == null){
				paths = new LinkedHashSet<IPath>(initialSize, initialLoad);
				pathsMap[sizeKey][pathKey] = paths;
			}
			paths.add(path);
			size ++;
		}
	}

	public boolean contains(IPath path){
		int sizeKey = categorize(path.segmentCount());
		int pathKey = categorize(path.lastSegment());
		synchronized (pathsMap){
			Set<IPath> paths = pathsMap[sizeKey][pathKey];
			if(paths == null){
				return false;
			}
			return paths.contains(path);
		}
	}

	public void remove(IPath path){
		int sizeKey = categorize(path.segmentCount());
		int pathKey = categorize(path.lastSegment());
		synchronized (pathsMap){
			Set<IPath> paths = pathsMap[sizeKey][pathKey];
			if(paths == null){
				return;
			}
			paths.remove(path);
			if(paths.isEmpty()){
				pathsMap[sizeKey][pathKey] = null;
			}
			size --;
		}
	}

	private static int categorize(int segmentsCount){
		if(segmentsCount < MAX_SEGMENTS){
			return segmentsCount;
		}
		return MAX_SEGMENTS - 1;
	}

	private static int categorize(String pathSegment){
		if(pathSegment == null){
			return 0;
		}
		int length = pathSegment.length();
		if(length < MAX_NAME){
			return length;
		}
		return 0;
	}

}
