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
package org.jacoco.core.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public class BytecodeVersionTest {

	@Test
	public void should_get_and_set_major_version() {
		final byte[] bytes = createClass(Opcodes.V1_1);
		assertEquals(45, BytecodeVersion.get(bytes));

		BytecodeVersion.set(bytes, Opcodes.V1_2);
		assertEquals(46, BytecodeVersion.get(bytes));
	}

	@Test
	public void should_return_original_when_less_than_MAX_VERSION() {
		int lowerVersion = BytecodeVersion.MAX_VERSION - 1;
		final byte[] originalBytes = createClass(lowerVersion);

		final byte[] bytes = BytecodeVersion.downgradeIfNeeded(lowerVersion,
				originalBytes);

		assertSame(originalBytes, bytes);
	}

	@Test
	public void should_return_original_when_equals_MAX_VERSION() {
		final byte[] originalBytes = createClass(BytecodeVersion.MAX_VERSION);

		final byte[] bytes = BytecodeVersion
				.downgradeIfNeeded(BytecodeVersion.MAX_VERSION, originalBytes);

		assertSame(originalBytes, bytes);
	}

	@Test
	public void should_return_copy_when_greater_than_MAX_VERSION() {
		int higerVersion = BytecodeVersion.MAX_VERSION + 1;
		final byte[] originalBytes = createClass(higerVersion);

		final byte[] bytes = BytecodeVersion.downgradeIfNeeded(higerVersion,
				originalBytes);

		assertNotSame(originalBytes, bytes);
		assertEquals(BytecodeVersion.MAX_VERSION, BytecodeVersion.get(bytes));
		assertEquals(higerVersion, BytecodeVersion.get(originalBytes));
	}

	private static byte[] createClass(final int version) {
		final ClassWriter cw = new ClassWriter(0);
		cw.visit(version, 0, "Foo", null, "java/lang/Object", null);
		cw.visitEnd();
		return cw.toByteArray();
	}

}
