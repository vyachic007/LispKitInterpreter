package by.slava_borisov.LispKitInterpreter.console;

import by.slava_borisov.LispKitInterpreter.ast.Ast;
import by.slava_borisov.LispKitInterpreter.interpreter.Environment;
import by.slava_borisov.LispKitInterpreter.interpreter.Evaluator;
import by.slava_borisov.LispKitInterpreter.interpreter.LispValue;

import java.util.Scanner;

public class LispKitConsole {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("\nВведите выражение LispKit (0 — выход): ");

            if (!scanner.hasNextLine()) {
                break;
            }

            String input = scanner.nextLine().trim();

            if (input.equals("0")) {
                System.out.println("Работа завершена.");
                break;
            }

            try {
                Ast ast = new Ast();
                int root = ast.parse(input);

                System.out.println("\nИсходное выражение:");
                System.out.println(input);

                System.out.println("\nЛогическое АСД:");
                ast.printLogicalTree(root, "", true);

                System.out.println("\nПолное дерево CONS-ячеек:");
                System.out.println("Корневой адрес: " + root);
                ast.printConsTree(root);

                System.out.println("\nЯчейки памяти исходного AST:");
                ast.printMemory();

                /// Инициализация эмулятора и окружения
                Evaluator evaluator = new Evaluator(ast);
                Environment environment = new Environment();

                /// Передача построенного абстрактного синтаксического дерева в эмулятор
                LispValue result = evaluator.eval(root, environment);

                /// Получение преобразованного абстрактного синтаксического дерева в результате работы эмулятора
                int resultRoot = evaluator.toAst(result);

                System.out.println("\nРезультат вычисления:");
                System.out.println(ast.restore(resultRoot));

                /// Обход преобразованного АСД и вывод результата
                System.out.println("\nЛогическое АСД результата:");
                ast.printLogicalTree(resultRoot, "", true);

                System.out.println("\nПолное дерево CONS-ячеек результата:");
                System.out.println("Корневой адрес: " + resultRoot);
                ast.printConsTree(resultRoot);

                System.out.println("\nИтоговые ячейки памяти:");
                ast.printMemory();

            } catch (IllegalArgumentException e) {
                System.out.println("\nОшибка: " + e.getMessage());
            }
        }

        scanner.close();
    }
}