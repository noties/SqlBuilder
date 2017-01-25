package ru.noties.sqlbuilder;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class InputDataImpl extends InputData {

    private Set<String> mArgumentsNames;

    private String mFormattedInput; // with all substitutions
    private Map<String, List<Integer>> mFormatArgs;
    private Map<String, List<Integer>> mBindArgs;

    private int mFormatArgsLength;
    private int mBindArgsLength;

    InputDataImpl(@Nonnull String input) {
        prepare(input);
    }

    private void prepare(String input) {

        int formatArgsLength = 0;
        int bindArgsLength = 0;

        final Set<String> names = new HashSet<>(3);

        final Map<String, List<Integer>> formatArgs = new HashMap<>(3);
        final Map<String, List<Integer>> bindArgs = new HashMap<>(3);

        final StringBuilder builder = new StringBuilder();

        // so, we are tracking the `${}` to format and `?{}` to place-hold

        boolean isFormatArg = false;
        boolean isBindArg = false;

        int start = -1;
        char c;

        for (int i = 0, length = input.length(); i < length; i++) {
            c = input.charAt(i);
            if ('{' == c) {
                // check the previous char to detect what kind of arg is that
                if (i > 0) {

                    isFormatArg = '$' == input.charAt(i - 1);
                    isBindArg = !isFormatArg && '?' == input.charAt(i - 1);

                    if (start != -1 && (isFormatArg || isBindArg)) {
                        throw new IllegalStateException("Nested placeholders detected at index: `" + i + "` " +
                                ". Input: `" + input + "`");
                    }

                    if (isBindArg || isFormatArg) {
                        start = i - 1;

                        // we also need to remove previous char
                        builder.setLength(builder.length() - 1);
                    } else {
                        builder.append('{');
                    }
                }
            } else if ('}' == c && start != -1) {

                // extract name
                final int left = start + 2;
                if ((i - left) < 2) {
                    throw new IllegalStateException("Named placeholder has empty name at index: `" + start + "`. " +
                            "Input: `" + input + "`");
                }

                final String name = input.substring(left, i);
                if (isBindArg) {
                    // okay, here is what we are doing
                    List<Integer> indexes = bindArgs.get(name);
                    if (indexes == null) {
                        indexes = new ArrayList<>(3);
                        bindArgs.put(name, indexes);
                    }
                    indexes.add(bindArgsLength++);
                    names.add(name);
                    builder.append('?');
                } else if (isFormatArg) {

                    // here is another spin: we can have modifiers here

                    final String modifier;
                    final String argumentName;

                    final String[] split = name.split(" ");

                    if (split.length == 1) {
                        modifier = "%s";
                        argumentName = name;
                    } else {
                        modifier = split[0];
                        argumentName = split[1];
                    }

                    List<Integer> indexes = formatArgs.get(argumentName);
                    if (indexes == null) {
                        indexes = new ArrayList<>(3);
                        formatArgs.put(argumentName, indexes);
                    }
                    names.add(argumentName);
                    indexes.add(formatArgsLength++);
                    builder.append(modifier);
                } else {
                    throw new IllegalStateException("Unexpected state");
                }

                start = -1;
                isBindArg = false;
                isFormatArg = false;

            } else if (start == -1) {
                builder.append(c);
            }
        }

        // a check if arguments are closed
        if (start != -1
                || isBindArg
                || isFormatArg) {
            throw new IllegalStateException("Bind argument is not closed. Input: `" + input + "`");
        }

        mArgumentsNames = names;
        mFormattedInput = builder.toString();
        mFormatArgs = formatArgs;
        mBindArgs = bindArgs;
        mFormatArgsLength = formatArgsLength;
        mBindArgsLength = bindArgsLength;
    }

    @Override
    public String formattedInput() {
        return mFormattedInput;
    }

    @Override
    public Collection<Integer> bindArgIndexes(String name) {
        final List<Integer> out;
        final List<Integer> indexes = mBindArgs.get(name);
        if (indexes == null) {
            //noinspection unchecked
            out = Collections.EMPTY_LIST;
        } else {
            out = Collections.unmodifiableList(indexes);
        }
        return out;
    }

    @Override
    public Collection<Integer> formatArgIndexes(String name) {
        final List<Integer> out;
        final List<Integer> indexes = mFormatArgs.get(name);
        if (indexes == null) {
            //noinspection unchecked
            out = Collections.EMPTY_LIST;
        } else {
            out = Collections.unmodifiableList(indexes);
        }
        return out;
    }

    @Override
    public int bindArgsLength() {
        return mBindArgsLength;
    }

    @Override
    public int formatArgsLength() {
        return mFormatArgsLength;
    }

    @Override
    public int argsLength() {
        return mArgumentsNames.size();
    }

    @Override
    public Collection<String> argumentNames() {
        return Collections.unmodifiableSet(mArgumentsNames);
    }
}
