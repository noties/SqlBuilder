package ru.noties.sqlbuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Utility class to create SQL statements. Allows specifying named binding arguments.
 * There are 2 named arguments types:
 *      * simple SQL arguments (which are represented via `?` symbol)
 *      * arguments that are replaced in the SQL statement itself (like String.format())
 * For example:
 * {@code
 *      final SqlStatementBuilder builder = SqlStatementBuilder.create("select ${columns} from ${table} where id = ?{id}");
 *      builder.bind("columns", "id, name, time");
 *      builder.bind("table", "my_table");
 *      builder.bind("id", 33L);
 *      builder.sqlStatement(); // `select id, name, time from my_table where id = ?`
 *      builder.sqlBindArguments(); // [33L] (which is an Object[] with one element)
 * }
 *
 * Every named argument must have a name, so `${}` or `?{}` - are not valid, whilst
 * `${name}` or `?{name}` are OK.
 *
 * Format and SQL binding arguments are shared, so:
 * `select * from ${table} where table_name = ?{table}` is OK and requires only one
 * argument to be bound (in this case it's called `table`)
 *
 * Format arguments (that are inserted into SQL statement) are declared with `${}`.
 * If some specific formatting is required `${modifier name}` can be used, for example:
 * `select id, ${%.2f ratio} from table`.
 * All modifiers that are allowed into {@link String#format(String, Object...)} are allowed here.
 * If formatting is required for a specific {@link Locale} then {@link #create(String, Locale)} can be used.
 * By default SqlStatementBuilder uses {@link Locale#US} for formatting.
 *
 * SQL statement and arguments are evaluated lazily, so if there is an error parsing input string
 * an exception will be thrown on one of the calls to: {@link #sqlStatement()}, {@link #sqlBindArguments()}
 *
 * Right now exception is thrown if:
 *      * {@link #create(String)} or {@link #create(String, Locale)} are called with NULL as an `input` parameter
 *      * input has named parameters, but they are not bound (via {@link #bind(String, Object)} call
 *      * input has no named parameters, but {@link #bind(String, Object)} was called
 *      * named parameter has no name `${}` or `?{}`
 *      * named parameters are nested, for example: `${table ?{name}}`
 *      * named parameter is not closed, for example: `${table`, `?{name`
 * In all cases (except {@link #create(String)} which throws {@link NullPointerException}}} an
 * {@link IllegalStateException} is thrown
 *
 * In order to create an instance of SqlStatementBuilder one of the static factory methods must be called:
 * {@link #create(String)}
 * {@link #create(String, Locale)}
 *
 * This class is not thread safe. There is no any kind of synchronisation. If this class is
 * intended to be used by multiple threads, user of this class must provide own means of synchronisation
 */
@SuppressWarnings("WeakerAccess")
public abstract class SqlStatementBuilder {

    /**
     * The same as {@link #create(String, Locale)} with `null` as a locale parameter
     * @see #create(String, Locale)
     */
    public static SqlStatementBuilder create(@Nonnull String input) {
        return create(input, null);
    }

    /**
     * @param input string value to be used to construct SQL statement. Must not be null
     * @param locale {@link Locale} object to be used in SQL statement formatting. If passed null
     *                             the {@link Locale#US} will be used
     * @return an instance of {@link SqlStatementBuilder}
     */
    public static SqlStatementBuilder create(@Nonnull String input, @Nullable Locale locale) {
        //noinspection ConstantConditions
        if (input == null) {
            throw new NullPointerException("`input` string parameter cannot be null");
        }
        final Locale outLocale = locale == null
                ? Locale.US // US is used as default locale for String.format() call (if present)
                : locale;
        return new SqlStatementBuilderImpl(input, outLocale);
    }

    /**
     * Does not allow null as a `value` parameter. In SQL the better way is to use:
     * `is null` statement
     * @param name of the parameter to be bound
     * @param value value of the binding argument
     * @return self to chain calls
     */
    public abstract SqlStatementBuilder bind(@Nonnull String name, @Nonnull Object value);

    /**
     * Clears all bindings, that were previously bound by {@link #bind(String, Object)} calls
     */
    public abstract void clearBindings();

    /**
     * @return a SQL statement with all substitutions. If there are no named arguments the `input`
     *      string will be returned without modification (passed to {@link #create(String)} call)
     * @throws IllegalStateException if there was an error parsing/preparing the SQL statement
     */
    public abstract String sqlStatement() throws IllegalStateException;

    /**
     * @return an object array of SQL binding arguments or null if there are none
     * @throws IllegalStateException if there was an error parsing/preparing the SQL statement
     */
    public abstract Object[] sqlBindArguments() throws IllegalStateException;
}
