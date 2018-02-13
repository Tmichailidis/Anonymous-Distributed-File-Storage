package gr.uoa.di;
import javafx.beans.property.SimpleStringProperty;

public class TableObj {

    private SimpleStringProperty filename, filekey;

    public TableObj(String fName, String fKey){
        this.filename = new SimpleStringProperty(fName);
        this.filekey =  new SimpleStringProperty(fKey);
    }

    public String getFileName() {
        return filename.get();
    }

    public String getFileKey() {
        return filekey.get();
    }

    public void setFileName(String fName) {
        filename.set(fName);
    }

    public void setFileKey(String fKey) {
        filekey.set(fKey);
    }

}
