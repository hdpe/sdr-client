package uk.co.blackpepper.sdr.model.gen;

import java.util.Map;

public interface AnnotationApplicator {

	void apply(String fullyQualifiedAnnotationName, Map<String, Object> annotationAttributes);
}
