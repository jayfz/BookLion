package co.harborbytes.booklion;


import org.junit.jupiter.api.DisplayNameGenerator;

import java.lang.reflect.Method;

public class ReplaceCamelCase extends DisplayNameGenerator.Standard {
    public ReplaceCamelCase() {
    }

    public String generateDisplayNameForClass(Class<?> testClass) {
        return this.replaceCapitals(super.generateDisplayNameForClass(testClass));
    }

    public String generateDisplayNameForNestedClass(Class<?> nestedClass) {
        return this.replaceCapitals(super.generateDisplayNameForNestedClass(nestedClass));
    }

    public String generateDisplayNameForMethod(Class<?> testClass, Method testMethod) {
        return this.replaceCapitals(testMethod.getName()).toLowerCase();
    }

    private String replaceCapitals(String name) {
        name = name.replaceAll("([A-Z])", " $1");
        name = name.replaceAll("([0-9]+)", " $1");
        return name;
    }
}
