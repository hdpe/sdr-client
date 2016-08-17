package uk.co.blackpepper.sdr.model.gen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.forge.roaster.model.source.AnnotationSource;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;

import uk.co.blackpepper.sdr.model.gen.model.Annotation;
import uk.co.blackpepper.sdr.model.gen.model.ClassSource;
import uk.co.blackpepper.sdr.model.gen.model.Field;

class RoasterClassSourceAdapter implements ClassSource {

	private static class AnnotationImpl implements Annotation {

		private final AnnotationSource<?> source;

		AnnotationImpl(AnnotationSource<?> source) {
			this.source = source;
		}

		@Override
		public String getFullyQualifiedName() {
			return source.getQualifiedName();
		}

		@Override
		public Map<String, Object> values() {
			Map<String, Object> result = new LinkedHashMap<String, Object>();
			if (source.isSingleValue()) {
				result.put("value", source.getStringValue());
			}
			return result;
		}
	}

	private static class FieldImpl implements Field {

		private final FieldSource<?> source;

		FieldImpl(FieldSource<?> source) {
			this.source = source;
		}

		@Override
		public String getName() {
			return source.getName();
		}

		@Override
		public String getQualifiedTypeNameWithGenerics() {
			return source.getType().getQualifiedNameWithGenerics();
		}

		@Override
		public Collection<Annotation> getAnnotations() {
			List<Annotation> result = new ArrayList<Annotation>();
			for (AnnotationSource<?> annotation : source.getAnnotations()) {
				result.add(new AnnotationImpl(annotation));
			}
			return result;
		}
	}

	private final JavaClassSource source;

	RoasterClassSourceAdapter(JavaClassSource source) {
		this.source = source;
	}

	@Override
	public String getName() {
		return source.getName();
	}

	@Override
	public String getPackage() {
		return source.getPackage();
	}

	@Override
	public Collection<Annotation> getAnnotations() {
		Collection<Annotation> result = new ArrayList<Annotation>();
		for (AnnotationSource<?> annotation : source.getAnnotations()) {
			result.add(new AnnotationImpl(annotation));
		}
		return result;
	}

	@Override
	public Collection<Field> getFields() {
		Collection<Field> result = new ArrayList<Field>();
		for (FieldSource<?> field : source.getFields()) {
			result.add(new FieldImpl(field));
		}
		return result;
	}
}
