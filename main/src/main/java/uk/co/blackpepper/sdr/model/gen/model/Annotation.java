package uk.co.blackpepper.sdr.model.gen.model;

import java.util.Map;

public interface Annotation {

	String getFullyQualifiedName();

	Map<String, Object> values();
}
