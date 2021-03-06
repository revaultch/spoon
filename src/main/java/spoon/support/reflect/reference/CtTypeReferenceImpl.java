/*
 * Spoon - http://spoon.gforge.inria.fr/
 * Copyright (C) 2006 INRIA Futurs <renaud.pawlak@inria.fr>
 *
 * This software is governed by the CeCILL-C License under French law and
 * abiding by the rules of distribution of free software. You can use, modify
 * and/or redistribute the software under the terms of the CeCILL-C license as
 * circulated by CEA, CNRS and INRIA at http://www.cecill.info.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the CeCILL-C License for more details.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 */

package spoon.support.reflect.reference;

import spoon.Launcher;
import spoon.reflect.code.CtNewClass;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtGenericElementReference;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeAnnotableReference;
import spoon.reflect.reference.CtTypeParameterReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtVisitor;
import spoon.reflect.visitor.filter.AbstractFilter;
import spoon.support.reflect.declaration.CtElementImpl;
import spoon.support.util.RtHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static spoon.reflect.ModelElementContainerDefaultCapacities.ANNOTATIONS_CONTAINER_DEFAULT_CAPACITY;
import static spoon.reflect.ModelElementContainerDefaultCapacities.TYPE_TYPE_PARAMETERS_CONTAINER_DEFAULT_CAPACITY;

public class CtTypeReferenceImpl<T> extends CtReferenceImpl implements CtTypeReference<T> {
	private static final long serialVersionUID = 1L;

	List<CtTypeReference<?>> actualTypeArguments = CtElementImpl.emptyList();

	List<CtAnnotation<? extends Annotation>> annotations = CtElementImpl.emptyList();

	CtTypeReference<?> declaringType;

	private CtPackageReference pack;

	public CtTypeReferenceImpl() {
		super();
	}

	@Override
	public void accept(CtVisitor visitor) {
		visitor.visitCtTypeReference(this);
	}

