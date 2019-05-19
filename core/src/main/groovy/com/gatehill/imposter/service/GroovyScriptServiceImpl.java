package com.gatehill.imposter.service;

import com.gatehill.imposter.plugin.config.ResourceConfig;
import com.gatehill.imposter.script.InternalResponseBehavior;
import com.gatehill.imposter.script.impl.GroovyResponseBehaviourImpl;
import groovy.lang.Binding;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyClassLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.runtime.InvokerHelper;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class GroovyScriptServiceImpl implements ScriptService {
    private static final Logger LOGGER = LogManager.getLogger(GroovyScriptServiceImpl.class);

    /**
     * Execute the script and read response behaviour.
     *
     * @param config   the plugin configuration
     * @param bindings the script engine bindings
     * @return the response behaviour
     */
    @Override
    public InternalResponseBehavior executeScript(ResourceConfig config, Map<String, Object> bindings) {
        final String configFolder = config.getParentDir().getAbsolutePath();
	final String fileName = config.getResponseConfig().getScriptFile();

	final Path scriptFile = Paths.get(configFolder, fileName);
        
	LOGGER.trace("Executing script file: {}", scriptFile);

        // the script class will be a subclass of AbstractResponseBehaviour
        final CompilerConfiguration compilerConfig = new CompilerConfiguration();
        compilerConfig.setScriptBaseClass(GroovyResponseBehaviourImpl.class.getCanonicalName());
	compilerConfig.setClasspath(configFolder);

	final GroovyClassLoader groovyClassLoader = new GroovyClassLoader(
            this.getClass().getClassLoader(), compilerConfig, true);
        
	try {
            final GroovyResponseBehaviourImpl script = (GroovyResponseBehaviourImpl) InvokerHelper.createScript(
                    groovyClassLoader.parseClass(new GroovyCodeSource(scriptFile.toFile(), 
                            compilerConfig.getSourceEncoding())), convertBindings(bindings));

            script.run();

            return script;
        } catch (Exception e) {
            throw new RuntimeException("Script execution terminated abnormally", e);
        }
    }

    private static Binding convertBindings(Map<String, Object> bindings) {
        final Binding binding = new Binding();
        bindings.forEach(binding::setVariable);
        return binding;
    }
}
