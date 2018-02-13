package gr.uoa.di;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;

import static gr.uoa.di.Client.*;

public class ClientInterface extends Application {

    private Label homeLabel;
    private static String IP, portNum, fileName, filepath, fileKey, descriptiveString, encodedPK, digitalSignature;

    /* Initialize the observable list that is responsible for the table with the uploaded files */
    private ObservableList<TableObj> fileInfo = FXCollections.observableArrayList();

    private void login(Stage primaryStage) {
        Label label;
        Stage stage;
        Button joinBtn;
        Scene loginScene;
        GridPane loginPane;
        TextField ipField, portField;

        stage = new Stage();
        loginPane = new GridPane();
        ipField = new TextField();
        portField = new TextField();
        joinBtn = new Button("Log in");
        loginScene = new Scene(loginPane, 400, 250);
        label = new Label("Please, insert the IP and port number \nto connect to your node");

        IP = null;
        portNum = null;
        loginPane.requestFocus();
        /* Set field properties */
        ipField.setMaxWidth(200);
        portField.setMaxWidth(200);
        ipField.setPromptText("Insert IP");
        portField.setPromptText("Insert port number");
        /* Set pane properties */
        loginPane.setHgap(10);
        loginPane.setVgap(10);
        loginPane.setStyle("-fx-background-color:thistle;-fx-padding:10px;");
        /* Set label properties */
        label.setTextFill(Color.BLUEVIOLET.darker());
        label.setFont(Font.font("Verdana", 14));

        /* Set button properties */
        joinBtn.setDisable(true);
        joinBtn.setOnAction(event-> {

            stage.close();
            try {
                connectToNode(IP, Integer.parseInt(portNum));
                getPreviousFileInfo();
                homeLabel.setText("You can upload your files in a secure and anonymous way." +
                        "\n\n\nYour IP is " + Client.getNodeIP() + " and your port number is "+Client.getNodePortNum());
            } catch (IOException | NoSuchPaddingException |
                    NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
                e.printStackTrace();
            }
            primaryStage.show();
        });

        /* listener of IP textfield to retrieve IP */
        ipField.textProperty().addListener((observable, oldValue, newValue) ->
                txtListener(newValue, 'i', joinBtn, loginPane));

        /* listener of port textfield to retrieve port number */
        portField.textProperty().addListener((observable, oldValue, newValue) ->
                txtListener(newValue, 'p', joinBtn, loginPane));

        /* Add everything to pane*/
        loginPane.add(label, 1, 0);
        loginPane.add(ipField, 1, 4);
        loginPane.add(portField, 1, 5);
        loginPane.add(joinBtn, 2, 8);
        loginPane.setAlignment(Pos.CENTER);

        //make scene from pane
        stage.setTitle("Login to System");
        stage.setScene(loginScene);
        stage.setMinWidth(600);
        stage.show();
    }


    private TabPane setTabPane(Stage primaryStage) {
        TabPane tabPane = new TabPane();
        Tab uploadFile, downloadFile, addFriendNode, homeTab;

        uploadFile = setUploadTab();
        downloadFile = setDownloadTab();
        addFriendNode = setAddFriendTab();
        homeTab = setHomeTab(primaryStage);

        //first tab - upload file tab
        homeTab.setText("Home");
        homeTab.closableProperty().set(false);
        //second tab - upload file tab
        uploadFile.setText("Upload File");
        uploadFile.closableProperty().set(false);
        //third tab - download file tab
        downloadFile.setText("Download File");
        downloadFile.closableProperty().set(false);
        //fourth tab - add new trusting node to your friends tab
        addFriendNode.setText("Add Friend");
        addFriendNode.closableProperty().set(false);

        tabPane.setPrefWidth(900);
        tabPane.setTabMinWidth(138);
        tabPane.setTabMaxWidth(400);
        tabPane.getTabs().addAll(homeTab, uploadFile, downloadFile, addFriendNode);

        return  tabPane;
    }

