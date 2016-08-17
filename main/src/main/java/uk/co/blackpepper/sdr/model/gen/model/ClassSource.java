package uk.co.blackpepper.sdr.model.gen.model;

import java.util.Collection;

public interface ClassSource {

	String getName();

	String getPackage();

	Collection<Annotation> getAnnotations();

	Collection<Field> getFields();
}
