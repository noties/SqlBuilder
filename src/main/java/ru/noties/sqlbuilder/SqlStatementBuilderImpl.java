package ru.noties.sqlbuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

class SqlStatementBuilderImpl extends SqlStatementBuilder {

    private final String mInput;
    private final Locale mLocale;

    private InputData mInputData;
    private String mSqlStatement;
    private Object[] mSqlBindArgs;
    private Map<String, Object> mArgumentsMap;

    private boolean mChanged;

    SqlStatementBuilderImpl(@Nonnull String input, @Nonnull Locale locale) {
        mInput = input;
        mLocale = locale;
        mChanged = true; // initial value
    }

    @Override
    public SqlStatementBuilder bind(@Nonnull String name, @Nullable Object value) {
        if (mArgumentsMap == null) {
            mArgumentsMap = new HashMap<>(3);
        }
        mChanged = true;
        mArgumentsMap.put(name, value);
        return this;
    }

    // okay, the thing is... if we have formatArgs, we might need to construct a new sqlString
    // if not, we might skip it and re-use

    @Override
    public String sqlStatement() {

        if (mChanged) {
            bind();
        }

        return mSqlStatement;
    }

    @Override
    public Object[] sqlBindArguments() {

        if (mChanged) {
            bind();
        }

        return mSqlBindArgs;
    }

    private void bind() {

        final InputData data;
        if (mInputData == null) {
            data = mInputData = InputData.create(mInput);
        } else {
            data = mInputData;
        }

        // next detect if we need to `string.format` input
        final int argsLength = data.argsLength();

        final String sqlStatement;
        final Object[] sqlBindArgs;

        // if we have none, just return unmodified
        if (argsLength == 0) {

            // we just need to validate that our bindArgsMap is null or empty
            if (mArgumentsMap != null && mArgumentsMap.size() > 0) {

                throw new IllegalStateException("Input string has no named arguments, but `bind` method was " +
                        "called. Most likely there was an error constructing an input string: `" +
                        mInput + "`");

            }

            sqlStatement = mInput;
            sqlBindArgs = null;

        } else {

            final int boundArgsLength = mArgumentsMap == null
                    ? 0
                    : mArgumentsMap.size();

            if (boundArgsLength == 0) {

                throw new IllegalStateException("Input string has named arguments, but they are not " +
                        "bound. Please make sure to bind all named arguments. Input: `" +
                        mInput + "`, expected arguments: `" + data.argumentNames() + "`");

            } else if (boundArgsLength != argsLength) {

                // bound arguments mismatch
                // let's detect what arguments were not bound
                throw mismatchException(mInput, data, mArgumentsMap);
            }

            // okay, what we do here is:
            // if we have

            final int formatArgsLength  = data.formatArgsLength();
            final int bindArgsLength    = data.bindArgsLength();

            final Object[] formatArgs = formatArgsLength > 0
                    ? new Object[formatArgsLength]
                    : null;

            final Object[] bindArgs = bindArgsLength > 0
                    ? new Object[bindArgsLength]
                    : null;

            String key;
            Object value;

            int formatAdded = 0;
            int bindAdded = 0;

            for (Map.Entry<String, Object> entry: mArgumentsMap.entrySet()) {

                key = entry.getKey();
                value = entry.getValue();

                for (int formatIndex: data.formatArgIndexes(key)) {
                    //noinspection ConstantConditions
                    formatArgs[formatIndex] = value;
                    formatAdded += 1;
                }

                for (int bindIndex: data.bindArgIndexes(key)) {
                    //noinspection ConstantConditions
                    bindArgs[bindIndex] = value;
                    bindAdded += 1;
                }
            }

            if (formatAdded != formatArgsLength
                    || bindAdded != bindArgsLength) {
                throw mismatchException(mInput, data, mArgumentsMap);
            }

            if (formatArgsLength > 0) {
                sqlStatement = String.format(mLocale, data.formattedInput(), formatArgs);
            } else {
                // this call can be cached by InputData for example
                sqlStatement = data.formattedInput();
            }

            sqlBindArgs = bindArgs;
        }

        mSqlStatement = sqlStatement;
        mSqlBindArgs = sqlBindArgs;

        mChanged = false;
    }

    @Override
    public void clearBindings() {
        mChanged = true;
        if (mArgumentsMap != null) {
            mArgumentsMap.clear();
        }
    }

    private static IllegalStateException mismatchException(String input, InputData data, Map<String, Object> argumentsMap) {

        // bound arguments mismatch
        // let's detect what arguments were not bound
        final Set<String> notFound = new HashSet<>();
        for (String key: data.argumentNames()) {
            if (argumentsMap.get(key) == null) {
                notFound.add(key);
            }
        }

        return new IllegalStateException("Some named arguments are not bound: `" + notFound.toString() + "`. " +
                "Input: `" + input + "`");
    }
}
