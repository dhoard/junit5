/*
 * Copyright 2015-2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package platform.tooling.support.process;

import java.time.Duration;
import java.util.List;

public record ProcessResult(int exitCode, Duration duration, String stdOut, String stdErr) {

	public List<String> stdOutLines() {
		return stdOut.lines().toList();
	}

	public List<String> stdErrLines() {
		return stdErr.lines().toList();
	}
}