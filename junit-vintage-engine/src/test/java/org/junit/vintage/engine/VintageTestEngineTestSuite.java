/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine;

import org.junit.platform.suite.api.ExcludeTags;
import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/**
 * Test suite for the {@link VintageTestEngine}.
 *
 * <h2>Logging Configuration</h2>
 *
 * <p>In order for our log4j2 configuration to be used in an IDE, you must
 * set the following system property before running any tests &mdash; for
 * example, in <em>Run Configurations</em> in Eclipse.
 *
 * <pre class="code">
 * -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager
 * </pre>
 *
 * @since 4.12
 */
@Suite
@SelectPackages("org.junit.vintage.engine")
@IncludeClassNamePatterns(".*Tests?")
@IncludeEngines("junit-jupiter")
@ExcludeTags("missing-junit4")
class VintageTestEngineTestSuite {
}