    private Tab setHomeTab(Stage stage){
        Tab tab;
        GridPane pane;
        Label welcomeLbl;
        Button shutdownBtn;

        tab = new Tab();
        pane = new GridPane();
        homeLabel = new Label();
        welcomeLbl = new Label("Welcome!");
        shutdownBtn = new Button("Shut down node");

        /* Set properties */
        pane.setVgap(10);
        pane.setHgap(10);
        pane.setAlignment(Pos.CENTER);
        GridPane.setHalignment(homeLabel, HPos.CENTER);
        GridPane.setHalignment(welcomeLbl, HPos.CENTER);
        GridPane.setHalignment(shutdownBtn, HPos.RIGHT);
        GridPane.setValignment(shutdownBtn, VPos.BOTTOM);
        homeLabel.setTextFill(Color.BLUEVIOLET.darker());
        welcomeLbl.setTextFill(Color.BLUEVIOLET.darker());
        homeLabel.setFont(Font.font("Verdana", 14));
        welcomeLbl.setFont(Font.font("Verdana", 16));
        pane.setStyle("-fx-background-color:thistle;-fx-padding:10px;");

        /* Shut down button handler */
        shutdownBtn.setOnAction(event-> {
            try {
                stage.close();
                Client.nodeShutdown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        /* Add items into pane */
        pane.add(welcomeLbl, 1, 1);
        pane.add(homeLabel, 1, 3);
        pane.add(shutdownBtn, 3, 20);
        /* Add items into tab */
        tab.setContent(pane);

        return tab;
    }

    private void getPreviousFileInfo(){
        /* Retrieve information about all uploaded files yet */
        HashMap<String, String> upFiles = null;
        try {
            upFiles = getAllFileInfo();
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert upFiles != null;
        for(Map.Entry entry : upFiles.entrySet()){
            fileInfo.add(new TableObj((String)entry.getValue(), (String)entry.getKey()));
        }
    }

    private Tab setUploadTab(){
        Tab tab;
        Stage stage;
        GridPane pane;
        TableView table;
        TableColumn filekeyCol;
        TableColumn filenameCol;
        TextField fileTF, descrStrTF;
        Label uploadLbl, uploadedFiles, descrStrLbl;
        Button uploadBtn, fileChooserBtn, credentialsBtn;

        tab = new Tab();
        stage = new Stage();
        pane = new GridPane();
        fileTF = new TextField();
        table = new TableView<>();
        descrStrTF = new TextField();
        uploadBtn = new Button("Upload");
        filekeyCol = new TableColumn("File Key");
        fileChooserBtn = new Button("Choose File");
        filenameCol = new TableColumn("File Name");
        credentialsBtn = new Button("Get Credentials");
        uploadedFiles = new Label("Your Uploaded Files: \n");
        uploadLbl = new Label("Please, choose the file you wish to upload");
        descrStrLbl = new Label("Please, insert a short description for your file");


        /* Set properties */
        fileName = null;
        filepath = null;
        descriptiveString = null;
        pane.setVgap(10);
        pane.setHgap(10);
        table.setMinWidth(450);
        table.setMaxHeight(200);
        fileTF.setDisable(true);
        table.setEditable(false);
        uploadBtn.setDisable(true);
        filekeyCol.setMinWidth(250);
        filenameCol.setMinWidth(200);
        fileTF.setPromptText("Choose File");
        descrStrTF.setPromptText("Insert descriptive string");
        GridPane.setHalignment(uploadBtn, HPos.LEFT);
        GridPane.setValignment(credentialsBtn, VPos.BOTTOM);
        uploadLbl.setTextFill(Color.BLUEVIOLET.darker());
        descrStrLbl.setTextFill(Color.BLUEVIOLET.darker());
        uploadedFiles.setTextFill(Color.BLUEVIOLET.darker());
        uploadLbl.setFont(Font.font("Verdana", 14));
        descrStrLbl.setFont(Font.font("Verdana", 14));
        uploadedFiles.setFont(Font.font("Verdana", 12));
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);


        /* Assign the kind of information that each column stores */
        filenameCol.setCellValueFactory(
                new PropertyValueFactory<TableObj, String>("fileName")
        );
        filekeyCol.setCellValueFactory(
                new PropertyValueFactory<TableObj, String>("fileKey")
        );

        table.setItems(fileInfo);
        /* Add table columns to table view */
        table.getColumns().addAll(filenameCol, filekeyCol);



        /* Table handler to manage selected rows from table */
        table.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (table.getSelectionModel().getSelectedItem() != null) {
                    TableObj selectedObj = (TableObj) table.getSelectionModel().getSelectedItem();
                    /* Button handler */
                    credentialsBtn.setOnAction(event2 -> {
                        getCredentials(selectedObj.getFileKey());
                    });
                }
            }
        });

        /* Text field listener to get input text */
        descrStrTF.textProperty().addListener((observable, oldValue, newValue) -> {

            if(newValue != null && !newValue.isEmpty()){
                descriptiveString = newValue;
                if(filepath != null) {
                    uploadBtn.setDisable(false);
                }
            }
            else{
                uploadBtn.setDisable(true);
            }

        });

        /* Upload button handler */
        uploadBtn.setOnAction(event-> {
            try {
                fileKey = uploadFile(filepath, fileName, descriptiveString);
            } catch (IOException | NoSuchPaddingException |
                        NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
                    e.printStackTrace();
            }
            TableObj entry = new TableObj(fileName, fileKey);
            fileInfo.add(entry);
            fileKey = null;
            filepath = null;
            fileName = null;
            descriptiveString = null;
            fileTF.clear();
            descrStrTF.clear();
        });

        /* file chooser button handler */
        fileChooserBtn.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            File selectedFile = fileChooser.showOpenDialog(stage);
            if(selectedFile != null) {
                fileName = selectedFile.getName();
                filepath = selectedFile.getAbsolutePath();
                fileTF.setText(selectedFile.getName());
                if(descriptiveString != null){
                    uploadBtn.setDisable(false);
                }
            }
        });


        /* Add items to the pane grid of this tab */
        pane.add(uploadLbl,1,2);
        pane.add(fileTF, 1, 3);
        pane.add(fileChooserBtn, 3 , 3);
        pane.add(descrStrLbl,1,4);
        pane.add(descrStrTF,1, 5);
        pane.add(uploadBtn, 3, 5);
        pane.add(uploadedFiles, 1, 7);
        pane.add(table, 1, 8);
        pane.add(credentialsBtn, 3, 8);
        /* Add items into tab */
        tab.setContent(pane);

        return tab;
    }

    private Tab setDownloadTab(){
        Tab tab;
        GridPane pane;
        TextField textField;
        Label downloadLbl;
        Button downloadBtn, showDownloadsBtn;

        tab = new Tab();
        pane = new GridPane();
        textField = new TextField();
        downloadBtn = new Button("Download");
        showDownloadsBtn = new Button("Show Downloads");
        downloadLbl = new Label("Please, insert the key of the file you wish to download");

        /* Set properties */
        fileKey = null;
        pane.setVgap(10);
        pane.setHgap(10);
        pane.setAlignment(Pos.CENTER);
        downloadBtn.setDisable(true);
        textField.setPromptText("Insert file key");
        GridPane.setHalignment(downloadBtn, HPos.RIGHT);
        GridPane.setHalignment(showDownloadsBtn, HPos.CENTER);
        GridPane.setValignment(showDownloadsBtn, VPos.BOTTOM);
        downloadLbl.setTextFill(Color.BLUEVIOLET.darker());
        downloadLbl.setFont(Font.font("Verdana", 14));

        /* Text field listener to get input text */
        textField.textProperty().addListener((observable, oldValue, newValue) ->
                fileKey = downloadListener(newValue, downloadBtn, pane));

        /* Button handlers */
        downloadBtn.setOnAction(event-> {
            if(fileKey != null){
                try {
                    String [] fileCredentials = getFileInfo(fileKey);
                    if (fileCredentials == null) {
                        fileCredentialsWindow(textField);
                    }
                    else{
                        /* assign text field value to a temporary string in order to clear the field
                        * without waiting for the command to complete */
                        String tmpFileKey;
                        fileName = fileCredentials[0];
                        descriptiveString = fileCredentials[1];
                        encodedPK = fileCredentials[2];
                        digitalSignature = fileCredentials[3];

                        tmpFileKey = textField.getText();
                        textField.clear();
                        downloadFile(tmpFileKey, fileName, descriptiveString, encodedPK, digitalSignature);
                    }
                }
                catch (IOException | NoSuchPaddingException | InvalidKeySpecException |
                        NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
                    e.printStackTrace();
                }
            }

        });

        showDownloadsBtn.setOnAction(event-> {
            String path, command;

            path = System.getProperty("user.dir") + "/Downloads/" + Client.getNodeSubDirName() + "/";
            if (System.getProperty("os.name").contains("Mac")) {
                command = "open " + path;
            }
            else {
                command = "xdg-open " + path;
            }
            try {
                Runtime.getRuntime().exec(command);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        /* Add items to the pane grid of this tab */
        pane.add(downloadLbl,1,0);
        pane.add(textField, 1, 2);
        pane.add(downloadBtn, 2, 2);
        pane.add(showDownloadsBtn,1, 5);
        /* Add items into tab */
        tab.setContent(pane);

        return tab;
    }

    // Listener for text fields in download/upload tabs in order to retrieve file names and file keys, respectively
    private String downloadListener(String newValue, Button btn, GridPane pane){
        if(newValue != null && !newValue.isEmpty()){
            btn.setDisable(false);
            return  newValue;
        }
        else{
            pane.requestFocus();
            btn.setDisable(true);
            return  null;
        }
    }

    private void fileCredentialsWindow(TextField txtField){
        Stage stage;
        Scene scene;
        GridPane pane;
        Button submitBtn;
        Label mainLbl, filenameLbl, descLbl, signLbl, encodedPKlbl;
        TextField fileNameTF, descStringTF, digSignatureTF, encodedPKTF;

        stage = new Stage();
        pane = new GridPane();
        fileNameTF = new TextField();
        encodedPKTF = new TextField();
        descStringTF = new TextField();
        digSignatureTF = new TextField();
        submitBtn = new Button("Submit");
        scene = new Scene(pane, 400, 400);
        filenameLbl = new Label("Insert the name of the file");
        descLbl = new Label("Insert the descriptive string of the file");
        signLbl = new Label("Insert the digital signature of the file");
        encodedPKlbl = new Label("Insert the encoded public key of the file");
        mainLbl = new Label("The requested file does not belong to you. " +
                "\nThe following credentials are required \n" +
                "if you want to download it.");

        fileName = null;
        encodedPK = null;
        digitalSignature = null;
        descriptiveString = null;
        pane.requestFocus();

        /* Set properties */
        pane.setHgap(10);
        pane.setVgap(10);
        submitBtn.setDisable(true);
        fileNameTF.setMaxWidth(300);
        encodedPKTF.setMaxWidth(300);
        descStringTF.setMaxWidth(300);
        digSignatureTF.setMaxWidth(300);
        fileNameTF.setPromptText("Insert file name");
        encodedPKTF.setPromptText("Insert public key");
        descStringTF.setPromptText("Insert descriptive string");
        digSignatureTF.setPromptText("Insert digital signature");
        descLbl.setTextFill(Color.BLUEVIOLET.darker());
        signLbl.setTextFill(Color.BLUEVIOLET.darker());
        mainLbl.setTextFill(Color.BLUEVIOLET.darker());
        filenameLbl.setTextFill(Color.BLUEVIOLET.darker());
        encodedPKlbl.setTextFill(Color.BLUEVIOLET.darker());
        mainLbl.setFont(Font.font("Verdana", 14));
        signLbl.setFont(Font.font("Verdana", 12));
        descLbl.setFont(Font.font("Verdana", 12));
        filenameLbl.setFont(Font.font("Verdana", 12));
        encodedPKlbl.setFont(Font.font("Verdana", 12));

        pane.setStyle("-fx-background-color:thistle;-fx-padding:10px;");

        /* listener of file name textfield */
        fileNameTF.textProperty().addListener((observable, oldValue, newValue) ->
                credentialsListener(newValue, 'n', submitBtn, pane));

        /* listener of descriptive string textfield to retrieve descriptive string */
        descStringTF.textProperty().addListener((observable, oldValue, newValue) ->
                credentialsListener(newValue, 'd', submitBtn, pane));

        /* listener of digital signature string textfield to retrieve digital signature */
        digSignatureTF.textProperty().addListener((observable, oldValue, newValue) ->
                credentialsListener(newValue, 's', submitBtn, pane) );

        /* listener of encoded public key textfield to retrieve public key of file */
        encodedPKTF.textProperty().addListener((observable, oldValue, newValue) ->
                credentialsListener(newValue, 'p', submitBtn, pane) );

        /* Set button handler */
        submitBtn.setOnAction(e-> {
            /* assign text field value to a temporary string in order to clear the field
               without waiting for the command to complete */
            String tmpFileKey;

            tmpFileKey = txtField.getText();
            stage.close();
            txtField.clear();
            try {
                downloadFile(tmpFileKey, fileName, descriptiveString, encodedPK, digitalSignature);
            } catch (IOException | NoSuchPaddingException | InvalidKeySpecException |
                NoSuchAlgorithmException | InvalidKeyException | SignatureException e1) {
            e1.printStackTrace();
            }

        });

        /* Add everything to pane*/
        pane.add(mainLbl, 0, 0);
        pane.add(filenameLbl, 0, 3);
        pane.add(fileNameTF, 0, 4);
        pane.add(descLbl,0, 6);
        pane.add(descStringTF, 0, 7);
        pane.add(signLbl, 0, 9);
        pane.add(digSignatureTF, 0, 10);
        pane.add(encodedPKlbl, 0, 12);
        pane.add(encodedPKTF, 0, 13);
        pane.add(submitBtn, 1, 20);

        //make scene from pane
        stage.setTitle("File Credentials");
        stage.setScene(scene);
        stage.setMinWidth(300);
        stage.setMinHeight(500);
        stage.show();
    }

    private void getCredentials(String key){
        Stage stage;
        Scene scene;
        GridPane pane;
        Label mainLbl;
        Clipboard clipboard;
        Button closeBtn, copyBtn;
        ClipboardContent content;
        TableView<TableObj> table;
        String [] fileCredentials;
        TableColumn credentials, properties;

        stage = new Stage();
        pane = new GridPane();
        table = new TableView<>();
        properties = new TableColumn();
        credentials = new TableColumn();
        content = new ClipboardContent();
        closeBtn = new Button("Close");
        clipboard = Clipboard.getSystemClipboard();
        copyBtn = new Button("Copy to Clipboard");
        scene = new Scene(pane, 400, 400);
        mainLbl = new Label("The credentials of the requested file are the following.");

        ObservableList<TableObj> data = FXCollections.observableArrayList();
        table.setItems(data);
        /* Add table columns to table view */
        table.getColumns().addAll(properties, credentials);
        try {
            fileCredentials = getFileInfo(key);
            data.add(new TableObj("File key: ", key));
            data.add(new TableObj("File name: ", fileCredentials[0]));
            data.add(new TableObj("Descriptive String: ", fileCredentials[1]));
            data.add(new TableObj("Digital Signature: ", fileCredentials[3]));
            data.add(new TableObj("Encoded public key: ", fileCredentials[2]));

        } catch (IOException e) {
            e.printStackTrace();
        }

        /* Set properties */
        pane.setHgap(10);
        pane.setVgap(10);
        table.setMaxHeight(200);
        pane.setAlignment(Pos.CENTER);
        pane.setHalignment(copyBtn, HPos.RIGHT);
        pane.setHalignment(closeBtn, HPos.RIGHT);
        mainLbl.setTextFill(Color.BLUEVIOLET.darker());
        mainLbl.setFont(Font.font("Verdana", 14));
        pane.setStyle("-fx-background-color:thistle;-fx-padding:10px;");
        pane.setValignment(copyBtn, VPos.BOTTOM);

        /* Assign the kind of information that each column stores */
        properties.setCellValueFactory(
                new PropertyValueFactory<TableObj, String>("fileName")
        );

        credentials.setCellValueFactory(
                new PropertyValueFactory<TableObj, String>("fileKey")
        );

        /* Set button handler */
        closeBtn.setOnAction(e-> {
            stage.close();
        });


        /* Table handler to manage selected rows from table */
        table.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (table.getSelectionModel().getSelectedItem() != null) {
                    TableObj selectedObj = (TableObj) table.getSelectionModel().getSelectedItem();
                    /* Set button handler */
                    copyBtn.setOnAction(event2 -> {
                        content.putString(selectedObj.getFileKey());
                        clipboard.setContent(content);
                    });                }
            }
        });

        /* Add everything to pane*/
        pane.add(mainLbl, 0, 0);
        pane.add(table, 0, 4);
        pane.add(copyBtn,0, 5);
        pane.add(closeBtn, 0, 10);

        //make scene from pane
        stage.setTitle("File Credentials");
        stage.setScene(scene);
        stage.setMinWidth(550);
        stage.setMaxWidth(550);
        stage.setMinHeight(500);
        stage.setMaxHeight(500);
        stage.show();
    }

    private void credentialsListener(String newValue, char field, Button btn, GridPane pane){

        if(field == 'n'){
            if(newValue != null && !newValue.isEmpty()){
                fileName = newValue;
            }
            else{
                pane.requestFocus();
                fileName = null;
            }
         }
         else if(field == 'd'){
            if(newValue != null && !newValue.isEmpty()){
                descriptiveString = newValue;
            }
            else{
                pane.requestFocus();
                descriptiveString = null;
            }
         }
         else if(field == 's'){
            if(newValue != null && !newValue.isEmpty()){
                digitalSignature = newValue;
            }
            else{
                pane.requestFocus();
                digitalSignature = null;
            }
         }
        else if(field == 'p'){
            if(newValue != null && !newValue.isEmpty()){
                encodedPK = newValue;
            }
            else{
                pane.requestFocus();
                encodedPK = null;
            }
        }
        if(fileName != null && descriptiveString != null && digitalSignature != null && encodedPK != null){
            btn.setDisable(false);
        }
        else {
            btn.setDisable(true);
        }
    }


    private Tab setAddFriendTab(){
        Tab addNeighbor;
        Label label;
        Button addBtn;
        GridPane pane;
        TextField ipField, portField;

        pane = new GridPane();
        addNeighbor = new Tab();
        ipField = new TextField();
        portField = new TextField();
        addBtn = new Button("Add Friend");
        label = new Label("Please, insert the IP and port number of \n" +
                "a trusting node to establish a connection");

        /* Set properties */
        pane.setVgap(10);
        pane.setHgap(10);
        addBtn.setDisable(true);
        ipField.setMaxWidth(200);
        portField.setMaxWidth(200);
        ipField.setPromptText("Insert IP");
        portField.setPromptText("Insert port number");
        label.setTextFill(Color.BLUEVIOLET.darker());
        label.setFont(Font.font("Verdana", 14));

        /* listener of IP textfield to retrieve IP */
        ipField.textProperty().addListener((observable, oldValue, newValue) ->
                txtListener(newValue, 'i', addBtn, pane));

        /* listener of port number textfield to retrieve port number */
        portField.textProperty().addListener((observable, oldValue, newValue) ->
                txtListener(newValue, 'p', addBtn, pane));

        addBtn.setOnAction(event-> {
            addBtn.setDisable(true);

            try {
                addNeighbor(IP, Integer.parseInt(portNum));
            } catch (IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            ipField.clear();
            portField.clear();
        });


        /* Add items to the pane of this tab */
        pane.add(label, 0, 2);
        pane.add(ipField, 0, 6);
        pane.add(portField,0, 7);
        pane.add(addBtn, 0, 10);
        pane.setAlignment(Pos.CENTER);

        /* Add pane to tab */
        addNeighbor.setContent(pane);

        return addNeighbor;
    }

    private void txtListener(String newValue, char field, Button btn, GridPane pane){

        if(field == 'i'){
            if(newValue != null && !newValue.isEmpty()){
                IP = newValue;
            }
            else{
                pane.requestFocus();
                IP = null;
            }
        }
        else {
            if(newValue != null && !newValue.isEmpty()){
                portNum = newValue;
            }
            else{
                pane.requestFocus();
                portNum = null;
            }
        }
        if (IP != null && portNum != null) {
            btn.setDisable(false);
        }
        else {
            btn.setDisable(true);
        }
    }


    private void mainUI(Stage primaryStage){
        Scene mainScene;
        TabPane tabPane;
        GridPane mainPane;

        mainPane = new GridPane();
        /* Create the main interface of interaction with th client */
        tabPane = setTabPane(primaryStage);
        /*set background color of Pane*/
        mainPane.setStyle("-fx-background-color:thistle;-fx-padding:10px;");
        mainPane.setHgap(10);
        mainPane.setVgap(10);
        /*add everything to pane*/
        mainPane.add(tabPane, 0, 0);
        mainPane.setAlignment(Pos.TOP_CENTER);

        //make scene from pane
        mainScene = new Scene(mainPane, 650, 500);
        primaryStage.setTitle("Client Interface");
        primaryStage.setScene(mainScene);
        primaryStage.setMinWidth(650);
        primaryStage.setMaxWidth(650);
        primaryStage.setMinHeight(500);
        primaryStage.setMaxHeight(500);



    }

    @Override
    public void start(Stage primaryStage) {

        login(primaryStage);
        mainUI(primaryStage);
//        primaryStage.show();
    }


    public static void main(String[] args) {
        launch();
    }
}

