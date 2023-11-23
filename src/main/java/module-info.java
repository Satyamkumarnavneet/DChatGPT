module com.example.dchatgpt {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.dchatgpt to javafx.fxml;
    exports com.example.dchatgpt;
}