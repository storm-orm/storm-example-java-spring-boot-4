package st.orm.demo.imdb;

import st.orm.test.CapturedSql;
import st.orm.test.SqlCapture;

/** Prints every captured statement so the test output shows the generated SQL. */
public final class TestSupport {

    private TestSupport() {
    }

    public static void printStatements(SqlCapture capture, String label) {
        for (CapturedSql captured : capture.statements()) {
            System.out.println("[" + label + "] " + captured.operation() + ": " + captured.statement().replace('\n', ' '));
        }
    }
}
