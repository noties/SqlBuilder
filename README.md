# SqlBuilder

This is Java tiny utility library to create SQL statements. Allows specifying named binding arguments.

```java
 final SqlStatementBuilder builder = SqlStatementBuilder.create("select ${columns} from ${table} where id = ?{id}");
 builder.bind("columns", "id, name, time");
 builder.bind("table", "my_table");
 builder.bind("id", 33L);
 builder.sqlStatement(); // `select id, name, time from my_table where id = ?`
 builder.sqlBindArguments(); // [33L] (which is an Object[] with one element)
```

## Installation
[![Maven Central](https://img.shields.io/maven-central/v/ru.noties/sqlbuilder.svg)](http://search.maven.org/#search|ga|1|g%3A%22ru.noties%22%20AND%20a%3A%22sqlbuilder%22)

## Usage

There are two types of arguments:
* Simple SQL arguments (which are represented by `?` symbol), declared with `?{}`
* Arguments that are replaced in the SQL statement itself (with the help of `String.format()`), declared with `${}`

Every named argument must have a name, so `${}` or `?{}` - are not valid, whilst `${name}` or `?{name}` are OK.

Format and SQL binding arguments are shared, so: `select * from ${table} where table_name = ?{table}` is OK and requires only one argument to be bound (in this case it's called `table`)

Format arguments (that are inserted into SQL statement) are declared with `${}`.

If some specific formatting is required `${modifier name}` can be used, for example: `select id, ${%.2f ratio} from table`. All modifiers that are allowed into `String#format(String, Object...)` are allowed here. If formatting is required for a specific `Locale` then `SqlStatementBuilder#create(String, Locale)` can be used.

By default SqlStatementBuilder uses `Locale#US` for formatting.

SQL statement and arguments are evaluated lazily, so if there is an error parsing input string an exception will be thrown on one of the calls to: `SqlStatementBuilder#sqlStatement()`, `SqlStatementBuilder#sqlBindArguments()`

Right now exception is thrown if:
 * `SqlStatementBuilder#create(String)` or `SqlStatementBuilder#create(String, Locale)` are called with NULL as an `input` parameter
 * input has named parameters, but they are not bound (via `SqlStatementBuilder#bind(String, Object)` call
 * input has no named parameters, but `SqlStatementBuilder#bind(String, Object)` was called
 * named parameter has no name `${}` or `?{}`
 * named parameters are nested, for example: `${table ?{name}}`
 * named parameter is not closed, for example: `${table`, `?{name`

In all cases (except `SqlStatementBuilder#create(String)` which throws `NullPointerException`) an `IllegalStateException` is thrown

In order to create an instance of SqlStatementBuilder one of the static factory methods must be called:
  * `SqlStatementBuilder#create(String)`
  * `SqlStatementBuilder#create(String, Locale)`

## Limitations

Please note that SqlStatementBuilder is **not thread safe**. There is no any kind of synchronisation. If this class is intended to be used by multiple threads, user of this class must provide own means of synchronisation.

Also, all binding arguments must be present during initial creation, for example:
```java
SqlStatementBuilder.create("select * from ${table}${selection};")
    .bind("table", "table_name")
    .bind("selection", " where id = ?{id}") // not valid, cannot add placeholders by formatting
    .bind("id", 45); // exception will be thrown as no named argument `id` is present during intial creation
```


## License

```
  Copyright 2017 Dimitry Ivanov (mail@dimitryivanov.ru)

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
```