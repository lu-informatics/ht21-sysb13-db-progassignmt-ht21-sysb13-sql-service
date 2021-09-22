package com.sqlservice;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.Callback;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

public class AppFunctions {

    public static ObservableList<ObservableList> fillList(ResultSet resultSet) throws SQLException {

        ObservableList<ObservableList> dataSet = FXCollections.observableArrayList();

        while (resultSet.next()) { //itererar över resultSet. För första raden skapar vi en observable list (kallar för row).
            ObservableList<String> row = FXCollections.observableArrayList(); //listan som är raderna.
            //för varje kolumn i den raden, lägger vi till värdet i den observablelist.
            //Den lägger till alla värden från resultSet till observableList.
            for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                row.add(resultSet.getString(i));
            }
            System.out.println("Lade till rad: " + row);
            dataSet.add(row); //lägger till row(är en observableList) i dataList.
        }
        return dataSet;

    }

    public static void setTableColumnNames(TableView tableView, ResultSet resultSet) throws SQLException{

        /**
         * Inuti den observerbara listan, har vi en lista av observerbara listor, den tar in tableName (tex Course) och tableView
         * som är vilket tableView vi ska fylla(länkat till FXML-dokumentet). Vi har en metod som anropar denna metoden i denna klassen.
         * skapar en observableList och för att kunna få in listan i ett tableview i JavaFX.
         * Vill man istället ha ett resultSet där man joinar två olika tables?
         * getAllfromTableName behövs även ändras då. **/


        for(int i=0; i<resultSet.getMetaData().getColumnCount(); i++) {
            /**Den lägger till alla kolumner från resultset.
             * courseCode, credits, name = den itererar över de. Så länge i < så många kolumner som finns i resultset, så kör den
             * loopen. Första gången vi kör är i = 0.
             * sätter att j = i **/
            final int j = i;

            TableColumn col = new TableColumn(resultSet.getMetaData().getColumnName(i + 1));

            /**Nytt table column. För varje kolumn skapar vi en ny kolumn som har namnet av den kolumnamnet på index i + 1.
             * första kolumnen är på index 1 i SQL och inte 0 som i en Array. **/

            col.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ObservableList, String>, ObservableValue<String>>() { //sätter vilket värde det ska vara i den kolumnen.
                @Override
                public ObservableValue call(TableColumn.CellDataFeatures<ObservableList, String> param) {
                    return new SimpleStringProperty(param.getValue().get(j).toString());
                }
            });

            tableView.getColumns().addAll(col);        //lägger till kolumner i tableView som vi har på rad 81. (kallar på metoden)
            System.out.println("Column ["+i+"] name: " + resultSet.getMetaData().getColumnName(i+1)); //printar ut.
        }
        /**
         * ObservableValue och ObservableList etc är wrappers vilka låter observera värdet/tablet och kunna göra ändringar till det med tex listeners!
         * Methoden ovan sätter lägger till columns i ett TableView och döper dessa till deras namn enlig resultsetet!**/

    }

    public static String getValueOfCell(TableView tableView, int columnIndex){
        ObservableList<ObservableList> row  = tableView.getSelectionModel().getSelectedItems(); //Hämtar raden vi vill få columnen från!
        ObservableList<ObservableList> object = row.get(columnIndex);
        Object ob = object.get(columnIndex); //hämtar objekt(ID, name) på index i listan.
        String id = ob.toString(); //gör objektet till en sträng för att skicka till databasen.
        System.out.println(id);

        return id;
    }

    public static void updateSearchableTableView(TableView tableView, TextField textField, ResultSet resultSet) throws SQLException { //tar in tableView,
        //textField, String som är namnet på table som vi vill fylla. När vi kallar på denna metoden kan vi säga vilket
        //table vi vill kolla på.

        //ob-list som vi fyller med det som
        //metoden fillTableViewByName returnerar.
        tableView.getColumns().clear();
        AppFunctions.setTableColumnNames(tableView, resultSet); //sätter kolumnNamn efter resultSetets kolumnNamn


        //ObservableList<ObservableList> dataList = AppFunktioner.fillList(resultSet);
        FilteredList<ObservableList> filteredData = new FilteredList<>(AppFunctions.fillList(resultSet), b -> true); //Wrappar dataList i en FilteredList.
        //b -> true gör att den kan lyssna när vi skriver i sökfältet.

        textField.textProperty().addListener((observable, oldvalue, newValue) ->{ //lägger till en listener som lyssnar efter när man skriver in något i searchfieldet
            //oldValue ändras aldrig, men det gör newValue.
            filteredData.setPredicate( row -> {
                if(newValue == null || newValue.isEmpty()){ //ifall inget är skrivet i sökfältet visas hela resultsetet!
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase(Locale.ROOT); //gör att vi allt är lowercase
                if(row.toString().toLowerCase().contains(lowerCaseFilter)){ //Ifall någon entitet i resultsetet överensstämmer med söksträngen returneras den/dessa!
                    return true;
                }
                else return false; //fail safe.

            });
        });

        tableView.setItems(filteredData);
        ContosoConnection.connectionClose(resultSet);
    }

}