	@Override
	public CtTypeReference<?> box() {
		if (!isPrimitive()) {
			return this;
		}
		if (getSimpleName().equals("int")) {
			return factory.Type().createReference(Integer.class);
		}
		if (getSimpleName().equals("float")) {
			return factory.Type().createReference(Float.class);
		}
		if (getSimpleName().equals("long")) {
			return factory.Type().createReference(Long.class);
		}
		if (getSimpleName().equals("char")) {
			return factory.Type().createReference(Character.class);
		}
		if (getSimpleName().equals("double")) {
			return factory.Type().createReference(Double.class);
		}
		if (getSimpleName().equals("boolean")) {
			return factory.Type().createReference(Boolean.class);
		}
		if (getSimpleName().equals("short")) {
			return factory.Type().createReference(Short.class);
		}
		if (getSimpleName().equals("byte")) {
			return factory.Type().createReference(Byte.class);
		}
		if (getSimpleName().equals("void")) {
			return factory.Type().createReference(Void.class);
		}
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<T> getActualClass() {
		if (isPrimitive()) {
			String simpleN = getSimpleName();
			if ("boolean".equals(simpleN)) {
				return (Class<T>) boolean.class;
			} else if ("byte".equals(simpleN)) {
				return (Class<T>) byte.class;
			} else if ("double".equals(simpleN)) {
				return (Class<T>) double.class;
			} else if ("int".equals(simpleN)) {
				return (Class<T>) int.class;
			} else if ("short".equals(simpleN)) {
				return (Class<T>) short.class;
			} else if ("char".equals(simpleN)) {
				return (Class<T>) char.class;
			} else if ("long".equals(simpleN)) {
				return (Class<T>) long.class;
			} else if ("float".equals(simpleN)) {
				return (Class<T>) float.class;
			} else if ("void".equals(simpleN)) {
				return (Class<T>) void.class;
			}
		}
		return findClass();
	}

	/**
	 * Finds the class requested in {@link #getActualClass()}, using the
	 * {@code ClassLoader} of the {@code Environment}
	 */
	@SuppressWarnings("unchecked")
	protected Class<T> findClass() {
		try {
			return (Class<T>) getFactory().getEnvironment().getClassLoader().loadClass(getQualifiedName());
		} catch (java.lang.ClassNotFoundException cnfe) {
			throw new SpoonClassNotFoundException("cannot load class: " + getQualifiedName() + " with class loader " + Thread.currentThread().getContextClassLoader(), cnfe);
		}
	}

	@Override
	public List<CtTypeReference<?>> getActualTypeArguments() {
		return actualTypeArguments;
	}

	@Override
	protected AnnotatedElement getActualAnnotatedElement() {
		return getActualClass();
	}

	@Override
	@SuppressWarnings("unchecked")
	public CtType<T> getDeclaration() {
		if (!isPrimitive() && !isAnonymous()) {
			return (CtType<T>) getFactory().Type().get(getQualifiedName());
		}
		if (!isPrimitive() && isAnonymous()) {
			final CtType<?> rootType = getFactory().Type().get(getDeclaringType().getQualifiedName());
			final List<CtNewClass<T>> elements = rootType.getElements(new AbstractFilter<CtNewClass<T>>(CtNewClass.class) {
				@Override
				public boolean matches(CtNewClass<T> element) {
					return getQualifiedName().equals(element.getAnonymousClass().getQualifiedName());
				}
			});
			if (elements.size()  == 0) {
				return null;
			}
			return (CtType<T>) elements.get(0).getAnonymousClass();
		}
		return null;
	}

	@Override
	public CtTypeReference<?> getDeclaringType() {
		return declaringType;
	}

	@Override
	public CtPackageReference getPackage() {
		return pack;
	}

	@Override
	public String getQualifiedName() {
		if (getDeclaringType() != null) {
			return getDeclaringType().getQualifiedName() + CtType.INNERTTYPE_SEPARATOR + getSimpleName();
		} else if (getPackage() != null && !CtPackage.TOP_LEVEL_PACKAGE_NAME.equals(getPackage().getSimpleName())) {
			if (!getTypeAnnotations().isEmpty()) {
				String qualifiedName = getPackage().getSimpleName() + CtPackage.PACKAGE_SEPARATOR;
				for (CtAnnotation<? extends Annotation> ctAnnotation : getTypeAnnotations()) {
					qualifiedName += "@" + ctAnnotation.getAnnotationType().getQualifiedName() + " ";
				}
				qualifiedName += getSimpleName();
				return qualifiedName;
			}
			return getPackage().getSimpleName() + CtPackage.PACKAGE_SEPARATOR + getSimpleName();
		} else {
			return getSimpleName();
		}
	}

	@Override
	public boolean isAssignableFrom(CtTypeReference<?> type) {
		return type != null && type.isSubtypeOf(this);
	}

	@Override
	public boolean isPrimitive() {
		return ("boolean".equals(getSimpleName()) || "byte".equals(getSimpleName()) || "double".equals(getSimpleName()) || "int".equals(getSimpleName()) || "short".equals(getSimpleName())
				|| "char".equals(getSimpleName()) || "long".equals(getSimpleName()) || "float".equals(getSimpleName()) || "void".equals(getSimpleName()));
	}

	@Override
	public boolean isSubtypeOf(CtTypeReference<?> type) {
		if (type instanceof CtTypeParameterReference) {
			return false;
		}
		if (NULL_TYPE_NAME.equals(getSimpleName()) || NULL_TYPE_NAME.equals(type.getSimpleName())) {
			return false;
		}
		// anonymous types cannot be resolved
		if (isAnonymous() || type.isAnonymous()) {
			return false;
		}
		if (isPrimitive() || type.isPrimitive()) {
			return equals(type);
		}
		CtType<?> superTypeDecl = type.getDeclaration();
		CtType<?> subTypeDecl = getDeclaration();
		if ((subTypeDecl == null) && (superTypeDecl == null)) {
			try {
				if (((this instanceof CtArrayTypeReference) && (type instanceof CtArrayTypeReference))) {
					return ((CtArrayTypeReference<?>) this).getComponentType().isSubtypeOf(((CtArrayTypeReference<?>) type).getComponentType());
				}
				Class<?> actualSubType = getActualClass();
				Class<?> actualSuperType = type.getActualClass();
				return actualSuperType.isAssignableFrom(actualSubType);
			} catch (Exception e) {
				Launcher.LOGGER.error("cannot determine runtime types for '" + this + "' (" + getActualClass() + ") and '" + type + "' (" + type.getActualClass() + ")", e);
				return false;
			}
		}
		if (getQualifiedName().equals(type.getQualifiedName())) {
			return true;
		}
		if (subTypeDecl != null) {
			for (CtTypeReference<?> ref : subTypeDecl.getSuperInterfaces()) {
				if (ref.isSubtypeOf(type)) {
					return true;
				}
			}
			if (subTypeDecl instanceof CtClass) {
				if (getFactory().Type().OBJECT.equals(type)) {
					return true;
				}
				if (((CtClass<?>) subTypeDecl).getSuperclass() != null) {
					if (((CtClass<?>) subTypeDecl).getSuperclass().equals(type)) {
						return true;
					}
					return ((CtClass<?>) subTypeDecl).getSuperclass().isSubtypeOf(type);
				}
			}
			return false;
		} else {
			try {
				Class<?> actualSubType = getActualClass();
				for (Class<?> c : actualSubType.getInterfaces()) {
					if (getFactory().Type().createReference(c).isSubtypeOf(type)) {
						return true;
					}
				}
				CtTypeReference<?> superType = getFactory().Type().createReference(actualSubType.getSuperclass());
				if (superType.equals(type)) {
					return true;
				} else {
					return superType.isSubtypeOf(type);
				}
			} catch (Exception e) {
				Launcher.LOGGER.error("cannot determine runtime types for '" + this + "' and '" + type + "'", e);
				return false;
			}
		}
	}

	@Override
	public <C extends CtGenericElementReference> C setActualTypeArguments(List<CtTypeReference<?>> actualTypeArguments) {
		this.actualTypeArguments = actualTypeArguments;
		return (C) this;
	}

	@Override
	public <C extends CtTypeReference<T>> C setDeclaringType(CtTypeReference<?> declaringType) {
		this.declaringType = declaringType;
		return (C) this;
	}

	@Override
	public <C extends CtTypeReference<T>> C setPackage(CtPackageReference pack) {
		this.pack = pack;
		return (C) this;
	}

	@Override
	public CtTypeReference<?> unbox() {
		if (isPrimitive()) {
			return this;
		}
		if (getActualClass() == Integer.class) {
			return factory.Type().createReference(int.class);
		}
		if (getActualClass() == Float.class) {
			return factory.Type().createReference(float.class);
		}
		if (getActualClass() == Long.class) {
			return factory.Type().createReference(long.class);
		}
		if (getActualClass() == Character.class) {
			return factory.Type().createReference(char.class);
		}
		if (getActualClass() == Double.class) {
			return factory.Type().createReference(double.class);
		}
		if (getActualClass() == Boolean.class) {
			return factory.Type().createReference(boolean.class);
		}
		if (getActualClass() == Short.class) {
			return factory.Type().createReference(short.class);
		}
		if (getActualClass() == Byte.class) {
			return factory.Type().createReference(byte.class);
		}
		if (getActualClass() == Void.class) {
			return factory.Type().createReference(void.class);
		}
		return this;
	}

	@Override
	public Collection<CtFieldReference<?>> getDeclaredFields() {
		Collection<CtFieldReference<?>> l = new ArrayList<CtFieldReference<?>>();
		CtType<?> t = getDeclaration();
		if (t == null) {
			for (Field f : getActualClass().getDeclaredFields()) {
				l.add(getFactory().Field().createReference(f));
			}
			if (getActualClass().isAnnotation()) {
				for (Method m : getActualClass().getDeclaredMethods()) {
					CtTypeReference<?> retRef = getFactory().Type().createReference(m.getReturnType());
					CtFieldReference<?> fr = getFactory().Field().createReference(this, retRef, m.getName());
					// fr.
					l.add(fr);
				}
			}

		} else {
			return t.getDeclaredFields();
		}
		return l;
	}

	@Override
	public Collection<CtExecutableReference<?>> getDeclaredExecutables() {
		CtType<T> t = getDeclaration();
		if (t == null) {
			return RtHelper.getAllExecutables(getActualClass(), getFactory());
		} else {
			return t.getDeclaredExecutables();
		}
	}

	@Override
	public Collection<CtFieldReference<?>> getAllFields() {
		CtType<?> t = getDeclaration();
		if (t == null) {
			return RtHelper.getAllFields(getActualClass(), getFactory());
		} else {
			return t.getAllFields();
		}
	}

	@Override
	public Collection<CtExecutableReference<?>> getAllExecutables() {
		Collection<CtExecutableReference<?>> l = new ArrayList<CtExecutableReference<?>>();
		CtType<T> t = getDeclaration();
		if (t == null) {
			Class<?> c = getActualClass();
			for (Method m : c.getDeclaredMethods()) {
				l.add(getFactory().Method().createReference(m));
			}
			for (Constructor<?> cons : c.getDeclaredConstructors()) {
				CtExecutableReference<?> consRef = getFactory().Constructor().createReference(cons);
				l.add(consRef);
			}
			Class<?> sc = c.getSuperclass();
			l.addAll(getFactory().Type().createReference(sc).getAllExecutables());
		} else {
			return t.getAllExecutables();
		}
		return l;
	}

	@Override
	public Set<ModifierKind> getModifiers() {
		CtType<T> t = getDeclaration();
		if (t != null) {
			return t.getModifiers();
		}
		Class<T> c = getActualClass();
		return RtHelper.getModifiers(c.getModifiers());
	}

	@Override
	public CtTypeReference<?> getSuperclass() {
		CtType<T> t = getDeclaration();
		if (t != null) {
			return t.getSuperclass();
		} else {
			Class<T> c = getActualClass();
			Class<?> sc = c.getSuperclass();
			if (sc == null) {
				return null;
			}
			return getFactory().Type().createReference(sc);
		}
	}

	@Override
	public Set<CtTypeReference<?>> getSuperInterfaces() {
		CtType<?> t = getDeclaration();
		if (t != null) {
			return t.getSuperInterfaces();
		} else {
			Class<?> c = getActualClass();
			Class<?>[] sis = c.getInterfaces();
			if ((sis != null) && (sis.length > 0)) {
				Set<CtTypeReference<?>> set = new TreeSet<CtTypeReference<?>>();
				for (Class<?> si : sis) {
					set.add(getFactory().Type().createReference(si));
				}
				return set;
			}
		}
		return new TreeSet<CtTypeReference<?>>();
	}

	@Override
	public boolean isAnonymous() {
		try {
			Integer.parseInt(getSimpleName());
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	@Override
	public <C extends CtGenericElementReference> C addActualTypeArgument(CtTypeReference<?> actualTypeArgument) {
		if (actualTypeArguments == CtElementImpl.<CtTypeReference<?>>emptyList()) {
			actualTypeArguments = new ArrayList<CtTypeReference<?>>(TYPE_TYPE_PARAMETERS_CONTAINER_DEFAULT_CAPACITY);
		}
		actualTypeArguments.add(actualTypeArgument);
		return (C) this;
	}

	@Override
	public boolean removeActualTypeArgument(CtTypeReference<?> actualTypeArgument) {
		return actualTypeArguments != CtElementImpl.<CtTypeReference<?>>emptyList() && actualTypeArguments.remove(actualTypeArgument);
	}

	@Override
	public boolean isInterface() {
		CtType<T> t = getDeclaration();
		if (t == null) {
			return getActualClass().isInterface();
		} else {
			return t.isInterface();
		}
	}

	@Override
	public List<CtAnnotation<? extends Annotation>> getTypeAnnotations() {
		return Collections.unmodifiableList(annotations);
	}

	@Override
	public <C extends CtTypeAnnotableReference> C setTypeAnnotations(List<CtAnnotation<? extends Annotation>> annotations) {
		this.annotations = annotations;
		return (C) this;
	}

	@Override
	public <C extends CtTypeAnnotableReference> C addTypeAnnotation(CtAnnotation<? extends Annotation> annotation) {
		if (annotation == null) {
			return (C) this;
		}
		if ((List<?>) this.annotations == (List<?>) CtElementImpl.emptyList()) {
			this.annotations = new ArrayList<CtAnnotation<? extends Annotation>>(ANNOTATIONS_CONTAINER_DEFAULT_CAPACITY);
		}
		this.annotations.add(annotation);
		return (C) this;
	}

	@Override
	public boolean removeTypeAnnotation(CtAnnotation<? extends Annotation> annotation) {
		return annotation != null && this.annotations.remove(annotation);
	}

}
