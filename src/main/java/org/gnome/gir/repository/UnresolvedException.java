package org.gnome.gir.repository;

public class UnresolvedException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public UnresolvedException(String namespace, String name) {
		super("ns=" + namespace + ",name=" + name + " is not loaded");
	}
}
