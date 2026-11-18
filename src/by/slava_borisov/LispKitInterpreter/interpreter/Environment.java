package by.slava_borisov.LispKitInterpreter.interpreter;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Map<String, LispValue> values = new HashMap<>();
    private final Environment parent;

    public Environment() {
        this(null);
    }

    public Environment(Environment parent) {
        this.parent = parent;
    }

    public void define(String name, LispValue value) { //доб переменной в текущ окр-е
        values.put(name, value);
    }

    public LispValue lookup(String name) {
        LispValue value = values.get(name); // Поиск переменной в текущем окружении

        if (value != null) {
            return value;
        }

        if (parent != null) {
            return parent.lookup(name);
        }

        throw new IllegalArgumentException("Неопределённая переменная: " + name);
    }
}
