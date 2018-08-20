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

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link Instruction}.
 */
public class InstructionTest {

	private Instruction instruction;

	@Before
	public void setup() {
		instruction = new Instruction(123);
	}

	@Test
	public void getLine_should_return_line_number() {
		assertEquals(123, instruction.getLine());
	}

	@Test
	public void new_instance_should_have_no_branches() {
		assertEquals(0, instruction.getBranches());
		assertEquals(0, instruction.getCoveredBranches());
	}

	@Test
	public void addBranchWithInstruction_should_increment_branches() {
		instruction.addBranch(new Instruction(122), 0);

		assertEquals(1, instruction.getBranches());
		assertEquals(0, instruction.getCoveredBranches());
	}

	@Test
	public void addBranchWithInstruction_should_propagate_existing_coverage_status() {
		final Instruction target = new Instruction(122);
		target.addBranch(true, 0);

		instruction.addBranch(target, 0);

		assertEquals(1, instruction.getBranches());
		assertEquals(1, instruction.getCoveredBranches());
	}

	@Test
	public void addBranchWithProbe_should_increment_branches_when_covered() {
		instruction.addBranch(true, 0);

		assertEquals(1, instruction.getBranches());
		assertEquals(1, instruction.getCoveredBranches());
	}

	@Test
	public void addBranchWithProbe_should_increment_branches_when__not_covered() {
		instruction.addBranch(false, 0);

		assertEquals(1, instruction.getBranches());
		assertEquals(0, instruction.getCoveredBranches());
	}

	@Test
	public void addBranchWithProbe_should_propagate_coverage_status_to_existing_predecessors() {
		final Instruction i1 = new Instruction(124);
		final Instruction i2 = new Instruction(125);
		instruction.addBranch(i1, 3);
		i1.addBranch(i2, 5);

		i2.addBranch(true, 8);

		assertEquals(1, instruction.getCoveredBranches());
	}

	@Test
	public void merge_should_calculate_superset_of_covered_branches() {
		instruction.addBranch(false, 1);
		instruction.addBranch(false, 2);
		instruction.addBranch(true, 3);
		instruction.addBranch(true, 4);
		final Instruction i2 = new Instruction(124);
		i2.addBranch(false, 1);
		i2.addBranch(true, 2);
		i2.addBranch(false, 3);
		i2.addBranch(true, 4);

		instruction.merge(i2);

		assertEquals(3, instruction.getCoveredBranches());
	}

	@Test
	public void addBranch_should_count_large_number_of_branches() {
		for (int branch = 0; branch < 0x1000; branch++) {
			instruction.addBranch(true, branch);
		}

		assertEquals(0x1000, instruction.getBranches());
		assertEquals(0x1000, instruction.getCoveredBranches());
	}

	@Test
	public void addBranch_should_propagate_coverage_status_over_very_long_sequence() {
		final Instruction first = new Instruction(0);
		Instruction next = first;
		for (int i = 0; i < 0x10000; i++) {
			final Instruction insn = new Instruction(i);
			next.addBranch(insn, 0);
			next = insn;
		}
		next.addBranch(true, 0);

		assertEquals(1, first.getCoveredBranches());
	}

}
