package org.scriptbox.util.remoting.jetty;

public class TestService implements TestServiceInterface {

	@Override
	public String testMe(String param) {
		return param;
	}

}
