package org.bundlebee.examples.hello;

public class Hello {
	public String compute( int seq ) {
		String s = "Hello! (" + seq + ")";
		System.out.println("computed string: " + s);
		return s;
	}
}