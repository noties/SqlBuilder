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

import org.junit.Test;

import static org.junit.Assert.*;

public class SqlStatementBuilderTest {

    @Test
    public void noNamedArguments() {
        final SqlStatementBuilder builder = SqlStatementBuilder.create("select * from table");
        builder.bind("not_present", 123L);
        try {
            builder.sqlStatement();
            //noinspection ConstantConditions
            assertTrue(false);
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
    }

    @Test
    public void noBoundArguments() {
        final SqlStatementBuilder builder = SqlStatementBuilder.create("select * from ${table}");
        try {
            builder.sqlStatement();
            //noinspection ConstantConditions
            assertTrue(false);
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
    }

    @Test
    public void hasNamedArgumentsAfterClear() {
        final SqlStatementBuilder builder = SqlStatementBuilder.create("select * from ${table}");
        builder.bind("table", "yo");
        assertEquals("select * from yo", builder.sqlStatement());
        assertNull(builder.sqlBindArguments());

        builder.clearBindings();

        try {
            builder.sqlStatement();
            //noinspection ConstantConditions
            assertTrue(false);
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
    }

    @Test
    public void boundNotPresent() {
        final SqlStatementBuilder builder = SqlStatementBuilder.create("select * from ${table}");
        builder.bind("id", 99);
        try {
            builder.sqlStatement();
            //noinspection ConstantConditions
            assertTrue(false);
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
    }

    @Test
    public void mismatchArgumentLengthEqualsHasNotBound() {
        final SqlStatementBuilder builder = SqlStatementBuilder.create("select ${columns} from ${table}");
        builder.bind("id", 76);
        builder.bind("name", "hello");
        try {
            builder.sqlStatement();
            //noinspection ConstantConditions
            assertTrue(false);
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
    }

    @Test
    public void formatWithNewArguments() {

        final String[] values = {
                "first",
                "second",
                "third"
        };

        final SqlStatementBuilder builder = SqlStatementBuilder.create("select * from ${table}");
        for (String value: values) {
            builder.clearBindings();
            builder.bind("table", value);
            assertEquals("select * from " + value, builder.sqlStatement());
            assertNull(builder.sqlBindArguments());
        }
    }

    @Test
    public void argumentBound() {
        final SqlStatementBuilder builder = SqlStatementBuilder.create("select * from table where id = ?{id}");
        builder.bind("id", 33L);
        assertEquals("select * from table where id = ?", builder.sqlStatement());
        assertArrayEquals(new Object[] { 33L }, builder.sqlBindArguments());
    }

    @Test
    public void mixedArguments() {

        final Object[][] values = {
                { "id", 33L },
                { "wk", "no one" },
                { "yeah", true }
        };

        final SqlStatementBuilder builder = SqlStatementBuilder.create("select * from table where ${column_name} = ?{column_value}");
        for (Object[] array: values) {
            builder.clearBindings();
            builder.bind("column_name", array[0]);
            builder.bind("column_value", array[1]);
            assertEquals("select * from table where " + array[0] + " = ?", builder.sqlStatement());
            assertArrayEquals(new Object[] { array[1] }, builder.sqlBindArguments());
        }
    }

    @Test
    public void bindingNullArguments() {
        final SqlStatementBuilder builder = SqlStatementBuilder.create("update ${table} set id = ?{id}, name = ?{name} where time = ?{time}");
        builder.bind("table", "table");
        builder.bind("id", 45);
        builder.bind("name", null);
        builder.bind("time", -1L);

        assertEquals("update table set id = ?, name = ? where time = ?", builder.sqlStatement());
        assertArrayEquals(new Object[] { 45, null, -1L }, builder.sqlBindArguments());
    }
}

