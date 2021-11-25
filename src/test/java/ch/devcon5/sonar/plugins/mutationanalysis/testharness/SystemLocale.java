package ch.devcon5.sonar.plugins.mutationanalysis.testharness;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.Locale;

public class SystemLocale implements TestRule {


    private final Locale overrideLocale;
    private Locale defaultLocale = Locale.getDefault();

    public SystemLocale(Locale locale) {
        this.overrideLocale = locale;
    }

    public static SystemLocale overrideDefault(Locale locale){
        return new SystemLocale(locale);
    }

    @Override
    public Statement apply(Statement statement, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    defaultLocale = Locale.getDefault();
                    Locale.setDefault(overrideLocale);
                    statement.evaluate();
                } finally {
                    Locale.setDefault(defaultLocale);
                }
            }
        };
    }
}
