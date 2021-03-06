package org.scriptbox.box.events;

import org.scriptbox.box.container.BoxContext;
import org.scriptbox.box.container.BoxScript;

public interface BoxContextListener {

	public void contextCreated( BoxContext context ) throws Exception;
	public void contextShutdown( BoxContext context ) throws Exception;
	public void executingScript( BoxInvocationContext invocation ) throws Exception;
}
