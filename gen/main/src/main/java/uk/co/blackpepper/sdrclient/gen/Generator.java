package uk.co.blackpepper.sdrclient.gen;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.Entity;

import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.Type;
import org.jboss.forge.roaster.model.source.AnnotationSource;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.jboss.forge.roaster.model.source.PropertySource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import uk.co.blackpepper.sdrclient.EmbeddedChildDeserializer;
import uk.co.blackpepper.sdrclient.gen.annotation.EmbeddedResource;
import uk.co.blackpepper.sdrclient.gen.annotation.LinkedResource;
import uk.co.blackpepper.sdrclient.gen.annotation.RemoteResource;
import uk.co.blackpepper.sdrclient.gen.model.Annotation;
import uk.co.blackpepper.sdrclient.gen.model.ClassSource;
import uk.co.blackpepper.sdrclient.gen.model.Field;

public class Generator {

	private static AnnotationRegistry annotationRegistry = new AnnotationRegistry();
	
	static {
		annotationRegistry.registerAnnotation(uk.co.blackpepper.sdrclient.annotation.RemoteResource.class.getName(),
				RemoteResource.class.getName());
		annotationRegistry.registerAnnotation(uk.co.blackpepper.sdrclient.annotation.LinkedResource.class.getName(),
				LinkedResource.class.getName());
		annotationRegistry.registerAnnotation(uk.co.blackpepper.sdrclient.annotation.EmbeddedResource.class.getName(),
				EmbeddedResource.class.getName());
		annotationRegistry.registerAnnotation(uk.co.blackpepper.sdrclient.annotation.EmbeddedResource.class.getName(),
				JsonDeserialize.class.getName(),
				Collections.<String, Object>singletonMap("contentUsing", EmbeddedChildDeserializer.class));
	}
	
	public void generate(ClassSource source, GeneratedClassWriter classWriter) throws IOException {

		Annotation entityAnnotation = getAnnotation(source, Entity.class);
		
		if (entityAnnotation == null) {
			return;
		}
		
		JavaClassSource result = Roaster.create(JavaClassSource.class)
				.setName(source.getName())
				.setPackage(convertToClientPackage(source.getPackage()));
		
		Annotation remoteResourceAnnotation = getAnnotation(source,
				uk.co.blackpepper.sdrclient.annotation.RemoteResource.class);
		
		if (remoteResourceAnnotation != null) {
			result.addAnnotation(RemoteResource.class)
					.setStringValue((String) remoteResourceAnnotation.values().get("value")).getOrigin();
		}

		result.addProperty(URI.class, getIdField(source).getName())
			.removeMutator();

		for (Field field : getNonIdFields(source)) {
			String type = convertFieldType(field, source);
			PropertySource<?> property = result.addProperty(type, field.getName());
			addAnnotations(property.getAccessor(), field.getAnnotations());
			addInitializer(property.getField(), result);
		}

		classWriter.write(createSourceFileRelativePath(result), result.toString());
	}

	private static void addInitializer(FieldSource<?> field, JavaClassSource result) {
		String implementationType = getImplementationType(field);
		if (implementationType != null) {
			String simpleName = result.addImport(implementationType).getSimpleName();
			String typeArgs = getTypeArgs(field.getType());
			
			field.setLiteralInitializer("new " + simpleName + typeArgs + "()");
		}
	}

	private static String getTypeArgs(Type<?> type) {
		StringBuilder sb = new StringBuilder("<");
		for (Type<?> typeArg : type.getTypeArguments()) {
			if (sb.length() > 1) {
				sb.append(", ");
			}
			sb.append(typeArg);
		}
		return sb.append(">").toString();
	}

	private static String getImplementationType(FieldSource<?> field) {
		String qualifiedName = field.getType().getQualifiedName();
		
		if ("java.util.Set".equals(qualifiedName)) {
			return "java.util.LinkedHashSet";
		}
		if ("java.util.List".equals(qualifiedName)) {
			return "java.util.ArrayList";
		}
		if ("java.util.SortedSet".equals(qualifiedName)) {
			return "java.util.TreeSet";
		}
		
		return null;
	}

	private static String convertFieldType(Field field, ClassSource source) {
		return field.getQualifiedTypeNameWithGenerics().replaceAll(source.getPackage(),
				convertToClientPackage(source.getPackage()));
	}
	
	private static String convertToClientPackage(String modelPackage) {
		return modelPackage + ".client";
	}
	
	private static void addAnnotations(final MethodSource<?> getter, Collection<Annotation> fieldAnnotations) {
		AnnotationApplicator applicator = new AnnotationApplicator() {
			
			public void apply(String fullyQualifiedAnnotationName, Map<String, Object> annotationAttributes) {
				AnnotationSource<?> annotation = getter.addAnnotation(fullyQualifiedAnnotationName);
				
				for (Entry<String, Object> attr : annotationAttributes.entrySet()) {
					if (attr.getValue() instanceof Class<?>) {
						annotation.setClassValue(attr.getKey(), (Class<?>) attr.getValue());
					}
				}
			}
		};
		
		for (Annotation annotation : fieldAnnotations) {
			annotationRegistry.applyAnnotations(annotation.getFullyQualifiedName(), applicator);
		}
	}

	public Field getIdField(ClassSource clazz) {
		for (Field field : clazz.getFields()) {
			if ("id".equals(field.getName())) {
				return field;
			}
		}

		throw new IllegalStateException("no @Id field found");
	}

	public Collection<Field> getNonIdFields(ClassSource clazz) {
		List<Field> result = new ArrayList<Field>();
		for (Field fieldSource : clazz.getFields()) {
			if (!"id".equals(fieldSource.getName())) {
				result.add(fieldSource);
			}
		}
		return result;
	}

	private static Annotation getAnnotation(ClassSource clazz, Class<?> type) {
		for (Annotation annotation : clazz.getAnnotations()) {
			if (type.getName().equals(annotation.getFullyQualifiedName())) {
				return annotation;
			}
		}

		return null;
	}

	private static String createSourceFileRelativePath(JavaClassSource result) {
		return result.getPackage().replaceAll("\\.", File.separator) + File.separator
				+ result.getName() + ".java";
	}
}
