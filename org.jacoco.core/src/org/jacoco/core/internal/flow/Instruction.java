/*******************************************************************************
 * Copyright (c) 2009, 2018 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *    
 *******************************************************************************/
package org.jacoco.core.internal.flow;

import java.util.BitSet;

/**
 * Execution status of a single byte code instruction internally used for
 * coverage analysis. The execution status is recorded separately for each
 * outgoing branch. Each instructions has at least one branch, for example in
 * case of a simple sequence of instructions (by convention branch 0).
 * 
 * The executions status can either be directly derived from a probe which has
 * been inserted in the execution flow. Or indirectly propagated along the
 * branches (i.e. edges of the CFG).
 */
public class Instruction {

	private final int line;

	private int branches;

	private final BitSet coveredBranches;

	private Instruction predecessor;

	private int predecessorBranch;

	/**
	 * New instruction at the given line.
	 * 
	 * @param line
	 *            source line this instruction belongs to
	 */
	public Instruction(final int line) {
		this.line = line;
		this.branches = 0;
		this.coveredBranches = new BitSet();
	}

	/**
	 * Adds a branch to this instruction which execution status is indirectly
	 * derived from the execution status of the target instruction. In case the
	 * branch is covered the status is propagated also to the predecessors of
	 * this instruction.
	 * 
	 * Note: This method is not idempotent and must be called exactly once for
	 * every branch
	 * 
	 * @param target
	 *            target instruction of this branch
	 * @param branch
	 *            branch identifier unique for this instruction
	 */
	public void addBranch(final Instruction target, final int branch) {
		branches++;
		target.predecessor = this;
		target.predecessorBranch = branch;
		if (!target.coveredBranches.isEmpty()) {
			propagateExecutedBranch(this, branch);
		}
	}

	/**
	 * Adds a branch to this instruction which execution status is directly
	 * derived from a probe. In case the branch is covered the status is
	 * propagated also to the predecessors of this instruction.
	 * 
	 * Note: This method is not idempotent and must be called exactly once for
	 * every branch
	 * 
	 * @param executed
	 *            whether the corresponding probe has been executed
	 * @param branch
	 *            branch identifier unique for this instruction
	 */
	public void addBranch(final boolean executed, final int branch) {
		branches++;
		if (executed) {
			propagateExecutedBranch(this, branch);
		}
	}

	private static void propagateExecutedBranch(Instruction insn, int branch) {
		// No recursion here, as there can be very long chains of instructions
		while (insn != null) {
			if (!insn.coveredBranches.isEmpty()) {
				insn.coveredBranches.set(branch);
				break;
			}
			insn.coveredBranches.set(branch);
			branch = insn.predecessorBranch;
			insn = insn.predecessor;
		}
	}

	/**
	 * Returns the source line this instruction belongs to.
	 * 
	 * @return corresponding source line
	 */
	public int getLine() {
		return line;
	}

	/**
	 * Returns the total number of branches starting from this instruction.
	 * 
	 * @return total number of branches
	 */
	public int getBranches() {
		return branches;
	}

	/**
	 * Returns the number of covered branches starting from this instruction.
	 * 
	 * @return number of covered branches
	 */
	public int getCoveredBranches() {
		return coveredBranches.cardinality();
	}

	/**
	 * Merges information about covered branches of given instruction into this
	 * instruction.
	 * 
	 * @param instruction
	 *            instruction from which to merge
	 */
	public void merge(final Instruction instruction) {
		this.coveredBranches.or(instruction.coveredBranches);
	}

}
