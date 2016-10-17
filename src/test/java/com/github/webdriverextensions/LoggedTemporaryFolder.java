package com.github.webdriverextensions;

import org.junit.rules.TemporaryFolder;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class LoggedTemporaryFolder extends TemporaryFolder {
    public Statement apply(Statement base, Description description) {
        return statement(base);
    }

    private Statement statement(final Statement base) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before();
                try {
                    base.evaluate();
                } catch (Throwable e) {
                    System.out.println(Utils.directoryToString(getRoot()));
                    throw e;
                } finally {
                    after();
                }
            }
        };
    }
}
