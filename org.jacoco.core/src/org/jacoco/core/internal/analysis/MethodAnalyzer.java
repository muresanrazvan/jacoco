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
import java.util.List;

import org.jacoco.core.analysis.ISourceNode;
import org.jacoco.core.internal.flow.IFrame;
import org.jacoco.core.internal.flow.Instruction;
import org.jacoco.core.internal.flow.LabelInfo;
import org.jacoco.core.internal.flow.MethodProbesVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

/**
 * A {@link MethodProbesVisitor} that analyzes which statements and branches of
 * a method have been executed based on given probe data.
 */
public class MethodAnalyzer extends MethodProbesVisitor {

	private final InstructionCoverageCalculator calc;

	/** Current node of the ASM tree API */
	private AbstractInsnNode currentNode;

	/** Current instruction in byte code sequence */
	private Instruction currentInsn;

	/** Current line number. */
	private int currentLine = ISourceNode.UNKNOWN_LINE;

	// Due to ASM issue #315745 there can be more than one label per instruction
	private final List<Label> currentLabel = new ArrayList<Label>(2);

	/**
	 * New Method analyzer for the given probe data.
	 *
	 * @param name
	 *            method name
	 * @param desc
	 *            method descriptor
	 * @param signature
	 *            optional parameterized signature
	 * @param probes
	 *            recorded probe date of the containing class or
	 *            <code>null</code> if the class is not executed at all
	 * @param filter
	 *            filter which should be applied
	 * @param filterContext
	 *            class context information for the filter
	 */
	MethodAnalyzer(final InstructionCoverageCalculator calc) {
		this.calc = calc;
	}

	/**
	 * {@link MethodNode#accept(MethodVisitor)}
	 */
	@Override
	public void accept(final MethodNode methodNode,
			final MethodVisitor methodVisitor) {
		methodVisitor.visitCode();
		for (final TryCatchBlockNode n : methodNode.tryCatchBlocks) {
			n.accept(methodVisitor);
		}
		currentNode = methodNode.instructions.getFirst();
		while (currentNode != null) {
			currentNode.accept(methodVisitor);
			currentNode = currentNode.getNext();
		}
		methodVisitor.visitEnd();
	}

	@Override
	public void visitLabel(final Label label) {
		currentLabel.add(label);
		if (!LabelInfo.isSuccessor(label)) {
			currentInsn = null;
		}
	}

	@Override
	public void visitLineNumber(final int line, final Label start) {
		currentLine = line;
	}

	private void visitInsn() {
		final Instruction insn = calc.addInstruction(currentNode, currentLine);
		if (currentInsn != null) {
			currentInsn.addBranch(insn, 0);
		}
		final int labelCount = currentLabel.size();
		if (labelCount > 0) {
			for (int i = labelCount; --i >= 0;) {
				LabelInfo.setInstruction(currentLabel.get(i), insn);
			}
			currentLabel.clear();
		}
		currentInsn = insn;
	}

	@Override
	public void visitInsn(final int opcode) {
		visitInsn();
	}

	@Override
	public void visitIntInsn(final int opcode, final int operand) {
		visitInsn();
	}

	@Override
	public void visitVarInsn(final int opcode, final int var) {
		visitInsn();
	}

	@Override
	public void visitTypeInsn(final int opcode, final String type) {
		visitInsn();
	}

	@Override
	public void visitFieldInsn(final int opcode, final String owner,
			final String name, final String desc) {
		visitInsn();
	}

	@Override
	public void visitMethodInsn(final int opcode, final String owner,
			final String name, final String desc, final boolean itf) {
		visitInsn();
	}

	@Override
	public void visitInvokeDynamicInsn(final String name, final String desc,
			final Handle bsm, final Object... bsmArgs) {
		visitInsn();
	}

	@Override
	public void visitJumpInsn(final int opcode, final Label label) {
		visitInsn();
		calc.addJump(currentInsn, label, 1);
	}

	@Override
	public void visitLdcInsn(final Object cst) {
		visitInsn();
	}

	@Override
	public void visitIincInsn(final int var, final int increment) {
		visitInsn();
	}

	@Override
	public void visitTableSwitchInsn(final int min, final int max,
			final Label dflt, final Label... labels) {
		visitSwitchInsn(dflt, labels);
	}

	@Override
	public void visitLookupSwitchInsn(final Label dflt, final int[] keys,
			final Label[] labels) {
		visitSwitchInsn(dflt, labels);
	}

	private void visitSwitchInsn(final Label dflt, final Label[] labels) {
		visitInsn();
		LabelInfo.resetDone(labels);
		int branch = 0;
		calc.addJump(currentInsn, dflt, branch);
		LabelInfo.setDone(dflt);
		for (final Label l : labels) {
			if (!LabelInfo.isDone(l)) {
				branch++;
				calc.addJump(currentInsn, l, branch);
				LabelInfo.setDone(l);
			}
		}
	}

	@Override
	public void visitMultiANewArrayInsn(final String desc, final int dims) {
		visitInsn();
	}

	@Override
	public void visitProbe(final int probeId) {
		addProbe(probeId, 0);
		currentInsn = null;
	}

	@Override
	public void visitJumpInsnWithProbe(final int opcode, final Label label,
			final int probeId, final IFrame frame) {
		visitInsn();
		addProbe(probeId, 1);
	}

	@Override
	public void visitInsnWithProbe(final int opcode, final int probeId) {
		visitInsn();
		addProbe(probeId, 0);
	}

	@Override
	public void visitTableSwitchInsnWithProbes(final int min, final int max,
			final Label dflt, final Label[] labels, final IFrame frame) {
		visitSwitchInsnWithProbes(dflt, labels);
	}

	@Override
	public void visitLookupSwitchInsnWithProbes(final Label dflt,
			final int[] keys, final Label[] labels, final IFrame frame) {
		visitSwitchInsnWithProbes(dflt, labels);
	}

	private void visitSwitchInsnWithProbes(final Label dflt,
			final Label[] labels) {
		visitInsn();
		LabelInfo.resetDone(dflt);
		LabelInfo.resetDone(labels);
		int branch = 0;
		visitSwitchTarget(dflt, branch);
		for (final Label l : labels) {
			branch++;
			visitSwitchTarget(l, branch);
		}
	}

	private void visitSwitchTarget(final Label label, final int branch) {
		final int id = LabelInfo.getProbeId(label);
		if (!LabelInfo.isDone(label)) {
			if (id == LabelInfo.NO_PROBE) {
				calc.addJump(currentInsn, label, branch);
			} else {
				addProbe(id, branch);
			}
			LabelInfo.setDone(label);
		}
	}

	private void addProbe(final int probeId, final int branch) {
		calc.addProbe(currentInsn, probeId, branch);
	}

}
