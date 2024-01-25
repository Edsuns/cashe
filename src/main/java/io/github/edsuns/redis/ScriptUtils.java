/*
 * Copyright 2017-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.edsuns.redis;

import org.springframework.dao.NonTransientDataAccessException;

/**
 * Utilities for Lua script execution and result deserialization.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 2.0
 */
class ScriptUtils {

    private ScriptUtils() {
    }

    /**
     * Checks whether given {@link Throwable} contains a {@code NOSCRIPT} error. {@code NOSCRIPT} is reported if a script
     * was attempted to execute using {@code EVALSHA}.
     *
     * @param e the exception.
     * @return {@literal true} if the exception or one of its causes contains a {@literal NOSCRIPT} error.
     */
    static boolean exceptionContainsNoScriptError(Throwable e) {

        if (!(e instanceof NonTransientDataAccessException)) {
            return false;
        }

        Throwable current = e;
        while (current != null) {

            String exMessage = current.getMessage();
            if (exMessage != null && exMessage.contains("NOSCRIPT")) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}
