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
package org.jacoco.core.internal.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jacoco.core.analysis.ISourceNode;
import org.jacoco.core.internal.flow.Instruction;
import org.jacoco.core.internal.flow.LabelInfo;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;

/**
 * Calculate the coverage status of every single byte code instruction based on
 * the control flow and the executed probes.
 */
class InstructionCoverageCalculator {

	/** Probe array of the class the analyzed method belongs to. */
	private final boolean[] probes;

	private int firstLine;

	private int lastLine;

	/**
	 * All instructions of a method mapped from the ASM node to the
	 * corresponding {@link Instruction} instance.
	 */
	private final Map<AbstractInsnNode, Instruction> instructions;

	/**
	 * List of all jumps within the control flow. We need to store jumps
	 * temporarily as the target {@link Instruction} object may not yet be
	 * created when jumps are reported.
	 */
	private final List<Jump> jumps = new ArrayList<Jump>();

	InstructionCoverageCalculator(final boolean[] probes) {
		this.probes = probes;
		this.firstLine = ISourceNode.UNKNOWN_LINE;
		this.lastLine = ISourceNode.UNKNOWN_LINE;
		this.instructions = new HashMap<AbstractInsnNode, Instruction>();
	}

	Instruction addInstruction(final AbstractInsnNode node, final int line) {
		if (line != ISourceNode.UNKNOWN_LINE) {
			if (firstLine > line || lastLine == ISourceNode.UNKNOWN_LINE) {
				firstLine = line;
			}
			if (lastLine < line) {
				lastLine = line;
			}
		}
		final Instruction insn = new Instruction(line);
		instructions.put(node, insn);
		return insn;
	}

	void addJump(final Instruction source, final Label target,
			final int branch) {
		jumps.add(new Jump(source, target, branch));
	}

	void addProbe(final Instruction insn, final int probeId, final int branch) {
		final boolean executed = probes != null && probes[probeId];
		insn.addBranch(executed, branch);
	}

	Map<AbstractInsnNode, Instruction> calculateInstructionStatus() {
		// Wire jumps:
		for (final Jump j : jumps) {
			j.source.addBranch(LabelInfo.getInstruction(j.target), j.branch);
		}

		return instructions;
	}

	public int getFirstLine() {
		return firstLine;
	}

	public int getLastLine() {
		return lastLine;
	}

	private static class Jump {

		final Instruction source;
		final Label target;
		final int branch;

		Jump(final Instruction source, final Label target, final int branch) {
			this.source = source;
			this.target = target;
			this.branch = branch;
		}
	}

}
