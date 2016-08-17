package uk.co.blackpepper.sdr.model.gen.model;

import java.util.Collection;

public interface Field {

	String getName();

	String getQualifiedTypeNameWithGenerics();
	
	Collection<Annotation> getAnnotations();
}
