/*
 * Copyright 2017 Dimitry Ivanov (mail@dimitryivanov.ru)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.noties.sqlbuilder;

import javax.annotation.Nonnull;
import java.util.Collection;

abstract class InputData {

    // might throw IllegalStateException if cannot parse the input
    // 1. nested placeholders, aka `${ ${}}`
    // 2. empty name for a placeholder, aka `${}`
    static InputData create(@Nonnull String input) {
        //noinspection ConstantConditions
        if (input == null) {
            throw new NullPointerException("`input` argument cannot be null");
        }
        return new InputDataImpl(input);
    }

    // okay, this should return ready-for-use string
    // `select * from %s where name = ?`
    abstract String formattedInput();

    abstract Collection<Integer> bindArgIndexes(String name);
    abstract Collection<Integer> formatArgIndexes(String name);

    abstract int bindArgsLength();
    abstract int formatArgsLength();
    abstract int argsLength();

    // for debugging purpose
    abstract Collection<String> argumentNames();
}
