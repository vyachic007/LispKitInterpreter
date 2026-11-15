package by.slava_borisov.LispKitInterpreter.interpreter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class LispValue {
    public enum Type {
        NUMBER,
        SYMBOL,
        CONS,
        FUNCTION,
        NIL
    }

    private final Type type;
    private final BigInteger number;
    private final String symbol;
    private final LispValue head;
    private final LispValue tail;
    private final int parameters;
    private final int body;
    private final Environment environment;

    private static final LispValue NIL_VALUE =
            new LispValue(Type.NIL, null, null, null, null, -1, -1, null);

    private LispValue(
            Type type,
            BigInteger number,
            String symbol,
            LispValue head,
            LispValue tail,
            int parameters,
            int body,
            Environment environment) {
        this.type = type;
        this.number = number;
        this.symbol = symbol;
        this.head = head;
        this.tail = tail;
        this.parameters = parameters;
        this.body = body;
        this.environment = environment;
    }

    public static LispValue number(BigInteger value) {
        return new LispValue(Type.NUMBER, value, null, null, null, -1, -1, null);
    }

    public static LispValue symbol(String value) {
        return new LispValue(Type.SYMBOL, null, value, null, null, -1, -1, null);
    }

    public static LispValue cons(LispValue head, LispValue tail) {
        return new LispValue(Type.CONS, null, null, head, tail, -1, -1, null);
    }

    public static LispValue function(
            int parameters,
            int body,
            Environment environment) {
        return new LispValue(
                Type.FUNCTION,
                null,
                null,
                null,
                null,
                parameters,
                body,
                environment);
    }

    public static LispValue nil() {
        return NIL_VALUE;
    }

    public Type getType() {
        return type;
    }

    public BigInteger getNumber() {
        return number;
    }

    public String getSymbol() {
        return symbol;
    }

    public LispValue getHead() {
        return head;
    }

    public LispValue getTail() {
        return tail;
    }

    public int getParameters() {
        return parameters;
    }

    public int getBody() {
        return body;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public boolean isNumber() {
        return type == Type.NUMBER;
    }

    public boolean isCons() {
        return type == Type.CONS;
    }

    public boolean isFunction() {
        return type == Type.FUNCTION;
    }

    public boolean isNil() {
        return type == Type.NIL;
    }

    public boolean isAtom() {
        return type != Type.CONS;
    }

    public List<LispValue> toList() {
        List<LispValue> result = new ArrayList<>();
        LispValue current = this;

        while (!current.isNil()) {
            if (!current.isCons()) {
                throw new IllegalArgumentException("Ожидался корректный список LispKit");
            }

            result.add(current.head);
            current = current.tail;
        }

        return result;
    }
}