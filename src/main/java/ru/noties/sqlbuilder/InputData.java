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
