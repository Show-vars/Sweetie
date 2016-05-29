/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bunjlabs.fugaframework.views;

import com.bunjlabs.fugaframework.foundation.Context;
import com.bunjlabs.fugaframework.templates.TemplateNotFoundException;
import com.bunjlabs.fugaframework.templates.TemplateRenderException;
import java.io.PrintStream;

public interface ViewRenderer {

    public void render(String name, Context ctx, PrintStream output) throws TemplateNotFoundException, TemplateRenderException;

    public String renderToString(String name, Context ctx) throws TemplateNotFoundException, TemplateRenderException;

}