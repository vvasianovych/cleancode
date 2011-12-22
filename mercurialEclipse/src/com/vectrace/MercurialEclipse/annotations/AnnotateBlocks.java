/*******************************************************************************
 * Copyright (c)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Charles O'Farrell - implementation (based on subclipse)
 *     Stefan C                  - Code cleanup
 *******************************************************************************/

package com.vectrace.MercurialEclipse.annotations;

import java.util.LinkedList;
import java.util.List;

public class AnnotateBlocks {
	private final List<AnnotateBlock> blocks = new LinkedList<AnnotateBlock>();

	/**
	 * Add an annotate block merging this block with the previous block if it is part of the same
	 * change.
	 *
	 * @param aBlock
	 */
	public void add(AnnotateBlock aBlock) {

		int size = blocks.size();
		if (size == 0) {
			blocks.add(aBlock);
		} else {
			AnnotateBlock lastBlock = blocks.get(size - 1);
			if (lastBlock.getRevision().equals(aBlock.getRevision())) {
				lastBlock.setEndLine(aBlock.getStartLine());
			} else {
				blocks.add(aBlock);
			}
		}
	}

	public List<AnnotateBlock> getAnnotateBlocks() {
		return blocks;
	}

}
