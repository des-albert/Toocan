module Toocan {
    requires transitive javafx.fxml;
    requires transitive javafx.base;
    requires transitive javafx.graphics;
    requires transitive javafx.controls;
    requires transitive gson;
    requires transitive java.sql;
    exports com.db to javafx.graphics, javafx.fxml;
    opens com.db to javafx.fxml, javafx.base, gson;
}