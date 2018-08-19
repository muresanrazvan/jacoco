/*******************************************************************************
 * Copyright (c) 2009, 2018 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Evgeny Mandrikov - initial API and implementation
 *    
 *******************************************************************************/
package org.jacoco.core.internal.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.internal.analysis.filter.IFilterOutput;
import org.jacoco.core.internal.flow.Instruction;
import org.objectweb.asm.tree.AbstractInsnNode;

/**
 * Calculates the filtered coverage of a single method.
 */
class MethodCoverageCalculator implements IFilterOutput {

	private final Map<AbstractInsnNode, Instruction> instructions;

	private final Set<AbstractInsnNode> ignored;

	/**
	 * Instructions that should be merged form disjoint sets. Coverage
	 * information from instructions of one set will be merged into
	 * representative instruction of set.
	 * 
	 * Each such set is represented as a singly linked list: each element except
	 * one references another element from the same set, element without
	 * reference - is a representative of this set.
	 * 
	 * This map stores reference (value) for elements of sets (key).
	 */
	private final Map<AbstractInsnNode, AbstractInsnNode> merged;

	private final Map<AbstractInsnNode, Set<AbstractInsnNode>> replacements;

	MethodCoverageCalculator(
			final Map<AbstractInsnNode, Instruction> instructions) {
		this.instructions = instructions;
		this.ignored = new HashSet<AbstractInsnNode>();
		this.merged = new HashMap<AbstractInsnNode, AbstractInsnNode>();
		this.replacements = new HashMap<AbstractInsnNode, Set<AbstractInsnNode>>();
	}

	void calculateCoverage(final MethodCoverageImpl coverage) {

		// Merge:
		for (final Map.Entry<AbstractInsnNode, Instruction> i : instructions
				.entrySet()) {
			final AbstractInsnNode m = i.getKey();
			final AbstractInsnNode r = findRepresentative(m);
			if (r != m) {
				ignored.add(m);
				instructions.get(r).merge(i.getValue());
			}
		}

		// Report result:
		for (final Map.Entry<AbstractInsnNode, Instruction> i : instructions
				.entrySet()) {
			if (ignored.contains(i.getKey())) {
				continue;
			}

			final Instruction insn = i.getValue();

			final int total;
			final int covered;
			final Set<AbstractInsnNode> r = replacements.get(i.getKey());
			if (r != null) {
				int cb = 0;
				for (final AbstractInsnNode b : r) {
					if (instructions.get(b).getCoveredBranches() > 0) {
						cb++;
					}
				}
				total = r.size();
				covered = cb;
			} else {
				total = insn.getBranches();
				covered = insn.getCoveredBranches();
			}

			final ICounter instrCounter = covered == 0 ? CounterImpl.COUNTER_1_0
					: CounterImpl.COUNTER_0_1;
			final ICounter branchCounter = total > 1
					? CounterImpl.getInstance(total - covered, covered)
					: CounterImpl.COUNTER_0_0;
			coverage.increment(instrCounter, branchCounter, insn.getLine());
		}
		coverage.incrementMethodCounter();
	}

	private AbstractInsnNode findRepresentative(AbstractInsnNode i) {
		AbstractInsnNode r = merged.get(i);
		while (r != null) {
			i = r;
			r = merged.get(i);
		}
		return i;
	}

	// === IFilterOutput API ===

	public void ignore(final AbstractInsnNode fromInclusive,
			final AbstractInsnNode toInclusive) {
		for (AbstractInsnNode i = fromInclusive; i != toInclusive; i = i
				.getNext()) {
			ignored.add(i);
		}
		ignored.add(toInclusive);
	}

	public void merge(AbstractInsnNode i1, AbstractInsnNode i2) {
		i1 = findRepresentative(i1);
		i2 = findRepresentative(i2);
		if (i1 != i2) {
			merged.put(i2, i1);
		}
	}

	public void replaceBranches(final AbstractInsnNode source,
			final Set<AbstractInsnNode> newTargets) {
		replacements.put(source, newTargets);
	}

}
