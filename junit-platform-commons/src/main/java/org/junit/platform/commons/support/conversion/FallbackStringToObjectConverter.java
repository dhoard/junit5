/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.support.conversion;

import static org.junit.platform.commons.support.HierarchyTraversalMode.BOTTOM_UP;
import static org.junit.platform.commons.support.ModifierSupport.isNotPrivate;
import static org.junit.platform.commons.support.ModifierSupport.isNotStatic;
import static org.junit.platform.commons.support.ReflectionSupport.findMethods;
import static org.junit.platform.commons.support.ReflectionSupport.invokeMethod;
import static org.junit.platform.commons.util.ReflectionUtils.findConstructors;
import static org.junit.platform.commons.util.ReflectionUtils.newInstance;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;

/**
 * {@code FallbackStringToObjectConverter} is a {@link StringToObjectConverter}
 * that provides a fallback conversion strategy for converting from a
 * {@link String} to a given target type by invoking a static factory method
 * or factory constructor defined in the target type.
 *
 * <h2>Search Algorithm</h2>
 *
 * <ol>
 * <li>Search for a single, non-private static factory method in the target
 * type that converts from a String to the target type. Use the factory method
 * if present.</li>
 * <li>Search for a single, non-private constructor in the target type that
 * accepts a String. Use the constructor if present.</li>
 * </ol>
 *
 * <p>If multiple suitable factory methods are discovered they will be ignored.
 * If neither a single factory method nor a single constructor is found, this
 * converter acts as a no-op.
 *
 * @since 1.11
 * @see ConversionSupport
 */
class FallbackStringToObjectConverter implements StringToObjectConverter {

	/**
	 * Implementation of the NULL Object Pattern.
	 */
	private static final Function<String, Object> NULL_EXECUTABLE = source -> source;

	/**
	 * Cache for factory methods and factory constructors.
	 *
	 * <p>Searches that do not find a factory method or constructor are tracked
	 * by the presence of a {@link #NULL_EXECUTABLE} object stored in the map.
	 * This prevents the framework from repeatedly searching for things which
	 * are already known not to exist.
	 */
	private static final ConcurrentHashMap<Class<?>, Function<String, @Nullable Object>> factoryExecutableCache //
		= new ConcurrentHashMap<>(64);

	@Override
	public boolean canConvertTo(Class<?> targetType) {
		return findFactoryExecutable(targetType) != NULL_EXECUTABLE;
	}

	@Override
	@Nullable
	public Object convert(String source, Class<?> targetType) throws Exception {
		Function<String, @Nullable Object> executable = findFactoryExecutable(targetType);
		Preconditions.condition(executable != NULL_EXECUTABLE,
			"Illegal state: convert() must not be called if canConvert() returned false");

		return executable.apply(source);
	}

	private static Function<String, @Nullable Object> findFactoryExecutable(Class<?> targetType) {
		return factoryExecutableCache.computeIfAbsent(targetType, type -> {
			Method factoryMethod = findFactoryMethod(type);
			if (factoryMethod != null) {
				return source -> invokeMethod(factoryMethod, null, source);
			}
			Constructor<?> constructor = findFactoryConstructor(type);
			if (constructor != null) {
				return source -> newInstance(constructor, source);
			}
			return NULL_EXECUTABLE;
		});
	}

	private static @Nullable Method findFactoryMethod(Class<?> targetType) {
		List<Method> factoryMethods = findMethods(targetType, new IsFactoryMethod(targetType), BOTTOM_UP);
		if (factoryMethods.size() == 1) {
			return factoryMethods.get(0);
		}
		return null;
	}

	private static @Nullable Constructor<?> findFactoryConstructor(Class<?> targetType) {
		List<Constructor<?>> constructors = findConstructors(targetType, new IsFactoryConstructor(targetType));
		if (constructors.size() == 1) {
			return constructors.get(0);
		}
		return null;
	}

	/**
	 * {@link Predicate} that determines if the {@link Method} supplied to
	 * {@link #test(Method)} is a non-private static factory method for the
	 * supplied {@link #targetType}.
	 */
	static class IsFactoryMethod implements Predicate<Method> {

		private final Class<?> targetType;

		IsFactoryMethod(Class<?> targetType) {
			this.targetType = targetType;
		}

		@Override
		public boolean test(Method method) {
			// Please do not collapse the following into a single statement.
			if (!method.getReturnType().equals(this.targetType)) {
				return false;
			}
			if (isNotStatic(method)) {
				return false;
			}
			return isNotPrivateAndAcceptsSingleStringArgument(method);
		}

	}

	/**
	 * {@link Predicate} that determines if the {@link Constructor} supplied to
	 * {@link #test(Constructor)} is a non-private factory constructor for the
	 * supplied {@link #targetType}.
	 */
	static class IsFactoryConstructor implements Predicate<Constructor<?>> {

		private final Class<?> targetType;

		IsFactoryConstructor(Class<?> targetType) {
			this.targetType = targetType;
		}

		@Override
		public boolean test(Constructor<?> constructor) {
			// Please do not collapse the following into a single statement.
			if (!constructor.getDeclaringClass().equals(this.targetType)) {
				return false;
			}
			return isNotPrivateAndAcceptsSingleStringArgument(constructor);
		}

	}

	private static boolean isNotPrivateAndAcceptsSingleStringArgument(Executable executable) {
		return isNotPrivate(executable) //
				&& (executable.getParameterCount() == 1) //
				&& (executable.getParameterTypes()[0] == String.class);
	}

}
