package by.slava_borisov.LispKitInterpreter.interpreter;

import by.slava_borisov.LispKitInterpreter.ast.Ast;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class Evaluator {
    private final Ast ast;

    public Evaluator(Ast ast) {
        this.ast = ast;
    }

    public LispValue eval(int address, Environment environment) {
        if (address == Ast.NIL) {
            return LispValue.nil();
        }

        if (ast.isAtom(address)) {
            String atom = ast.getAtom(address);

            if (atom.matches("-?\\d+")) {
                return LispValue.number(new BigInteger(atom));
            }

            if (atom.equalsIgnoreCase("T")) {
                return LispValue.symbol("T");
            }

            return environment.lookup(atom);
        }

        List<Integer> elements = ast.getElements(address);
        String operator = ast.getAtom(elements.get(0)).toUpperCase();

        switch (operator) {
            case "QUOTE":
                return quote(elements.get(1));
            case "ADD":
            case "SUB":
            case "MUL":
            case "MULT":
            case "DIV":
            case "DIVE":
            case "REM":
                return arithmetic(elements, environment, operator);
            case "EQUAL":
            case "LE":
            case "LEQ":
                return comparison(elements, environment, operator);
            case "CONS":
                return LispValue.cons(
                        eval(elements.get(1), environment),
                        eval(elements.get(2), environment));
            case "CAR":
                return car(eval(elements.get(1), environment));
            case "CDR":
                return cdr(eval(elements.get(1), environment));
            case "ATOM":
                return atom(eval(elements.get(1), environment));
            case "COND":
                if (isTrue(eval(elements.get(1), environment))) {
                    return eval(elements.get(2), environment);
                }
                return eval(elements.get(3), environment);
            case "LAMBDA":
                return LispValue.function(
                        elements.get(1),
                        elements.get(2),
                        environment);
            case "LET":
                return evalLet(elements, environment);
            case "LETREC":
                return evalLetRec(elements, environment);
            default:
                return evalFunctionCall(elements, environment);
        }
    }

    private LispValue quote(int address) {
        if (address == Ast.NIL) {
            return LispValue.nil();
        }

        if (ast.isAtom(address)) {
            String value = ast.getAtom(address);

            if (value.matches("-?\\d+")) {
                return LispValue.number(new BigInteger(value));
            }

            return LispValue.symbol(value);
        }

        List<Integer> elements = ast.getElements(address);
        LispValue result = LispValue.nil();

        for (int i = elements.size() - 1; i >= 0; i--) {
            result = LispValue.cons(quote(elements.get(i)), result);
        }

        return result;
    }

    private LispValue arithmetic(
            List<Integer> elements,
            Environment environment,
            String operation) {

        BigInteger left = requireNumber(eval(elements.get(1), environment));
        BigInteger right = requireNumber(eval(elements.get(2), environment));

        switch (operation) {
            case "ADD":
                return LispValue.number(left.add(right));
            case "SUB":
                return LispValue.number(left.subtract(right));
            case "MUL":
            case "MULT":
                return LispValue.number(left.multiply(right));
            case "DIV":
            case "DIVE":
                if (right.equals(BigInteger.ZERO)) {
                    throw new IllegalArgumentException("Деление на ноль");
                }
                return LispValue.number(left.divide(right));
            case "REM":
                if (right.equals(BigInteger.ZERO)) {
                    throw new IllegalArgumentException("Деление на ноль");
                }
                return LispValue.number(left.remainder(right));
            default:
                throw new IllegalStateException("Неизвестная арифметическая операция");
        }
    }

    private LispValue comparison(
            List<Integer> elements,
            Environment environment,
            String operation) {

        LispValue left = eval(elements.get(1), environment);
        LispValue right = eval(elements.get(2), environment);

        boolean result;

        if (operation.equals("EQUAL")) {
            result = equal(left, right);
        } else {
            result = requireNumber(left).compareTo(requireNumber(right)) <= 0;
        }

        if (result) {
            return LispValue.symbol("T");
        }

        return LispValue.nil();
    }

    private boolean equal(LispValue left, LispValue right) {
        if (left.getType() != right.getType()) {
            return false;
        }

        switch (left.getType()) {
            case NUMBER:
                return left.getNumber().equals(right.getNumber());
            case SYMBOL:
                return left.getSymbol().equals(right.getSymbol());
            case NIL:
                return true;
            case CONS:
                return equal(left.getHead(), right.getHead())
                        && equal(left.getTail(), right.getTail());
            case FUNCTION:
                return left == right;
            default:
                return false;
        }
    }

    private LispValue car(LispValue value) {
        if (value.isNil()) {
            return LispValue.nil();
        }

        if (!value.isCons()) {
            throw new IllegalArgumentException("CAR: ожидается список");
        }

        return value.getHead();
    }

    private LispValue cdr(LispValue value) {
        if (value.isNil()) {
            return LispValue.nil();
        }

        if (!value.isCons()) {
            throw new IllegalArgumentException("CDR: ожидается список");
        }

        return value.getTail();
    }

    private LispValue atom(LispValue value) {
        if (value.isAtom()) {
            return LispValue.symbol("T");
        }

        return LispValue.nil();
    }

    private LispValue evalLet(
            List<Integer> elements,
            Environment environment) {

        Environment local = new Environment(environment);
        List<String> names = new ArrayList<>();
        List<LispValue> values = new ArrayList<>();

        for (int i = 2; i < elements.size(); i++) {
            List<Integer> binding = ast.getElements(elements.get(i));

            names.add(ast.getAtom(binding.get(0)));
            values.add(eval(binding.get(1), environment));
        }

        for (int i = 0; i < names.size(); i++) {
            local.define(names.get(i), values.get(i));
        }

        return eval(elements.get(1), local);
    }

    private LispValue evalLetRec(
            List<Integer> elements,
            Environment environment) {

        Environment local = new Environment(environment);

        for (int i = 2; i < elements.size(); i++) {
            List<Integer> binding = ast.getElements(elements.get(i));
            String name = ast.getAtom(binding.get(0));
            List<Integer> lambda = ast.getElements(binding.get(1));

            local.define(
                    name,
                    LispValue.function(lambda.get(1), lambda.get(2), local));
        }

        return eval(elements.get(1), local);
    }

    private LispValue evalFunctionCall(
            List<Integer> elements,
            Environment environment) {

        String name = ast.getAtom(elements.get(0));
        LispValue function = environment.lookup(name);

        if (!function.isFunction()) {
            throw new IllegalArgumentException(name + " не является функцией");
        }

        List<Integer> parameters = ast.getElements(function.getParameters());
        int expected = parameters.size();
        int actual = elements.size() - 1;

        if (expected != actual) {
            throw new IllegalArgumentException(
                    "Функция " + name + ": ожидалось аргументов "
                            + expected + ", получено " + actual);
        }

        Environment local = new Environment(function.getEnvironment());

        for (int i = 0; i < expected; i++) {
            String parameter = ast.getAtom(parameters.get(i));
            LispValue value = eval(elements.get(i + 1), environment);

            local.define(parameter, value);
        }

        return eval(function.getBody(), local);
    }

    private BigInteger requireNumber(LispValue value) {
        if (!value.isNumber()) {
            throw new IllegalArgumentException("Ожидалось числовое значение");
        }

        return value.getNumber();
    }

    private boolean isTrue(LispValue value) {
        return !value.isNil();
    }

    public int toAst(LispValue value) {
        switch (value.getType()) {
            case NUMBER:
                return ast.createAtom(value.getNumber().toString());
            case SYMBOL:
                return ast.createAtom(value.getSymbol());
            case NIL:
                return Ast.NIL;
            case CONS:
                int tail = Ast.NIL;
                List<LispValue> elements = value.toList();

                for (int i = elements.size() - 1; i >= 0; i--) {
                    tail = ast.createCons(toAst(elements.get(i)), tail);
                }

                return tail;
            case FUNCTION:
                int lambda = ast.createAtom("LAMBDA");
                int body = ast.createCons(value.getBody(), Ast.NIL);
                int parameters = ast.createCons(value.getParameters(), body);

                return ast.createCons(lambda, parameters);
            default:
                return Ast.NIL;
        }
    }
}