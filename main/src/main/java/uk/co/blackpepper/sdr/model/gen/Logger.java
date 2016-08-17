package uk.co.blackpepper.sdr.model.gen;

public interface Logger {
	
	void debug(String message, Exception exception);
	
	void info(String message);
}
