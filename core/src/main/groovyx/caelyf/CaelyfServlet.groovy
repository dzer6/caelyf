/*
 * Copyright 2009-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package groovyx.caelyf

import groovy.servlet.GroovyServlet
import groovy.servlet.ServletBinding
import groovy.servlet.ServletCategory

import groovy.util.GroovyScriptEngine;

import org.codehaus.groovy.runtime.GroovyCategorySupport;

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import javax.servlet.ServletConfig
import groovyx.caelyf.plugins.PluginsHandler
import groovyx.caelyf.logging.GroovyLogger

/**
 * The Caelyf servlet extends Groovy's own Groovy servlet
 *
 * @author Marcel Overdijk
 * @author Guillaume Laforge
 *
 * @see groovy.servlet.GroovyServlet
 */
class CaelyfServlet extends GroovyServlet {
    
    /**
     * The script engine executing the Groovy scripts for this servlet
     */
    private GroovyScriptEngine gse
    
    @Override
    void init(ServletConfig config) {
        super.init(config)
        
        // Set up the scripting engine
        gse = createGroovyScriptEngine()
    }

    /**
     * Injects the default variables and services in the binding of Groovlets
     * as well as the variables contributed by plugins, and a logger.
     *  
     * @param binding the binding to enhance
     */
    @Override
    protected void setVariables(ServletBinding binding) {
        BindingEnhancer.bind(binding)
        PluginsHandler.instance.enrich(binding)
        binding.setVariable("log", GroovyLogger.forGroovletUri(super.getScriptUri(binding.request)))
    }

    /**
     * Service incoming requests applying the <code>CaelyfCategory</code>
     * and the other categories defined by the installed plugins.
     *
     * @param request the request
     * @param response the response
     * @throws IOException when anything goes wrong
     */
    @Override
    void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        use([CaelyfCategory, * PluginsHandler.instance.categories]) {
            PluginsHandler.instance.executeBeforeActions(request, response)
            try {
                // Get the script path from the request - include aware (GROOVY-815)
                final String scriptUri = getScriptUri(request)
                
                // Set it to HTML by default
                response.setContentType("text/html; charset="+encoding)
                
                // Set up the script context
                final ServletBinding binding = new ServletBinding(request, response, servletContext)
                setVariables(binding)
                
                Closure closure = new Closure(gse) {
                    public Object call() {
                        try {
                            return ((GroovyScriptEngine) getDelegate()).run(scriptUri, binding)
                        } catch (ResourceException e) {
                            throw new RuntimeException(e)
                        } catch (ScriptException e) {
                            throw new RuntimeException(e)
                        }
                    }
                }
                GroovyCategorySupport.use(ServletCategory.class, closure)
            } catch (e) {
                PluginsHandler.instance.executeHandleExceptionActions(e, request, response)
            }
            PluginsHandler.instance.executeAfterActions(request, response)
        }
    }
}
