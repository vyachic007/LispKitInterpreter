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

    private boolean isBoolean(LispValue value) {
        if (value.isNil()) {
            return true;                                      // F
        }
        return value.getType() == LispValue.Type.SYMBOL
                && "T".equals(value.getSymbol());              // T
    }

    public LispValue eval(int address, Environment environment) {
        if (ast.isAtom(address)) {
            String atom = ast.getAtom(address);

            if (atom.matches("-?\\d+")) {
                return LispValue.number(new BigInteger(atom));
            }

            if (atom.equalsIgnoreCase("T")) {
                return LispValue.symbol("T");
            }

            if (atom.equalsIgnoreCase("F")) {
                return LispValue.nil();
            }

            // Поиск значения переменной в текущем или родительском окружении
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
                // Создание новой CONS-ячейки из двух вычисленных аргументов
                return LispValue.cons(
                        eval(elements.get(1), environment),
                        eval(elements.get(2), environment));

            case "CAR":
                return car(eval(elements.get(1), environment));

            case "CDR":
                return cdr(eval(elements.get(1), environment));

            case "ATOM":
                return atom(eval(elements.get(1), environment));

            case "COND": {
                LispValue condition = eval(elements.get(1), environment);

                if (!isBoolean(condition)) {
                    throw new IllegalArgumentException(
                            "COND: условие должно быть T или F, получено: " + condition);
                }

                if (isTrue(condition)) {
                    return eval(elements.get(2), environment);
                }
                return eval(elements.get(3), environment);
            }

            case "LAMBDA":
                // Создание функции с параметрами, телом и текущим окружением
                return LispValue.function(
                        elements.get(1),
                        elements.get(2),
                        environment);

            case "LET":
                // Создание локального окружения и вычисление выражения
                return evalLet(elements, environment);

            case "LETREC":
                // Создание окружения для рекурсивных функций
                return evalLetRec(elements, environment);

            default:
                // Вызов пользовательской функции
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

        // Вычисление первого аргумента и проверка, что он является числом
        BigInteger left = requireNumber(eval(elements.get(1), environment));

        // Вычисление второго аргумента и проверка, что он является числом
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
                // Рекурсивное сравнение первых элементов и остатков списков
                return equal(left.getHead(), right.getHead())
                        && equal(left.getTail(), right.getTail());
            case FUNCTION:
                return left == right;
            default:
                return false;
        }
    }

    private LispValue car(LispValue value) {
        return value.getHead();
    }

    private LispValue cdr(LispValue value) {
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

        //списки имен и их значений
        List<String> names = new ArrayList<>();
        List<LispValue> values = new ArrayList<>();

        // Получение переменных и вычисление их значений
        for (int i = 2; i < elements.size(); i++) {
            // Одно объявление: имя, значение
            List<Integer> binding = ast.getElements(elements.get(i));

            // Имя переменной
            names.add(ast.getAtom(binding.get(0)));
            // Значение вычисляем в СТАРОМ environment
            values.add(eval(binding.get(1), environment));
        }

        // Добавление переменных в новое окружение
        for (int i = 0; i < names.size(); i++) {
            local.define(names.get(i), values.get(i));
        }

        // Вычисление основного выражения
        return eval(elements.get(1), local);
    }

    private LispValue evalLetRec(
            List<Integer> elements,
            Environment environment) {

        Environment local = new Environment(environment);

        for (int i = 2; i < elements.size(); i++) {
            //  // Получаем одно объявление: имя, значение
            List<Integer> binding = ast.getElements(elements.get(i));

            // Имя функции типо "SUM"
            String name = ast.getAtom(binding.get(0));

            // Разбираем (LAMBDA ...) на части: LAMBDA, параметры, тело
            List<Integer> lambda = ast.getElements(binding.get(1));

            // Кладём функцию в local; окружение = local → функция видит себя → рекурсия
            local.define(
                    name,
                    LispValue.function(lambda.get(1), lambda.get(2), local));
        }

        return eval(elements.get(1), local);
    }

    /// Вызов пользовательской функции
    private LispValue evalFunctionCall(
            List<Integer> elements,
            Environment environment) {

        String name = ast.getAtom(elements.get(0));

        // Поиск функции в текущем или родительском окружении
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

        // Передача аргументов в параметры функции
        for (int i = 0; i < expected; i++) {
            String parameter = ast.getAtom(parameters.get(i)); //SUM например

            LispValue value = eval(elements.get(i + 1), environment);

            // Сохранение значения аргумента под именем параметра
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

                //восстан-м список в обратном порядке
                for (int i = elements.size() - 1; i >= 0; i--) {
                    //преобразуем элемент и добавляем в начало списка
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