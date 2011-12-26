/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vectrace.MercurialEclipse.model.GChangeSet.Edge.EdgeType;

/**
 * Don't look at this code - your eyes will bleed.
 *
 * Seriously. Turn back now.
 */
public class GChangeSet {
	private final EdgeList middle;
	private final EdgeList after;
	private final int index;
	private final RowCount rowCount;
	private final int rev;

	public GChangeSet(RowCount rowCount, int index, String middleS,
			String afterS) {
		this.rowCount = rowCount;
		this.index = index;
		middle = new EdgeList(true);
		after = new EdgeList(false);
		middle.parse(middleS);
		after.parse(afterS);
		if (middleS != null && middleS.length() > 0) {
			rev = Integer.parseInt(middleS.substring(middleS.indexOf('*') + 1));
		} else {
			rev = 0;
		}
	}

	public int getRev() {
		return rev;
	}

	public GChangeSet clean(GChangeSet last) {
		middle.clean(last);
		return this;
	}

	public int getIndex() {
		return index;
	}

	public EdgeList getBefore() {
		return getMiddle();
	}

	public EdgeList getMiddle() {
		return middle;
	}

	public EdgeList getAfter() {
		return after;
	}

	public class EdgeList {

		private final List<Edge> edges = new ArrayList<Edge>();
		private final Set<Integer> above = new HashSet<Integer>();
		private final boolean straight;
		private int[] jumps;

		public EdgeList(boolean straight) {
			this.straight = straight;
		}

		public void parse(String string) {
			int length = string.length();
			int count = 0;
			for (int i = 0; i < length; i++) {
				if (string.charAt(i) != '*') {
					count += addEdge(string, i, count);
				} else {
					break;
				}
			}
			rowCount.endRow();
			if (string.indexOf('+') >= 0) {
				rowCount.jump = string.indexOf('o');
				jumps = new int[] { string.indexOf('+') / 2, rowCount.jump / 2, };
			}
		}

		private int addEdge(String string, int i, int count) {
			char c = string.charAt(i);
			if (c == ' ') {
				return rowCount.space(i, count);
			}
			Edge edge = new Edge(c, count);
			return rowCount.update(this, edge);
		}

		public int[] getJump() {
			return jumps;
		}

		public void add(Edge edge) {
			if (straight) {
				edge.straighten();
			}
			above.add(Integer.valueOf(edge.bottom));
			edges.add(edge);
		}

		public List<Edge> getEdges() {
			return edges;
		}

		public void clean(GChangeSet last) {
			for (Edge e : edges) {
				e.setFinish(e.isDot()
						&& (last == null || !last.after.above.contains(Integer.valueOf(e.top))));
			}
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("EdgeList [");
			if (above != null) {
				builder.append("above=");
				builder.append(above);
				builder.append(", ");
			}
			if (edges != null) {
				builder.append("edges=");
				builder.append(edges);
				builder.append(", ");
			}
			if (jumps != null) {
				builder.append("jumps=");
				builder.append(Arrays.toString(jumps));
				builder.append(", ");
			}
			builder.append("straight=");
			builder.append(straight);
			builder.append("]");
			return builder.toString();
		}

	}

	public static class RowCount {
		public int jump;
		public List<Integer> cols = new ArrayList<Integer>();
		private int unique;
		private Edge lastEdge;
		private int dec = -1;

		public RowCount() {
			cols.add(Integer.valueOf(0));
		}

		public int space(int i, int count) {
			lastEdge = null;
			if (jump == i) {
				dec = count;
				return 1;
			}
			return 0;
		}

		public int update(EdgeList edges, Edge edge) {
			Integer col;
			boolean lastLine = lastEdge != null
					&& lastEdge.type == EdgeType.line;
			int count = 1;
			if (edge.type == EdgeType.backslash && lastLine) {
				unique++;
				cols.add(edge.col, col = Integer.valueOf(unique));
			} else if (edge.type == EdgeType.slash && lastLine) {
				dec = edge.col;
				col = cols.get(edge.col);
			} else if (edge.type == EdgeType.line && lastEdge != null
					&& lastEdge.type == EdgeType.backslash) {
				count = 0;
				edge.dec();
				cols.remove(edge.col);
				col = cols.get(edge.col);
			} else if (edge.type == EdgeType.line && lastEdge != null
					&& lastEdge.type == EdgeType.slash) {
				count = 0;
				edge.dec();
				col = cols.get(edge.col);
				dec = -1;
			} else if (edge.type == EdgeType.dash
					&& (lastEdge == null || lastEdge.type != EdgeType.dash)) {
				lastEdge = edge;
				return 0;
			} else if (edge.col >= cols.size()) {
				unique++;
				cols.add(col = Integer.valueOf(unique));
			} else {
				col = cols.get(edge.col);
			}
			edge.lane = col.intValue();
			if (edge.type == EdgeType.dash) {
				lastEdge = null;
			} else {
				lastEdge = edge;
			}
			edges.add(edge);
			return count;
		}

		public void endRow() {
			lastEdge = null;
			if (dec > -1) {
				cols.remove(dec);
				dec = -1;
			}
			jump = -1;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("RowCount [");
			if (cols != null) {
				builder.append("cols=");
				builder.append(cols);
				builder.append(", ");
			}
			builder.append("dec=");
			builder.append(dec);
			builder.append(", jump=");
			builder.append(jump);
			builder.append(", ");
			if (lastEdge != null) {
				builder.append("lastEdge=");
				builder.append(lastEdge);
				builder.append(", ");
			}
			builder.append("unique=");
			builder.append(unique);
			builder.append("]");
			return builder.toString();
		}


	}

	public static class Edge {
		public static enum EdgeType {
			line, dot, working, plus, dash, slash, backslash
		}

		private int top;
		private int bottom;
		private int col;
		private int lane;
		private boolean finish;
		private EdgeType type;

		public Edge(char c, int i) {
			col = top = bottom = i;
			type = EdgeType.line;
			switch (c) {
			case '/':
				type = EdgeType.slash;
				bottom--;
				break;
			case '\\':
				type = EdgeType.backslash;
				top--;
				break;
			case 'o':
				type = EdgeType.dot;
				break;
			case '@':
				type = EdgeType.working;
				break;
			case '+':
				type = EdgeType.plus;
				break;
			case '-':
				type = EdgeType.dash;
				break;
			}
		}

		public void setFinish(boolean finish) {
			this.finish = finish;
		}

		public boolean isFinish() {
			return finish;
		}

		public void straighten() {
			top = bottom = Math.max(top, bottom);
		}

		public void dec() {
			top = bottom = col = col - 1;
		}

		public int getTop() {
			return top;
		}

		public int getBottom() {
			return bottom;
		}

		public int getLane() {
			return lane;
		}

		public boolean isDot() {
			return type == EdgeType.dot || type == EdgeType.working
					|| type == EdgeType.plus;
		}

		public boolean isPlus() {
			return type == EdgeType.plus;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Edge [bottom=");
			builder.append(bottom);
			builder.append(", col=");
			builder.append(col);
			builder.append(", finish=");
			builder.append(finish);
			builder.append(", lane=");
			builder.append(lane);
			builder.append(", top=");
			builder.append(top);
			builder.append(", ");
			if (type != null) {
				builder.append("type=");
				builder.append(type);
			}
			builder.append("]");
			return builder.toString();
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GChangeSet [rev=");
		builder.append(rev);
		builder.append(", index=");
		builder.append(index);
		builder.append(", ");
		if (rowCount != null) {
			builder.append("rowCount=");
			builder.append(rowCount);
		}
		builder.append("]");
		return builder.toString();
	}
}
