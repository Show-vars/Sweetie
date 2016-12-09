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
package com.bunjlabs.fuga.templates;

import com.bunjlabs.fuga.views.ViewException;

/**
 *
 * @author Artem Shurygin <artem.shurygin@bunjlabs.com>
 */
public class TemplateReaderException extends ViewException {

    public TemplateReaderException() {
    }


    public TemplateReaderException(String message) {
        super(message);
    }

    public TemplateReaderException(String message, Throwable cause) {
        super(message, cause);
    }

    public TemplateReaderException(Throwable cause) {
        super(cause);
    }

    public TemplateReaderException(String message, int line, int col) {
        super("(" + line + ":" + col + ") " + message);
    }

    public TemplateReaderException(String message, Throwable cause, int line, int col) {
        super("(" + line + ":" + col + ") " + message, cause);
    }

}
