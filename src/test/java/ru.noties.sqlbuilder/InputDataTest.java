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

import java.util.Collection;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InputDataTest {

    @Test
    public void noPlaceholders() {

        final String[] in = {
                "select * from my_table",
                "select * from {} {} {}",
                "select * from $$$$ ????",
                "$ {} ? {}"
        };

        for (String sql: in) {

            final InputData data = InputData.create(sql);

            assertEquals(sql, data.formattedInput());
            assertEquals(0, data.argsLength());
            assertEquals(0, data.formatArgsLength());
            assertEquals(0, data.bindArgsLength());
            assertEquals(0, data.argumentNames().size());

            // whatever
            assertEquals(0, data.formatArgIndexes("from").size());
            assertEquals(0, data.bindArgIndexes("select").size());
        }
    }

    @Test
    public void noClosed() {

        final String[] in = {
                "select * from ${",
                "select * from ?{",
                "${",
                "?{"
        };

        for (String sql: in) {
            try {
                InputData.create(sql);
                //noinspection ConstantConditions
                assertTrue(false);
            } catch (IllegalStateException e) {
                assertTrue(true);
            }
        }
    }

    @Test
    public void formatNestedPlaceholder() {
        try {
            InputData.create("select * from ${table ${another_one}}");
            //noinspection ConstantConditions
            assertTrue(false);
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
    }

    @Test
    public void bindNestedPlaceholder() {
        try {
            InputData.create("select * from table where id = ?{name ?{}}");
            //noinspection ConstantConditions
            assertTrue(false);
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
    }

    @Test
    public void mixedNestedPlaceholder() {
        try {
            InputData.create("select * from ${table ?{nested}}");
            //noinspection ConstantConditions
            assertTrue(false);
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
    }

    @Test
    public void formatEmptyName() {
        try {
            InputData.create("select * from ${}");
            //noinspection ConstantConditions
            assertTrue(false);
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
    }

    @Test
    public void bindEmptyName() {
        try {
            InputData.create("select * from table where id = ?{}");
            //noinspection ConstantConditions
            assertTrue(false);
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
    }

    @Test
    public void mixedEmptyName() {
        try {
            InputData.create("select * from ${} where id = ?{}");
            //noinspection ConstantConditions
            assertTrue(false);
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
    }

    @Test
    public void formatFirst() {

        final InputData data = InputData.create("${table}");
        assertEquals("%s", data.formattedInput());
        assertEquals(1, data.argsLength());
        assertEquals(1, data.formatArgsLength());
        assertEquals(0, data.bindArgsLength());
        assertEquals(1, data.argumentNames().size());
        assertTrue(data.argumentNames().contains("table"));
        assertEquals((Integer) 0, data.formatArgIndexes("table").iterator().next());
    }

    @Test
    public void formatSingle() {

        final InputData data = InputData.create("select count(1) from ${table}");

        assertEquals("select count(1) from %s", data.formattedInput());
        assertEquals(1, data.argumentNames().size());
        assertEquals("table", data.argumentNames().iterator().next());
        assertEquals(1, data.argsLength());
        assertEquals(1, data.formatArgsLength());
        assertEquals(0, data.bindArgsLength());

        final Collection<Integer> indexes = data.formatArgIndexes("table");
        assertEquals(1, indexes.size());
        assertEquals((Integer) 0, indexes.iterator().next());
    }

    @Test
    public void formatMultiple() {

        final InputData data = InputData.create("select ${id} from ${table}");

        assertEquals("select %s from %s", data.formattedInput());

        assertEquals(2, data.argsLength());

        final Collection<String> names = data.argumentNames();
        assertEquals(2, names.size());
        assertTrue(names.contains("id"));
        assertTrue(names.contains("table"));

        assertEquals(2, data.formatArgsLength());
        assertEquals(0, data.bindArgsLength());

        assertEquals((Integer) 0, data.formatArgIndexes("id").iterator().next());
        assertEquals((Integer) 1, data.formatArgIndexes("table").iterator().next());
    }

    @Test
    public void formatRepeated() {

        final InputData data = InputData.create("select ${id} from ${table} where ${table} = ${id}");
        assertEquals("select %s from %s where %s = %s", data.formattedInput());

        final Collection<String> names = data.argumentNames();
        assertEquals(2, names.size());
        assertEquals(2, data.argsLength());
        assertEquals(4, data.formatArgsLength());
        assertEquals(0, data.bindArgsLength());

        final Collection<Integer> id = data.formatArgIndexes("id");
        final Iterator<Integer> idIterator = id.iterator();
        assertEquals(2, id.size());
        assertEquals((Integer) 0, idIterator.next());
        assertEquals((Integer) 3, idIterator.next());
        assertFalse(idIterator.hasNext());

        final Collection<Integer> table = data.formatArgIndexes("table");
        final Iterator<Integer> tableIterator = table.iterator();
        assertEquals(2, table.size());
        assertEquals((Integer) 1, tableIterator.next());
        assertEquals((Integer) 2, tableIterator.next());
        assertFalse(tableIterator.hasNext());
    }

    @Test
    public void formatModifiers() {

        final InputData data = InputData.create("select ${%d id} from ${%.2f id} where ${%S id} = ${id}");

        assertEquals("select %d from %.2f where %S = %s", data.formattedInput());

        assertEquals(1, data.argsLength());
        assertEquals(1, data.argumentNames().size());
        assertTrue(data.argumentNames().contains("id"));
        assertEquals(4, data.formatArgsLength());
        assertEquals(0, data.bindArgsLength());

        final Collection<Integer> indexes = data.formatArgIndexes("id");
        final Iterator<Integer> iterator = indexes.iterator();
        assertEquals(4, indexes.size());
        assertEquals((Integer) 0, iterator.next());
        assertEquals((Integer) 1, iterator.next());
        assertEquals((Integer) 2, iterator.next());
        assertEquals((Integer) 3, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void bindFirst() {

        final InputData data = InputData.create("?{table}");
        assertEquals("?", data.formattedInput());
        assertEquals(1, data.argsLength());
        assertEquals(0, data.formatArgsLength());
        assertEquals(1, data.bindArgsLength());
        assertEquals(1, data.argumentNames().size());
        assertTrue(data.argumentNames().contains("table"));
        assertEquals((Integer) 0, data.bindArgIndexes("table").iterator().next());
    }

    @Test
    public void bindSingle() {

        final InputData data = InputData.create("select * from table where id = ?{id}");
        assertEquals("select * from table where id = ?", data.formattedInput());

        assertEquals(1, data.argsLength());
        assertEquals(1, data.argumentNames().size());
        assertTrue(data.argumentNames().contains("id"));
        assertEquals(1, data.bindArgsLength());
        assertEquals(0, data.formatArgsLength());
        assertEquals((Integer) 0, data.bindArgIndexes("id").iterator().next());
    }

    @Test
    public void bindMultiple() {

        final InputData data = InputData.create("select * from table where id = ?{id} and name = ?{name}");
        assertEquals("select * from table where id = ? and name = ?", data.formattedInput());
        assertEquals(2, data.argsLength());
        assertEquals(2, data.bindArgsLength());
        assertEquals(0, data.formatArgsLength());

        final Collection<Integer> id = data.bindArgIndexes("id");
        final Iterator<Integer> idIterator = id.iterator();
        assertEquals(1, id.size());
        assertEquals((Integer) 0, idIterator.next());
        assertFalse(idIterator.hasNext());

        final Collection<Integer> name = data.bindArgIndexes("name");
        final Iterator<Integer> nameIterator = name.iterator();
        assertEquals(1, name.size());
        assertEquals((Integer) 1, nameIterator.next());
        assertFalse(nameIterator.hasNext());
    }

    @Test
    public void bindRepeated() {

        final InputData data = InputData.create("select * from table where id = ?{id} or id = ?{id} " +
                "and name = ?{name} or id = ?{id} or name = ?{name}");
        assertEquals("select * from table where id = ? or id = ? and name = ? or id = ? or name = ?", data.formattedInput());

        assertEquals(2, data.argsLength());
        assertEquals(5, data.bindArgsLength());
        assertEquals(0, data.formatArgsLength());

        final Collection<String> names = data.argumentNames();
        assertEquals(2, names.size());
        assertTrue(names.contains("id"));
        assertTrue(names.contains("name"));

        final Collection<Integer> id = data.bindArgIndexes("id");
        final Iterator<Integer> idIterator = id.iterator();
        assertEquals(3, id.size());
        assertEquals((Integer) 0, idIterator.next());
        assertEquals((Integer) 1, idIterator.next());
        assertEquals((Integer) 3, idIterator.next());
        assertFalse(idIterator.hasNext());

        final Collection<Integer> name = data.bindArgIndexes("name");
        final Iterator<Integer> nameIterator = name.iterator();
        assertEquals(2, name.size());
        assertEquals((Integer) 2, nameIterator.next());
        assertEquals((Integer) 4, nameIterator.next());
        assertFalse(nameIterator.hasNext());
    }

    @Test
    public void mixedMultiple() {

        final InputData data = InputData.create("select * from ${table} where id = ?{id}");
        assertEquals("select * from %s where id = ?", data.formattedInput());

        assertEquals(2, data.argsLength());
        assertEquals(1, data.bindArgsLength());
        assertEquals(1, data.formatArgsLength());

        assertEquals((Integer) 0, data.bindArgIndexes("id").iterator().next());
        assertEquals((Integer) 0, data.formatArgIndexes("table").iterator().next());

        assertTrue(data.argumentNames().contains("table"));
        assertTrue(data.argumentNames().contains("id"));
    }

    @Test
    public void mixedRepeated() {

        final InputData data = InputData.create("select * from ${table} where id = ?{id} and ${%S table} = 'table' or id = ?{id}");
        assertEquals("select * from %s where id = ? and %S = 'table' or id = ?", data.formattedInput());

        assertEquals(2, data.argsLength());
        assertEquals(2, data.formatArgsLength());
        assertEquals(2, data.bindArgsLength());

        final Collection<Integer> table = data.formatArgIndexes("table");
        final Iterator<Integer> tableIterator = table.iterator();
        assertEquals(2, table.size());
        assertEquals((Integer) 0, tableIterator.next());
        assertEquals((Integer) 1, tableIterator.next());
        assertFalse(tableIterator.hasNext());

        final Collection<Integer> id = data.bindArgIndexes("id");
        final Iterator<Integer> idIterator = id.iterator();
        assertEquals(2, table.size());
        assertEquals((Integer) 0, idIterator.next());
        assertEquals((Integer) 1, idIterator.next());
        assertFalse(idIterator.hasNext());
    }

    @Test
    public void singleCharPlaceholder() {
        final InputData data = InputData.create("select * from ${t} where id = ?{i}");
        assertEquals(2, data.argsLength());
        assertEquals(1, data.formatArgsLength());
        assertEquals(1, data.bindArgsLength());
        assertEquals((Integer) 0, data.formatArgIndexes("t").iterator().next());
        assertEquals((Integer) 0, data.bindArgIndexes("i").iterator().next());
    }
}
