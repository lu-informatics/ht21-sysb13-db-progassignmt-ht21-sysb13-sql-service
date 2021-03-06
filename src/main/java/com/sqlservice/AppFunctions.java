package com.sqlservice;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.util.Callback;
import javafx.util.Duration;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;


public class AppFunctions {
    //Samlar alla metoder som är universella som kan kalla på dem från alla andra klasser, generella metoder.

    public static void updateSearchableTableView(TableView tableView, TextField searchField, ResultSet resultSet) throws SQLException {
        tableView.getColumns().clear();// tömmer tableview
        AppFunctions.setTableColumnNames(tableView, resultSet); //sätter columnName efter resultSetets columnName
        ObservableList<ObservableList> dataList = AppFunctions.fillList(resultSet); //skapar listan med de object som ska synas i tableview
        FilteredList<ObservableList> filteredData = new FilteredList<>(dataList, b -> true);//Wrappar dataList i en FilteredList.
        searchField.textProperty().addListener((observable, oldValue, newValue) ->{ //lägger till en listener som lyssnar efter när man skriver in något i searchfieldet
            //oldValue ändras aldrig, men det gör newValue.
            filteredData.setPredicate( row -> {
                if(newValue == null || newValue.isEmpty()){ //ifall inget är skrivet i sökfältet visas hela resultsetet!
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase(Locale.ROOT); //Ifall någon entitet i resultsetet överensstämmer med söksträngen returneras den/dessa!
                //fail safe.
                return row.toString().toLowerCase().contains(lowerCaseFilter); //returnerar de objekt som innehåller det man skrivit in
            });
        });
        //LÄGGER IN DATA I TABLEVIEW
        tableView.setItems(filteredData);
        ContosoConnection.connectionClose(resultSet);
    }

    //Metod som bara används i updateSearchableTableView()
    public static void setTableColumnNames(TableView tableView, ResultSet resultSet) throws SQLException{
        //TABLE COLUMN NAMES ADDED DYNAMICALLY
        for(int i=0; i<resultSet.getMetaData().getColumnCount(); i++) {
            final int j = i;
            TableColumn col = new TableColumn(resultSet.getMetaData().getColumnName(i + 1));
            /*Ny tablecolumn. För varje kolumn skapar vi en ny kolumn som har namnet av den kolumnamnet på index i + 1.
             * Första kolumnen är på index 1 i SQL och inte 0 som i en Array. **/

            //sätter vilket värde det ska vara i den kolumnen.
            col.setCellValueFactory((Callback<TableColumn.CellDataFeatures<ObservableList, String>, ObservableValue<String>>) param -> {
                if(param.getValue().get(j) == null){
                    return new SimpleStringProperty("NULL");
                }else {
                    return new SimpleStringProperty(param.getValue().get(j).toString());
                }
            });
            tableView.getColumns().addAll(col); //Lägger till kolumner i tableView som vi har på rad 81. (kallar på metoden)
        }
    }

    //Metod som bara används i updateSearchableTableView()
    public static ObservableList<ObservableList> fillList(ResultSet resultSet) throws SQLException {
        //Data added to ObservableList
        ObservableList<ObservableList> dataSet = FXCollections.observableArrayList();

        while (resultSet.next()) { //Itererar över resultSet. För första raden skapar vi en observable list (kallar för row).
            ObservableList<String> row = FXCollections.observableArrayList();   //listan som är raderna.
            //För varje kolumn i den raden, lägger vi till värdet i den observablelist.
            //Den lägger till alla värden från resultSet till observableList.
            for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                //Iterate Column
                row.add(resultSet.getString(i));
            }
            dataSet.add(row); //lägger till row(är en observableList) i dataList.
        }
        return dataSet;
    }

    public static String getValueOfCell(TableView tableView, int columnIndexOfWantedCell){

            ObservableList<ObservableList> row = tableView.getSelectionModel().getSelectedItems();//Hämtar den markerade listan(en rad). Lista som ligger i lista.
            ObservableList<ObservableList> objectList = row.get(0); //columnindex 0.
            Object object = objectList.get(columnIndexOfWantedCell); //hämtar objekt(ID, name) på index i listan.
            String cellValue = object.toString(); //gör objektet till en sträng för att returnera denna!
        return cellValue;
    }

    public static String getUniqueCode(String dbTableName, String idColumnName, String startingLetter) throws SQLException{
        DataAccessLayer dataAccessLayer = new DataAccessLayer();
        ResultSet resultSet = dataAccessLayer.getAllFromTable(dbTableName);
        //Antingen från Student eller Course
        ArrayList<String> arrayList = new ArrayList<>();
        while (resultSet.next()){
            arrayList.add(resultSet.getString(idColumnName));
        }
        ContosoConnection.connectionClose(resultSet);
        while (true) {
            int randomNum = ThreadLocalRandom.current().nextInt(10000, 99999);
            String randomCode = startingLetter + randomNum;
            if (!(arrayList.contains(randomCode))) {

                return randomCode;
            }
        }
    }

    public static void changeView(Parent root, Button viewButton, AnchorPane parentContainer, AnchorPane anchorRoot){
        Scene scene = viewButton.getScene();
        root.translateYProperty().set(scene.getHeight());
        parentContainer.getChildren().add(root);

        Timeline timeline = new Timeline();
        KeyValue kv = new KeyValue(root.translateYProperty(), 0, Interpolator.DISCRETE);
        KeyFrame kf = new KeyFrame(Duration.seconds(0.02),kv);
        timeline.getKeyFrames().add(kf);
        timeline.setOnFinished(event1 -> {
            parentContainer.getChildren().remove(anchorRoot);
        });
        timeline.play();
    }

    //Error-hantering för udda SQL-fel
    public static void unexpectedSQLError(TextArea textArea, SQLException exception){

        String exceptionMessage = exception.getMessage();
        int errorCode = exception.getErrorCode();
        if(exceptionMessage.contains("pk_student")){
            textArea.setText("StudentID is not unique, try again with a unique StudentID");
        }else if(exceptionMessage.contains("uc_studentSSN")){
            textArea.setText("Student with this social security number already exist");
        }else if(exceptionMessage.contains("pk_course")){
            textArea.setText("Course with this course code already exists, try again with unique course code");
        }else if(exceptionMessage.contains("pk_hasStudied")) {
            textArea.setText("This student already has a grade on this course. \nOnly one grade per course");
        }else if(exceptionMessage.contains("pk_studies")){
            textArea.setText("This student is already studying this course");
        }else if(errorCode == 2628 && exceptionMessage.contains("studentName")) {
            textArea.setText("A students name is limited to 200 characters");
        }else if((errorCode == 2628) && exceptionMessage.contains("studentSSN")){
            textArea.setText("A students social security number is limited to 12 characters");
        }else if(errorCode == 2628 && exceptionMessage.contains("studentCity")){
            textArea.setText("A students city is limited to 200 characters");
        }else if(errorCode == 2628 && exceptionMessage.contains("courseName")){
            textArea.setText("Course name is limited to 200 characters");
        }else if(errorCode == 2628 && exceptionMessage.contains("grade")){
            textArea.setText("Grade is limited to be one character between A-F \n" +
                    "Make sure a grade has been chosen!");
        }else if(errorCode == 0) {
            textArea.setText("Could not connect to database server. \nPlease contact support!");
        }else{
            exception.printStackTrace();
            textArea.setText("Ooops, something went wrong. \nPlease contact system administrator");
        }
        textArea.isVisible();
    }

}
