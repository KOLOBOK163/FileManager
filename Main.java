import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Comparator;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;


import java.util.logging.Logger;

public class Main extends Application {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        launch(args);
    }

    private boolean isDarkMode = false;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("File Manager");

        // Создаем корневой контейнер
        BorderPane root = new BorderPane();
        root.setPadding(new javafx.geometry.Insets(10));

        // Создаем сцену и применяем CSS
        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        // Верхняя панель: заголовок
        Label titleLabel = new Label("File Manager");
        titleLabel.setId("titleLabel");
        BorderPane.setAlignment(titleLabel, Pos.TOP_LEFT);
        //Кнопка "Switch Theme"
        Button switchThemeButton = createStyledButton("Switch Theme");
        switchThemeButton.setOnAction(e -> {
            if(isDarkMode){
                scene.getStylesheets().clear();
                scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            }
            else{
                scene.getStylesheets().clear();
                scene.getStylesheets().add(getClass().getResource("/dark.css").toExternalForm());
            }

            isDarkMode = !isDarkMode;
        });

        // Верхняя панель с двумя элементами
        HBox leftBox = new HBox(titleLabel);
        leftBox.setAlignment(Pos.CENTER_LEFT);
        HBox rightBox = new HBox(switchThemeButton);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        BorderPane topPane = new BorderPane();
        topPane.setLeft(leftBox);
        topPane.setRight(rightBox);

        root.setTop(topPane);

        // Центральная панель: поля ввода
        VBox centerPane = new VBox(10);
        centerPane.setPadding(new Insets(10));

        // Поле для ввода имени файла
        Label fileNameLabel = new Label("File Name:");
        TextField fileNameField = new TextField();
        fileNameField.setPromptText("Enter file name...");

        // Поле для ввода содержимого файла
        Label fileContentLabel = new Label("File Content:");
        TextArea fileContentArea = new TextArea();
        fileContentArea.setPromptText("Enter file content...");
        fileContentArea.setPrefRowCount(5);


        VBox centerPath = new VBox(10, fileContentLabel, fileContentArea);
        VBox.setVgrow(fileContentArea, Priority.ALWAYS);
        root.setCenter(centerPath);

        // Выпадающий список для выбора типа сортировки
        Label sortTypeLabel = new Label("Sort By:");
        ComboBox<String> sortTypeComboBox = new ComboBox<>(FXCollections.observableArrayList(
                "Size (Ascending)", "Size (Descending)", "Name (Ascending)", "Name (Descending)", "Creation Date (Newest)", "Creation Date (Oldest)"
        ));
        sortTypeComboBox.setValue("Size (Ascending)"); // Значение по умолчанию
        centerPane.getChildren().addAll(fileNameLabel, fileNameField, fileContentLabel, fileContentArea, sortTypeLabel, sortTypeComboBox);
        root.setCenter(centerPane);

        // Нижняя панель: кнопки
        HBox buttonPane = new HBox(10);
        buttonPane.setAlignment(Pos.CENTER);

        // Кнопка "Create File"
        Button createButton = createStyledButton("Create File");
        createButton.setOnAction(e -> {
            String fileName = fileNameField.getText();
            String content = fileContentArea.getText();
            createFile(fileName, content);
            fileContentArea.clear();
        });

        // Кнопка "Read File"
        Button readButton = createStyledButton("Read File");
        readButton.setOnAction(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Выберите файл");
            int result = fileChooser.showOpenDialog(null);

            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                String fileName = selectedFile.getAbsolutePath();
                String content = readFile(fileName); // Чтение файла
                fileContentArea.setText(content); // Отображение содержимого в TextArea
            }
        });

        // Кнопка "Update File"
        Button updateButton = createStyledButton("Update File");
        updateButton.setOnAction(e -> {
            String fileName = fileNameField.getText();
            String content = fileContentArea.getText();
            updateFile(fileName, content);
            fileContentArea.clear();
        });

        // Кнопка "Delete File"
        Button deleteButton = createStyledButton("Delete File");
        deleteButton.setOnAction(e -> deleteFile());

        // Кнопка "Sort Files"
        Button sortButton = createStyledButton("Sort Files");
        sortButton.setOnAction(e -> {
            String sortType = sortTypeComboBox.getValue();
            sortFiles(sortType, fileContentArea);
        });

        // Кнопка "Clear"
        Button clearButton = createStyledButton("Clear");
        clearButton.setOnAction(e ->{
            fileNameField.clear();
            fileContentArea.clear();
        });

        // Кнопка "Exit"
        Button exitButton = createStyledButton("Exit");
        exitButton.setOnAction(e -> primaryStage.close());

        buttonPane.getChildren().addAll(createButton, readButton, updateButton, deleteButton, sortButton, clearButton, exitButton);
        root.setBottom(buttonPane);


        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Метод для создания стилизованной кнопки
    private Button createStyledButton(String text) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px;");
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;"));
        return button;
    }

    // Метод для создания файла
    private void createFile(String fileName, String content) {
        try {
            File file = new File(fileName);
            if (file.createNewFile()) {
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(content);
                    System.out.println("File created: " + file.getName());
                }
            } else {
                System.out.println("File already exists.");
            }
        } catch (IOException e) {
            System.out.println("Error creating file: " + e.getMessage());
        }
    }

    // Метод для чтения файла
    private String readFile(String fileName) {
        if (fileName == null) {
            return "File name is null.";
        }
        try {
            if (fileName.endsWith(".docx")) {
                return readDocxFile(fileName);
            } else if (fileName.endsWith(".xlsx")) {
                return readExcelFile(fileName);
            } else if(fileName.endsWith(".pdf")){
                return readPDFFile(fileName);
            } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || 
                       fileName.endsWith(".png") || fileName.endsWith(".bmp") || 
                       fileName.endsWith(".tiff")) {
                return readImageFile(fileName);
            } else {
                return readTextFile(fileName);
            }
        } catch (IOException e) {
            logger.severe("Error reading file: " + e.getMessage());
            return "File not found or could not be read.";
        }
    }

    // Метод для чтения .docx файла
    private String readDocxFile(String fileName) throws IOException {
        try (FileInputStream fis = new FileInputStream(fileName);
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    // Метод для чтения .xlsx файла
    private String readExcelFile(String fileName) throws IOException {
        StringBuilder content = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(fileName);
             Workbook workbook = new XSSFWorkbook(fis)) {
            for (Sheet sheet : workbook) {
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        content.append(cell.toString()).append("\t");
                    }
                    content.append("\n");
                }
            }
        }
        return content.toString();
    }

    private String readPDFFile(String fileName) throws IOException {
        try (PDDocument document = Loader.loadPDF(new File(fileName))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String readImageFile(String fileName) throws IOException {
        ITesseract tesseract = new Tesseract();
        try {
            String tessdataPath = "G:\\Tesseract-OCR\\tessdata";
            System.out.println("Using tessdata path: " + tessdataPath);
            tesseract.setDatapath(tessdataPath);
            
            File imageFile = new File(fileName);
            System.out.println("Reading image file: " + imageFile.getAbsolutePath());
            
            String text = tesseract.doOCR(imageFile);
            System.out.println("Recognized text length: " + (text != null ? text.length() : "null"));
            
            return text != null ? text : "No text recognized";
        } catch (TesseractException e) {
            System.err.println("Tesseract error: " + e.getMessage());
            throw new IOException("Ошибка при распознавании текста: " + e.getMessage());
        }
    }

    // Метод для чтения текстового файла
    private String readTextFile(String fileName) throws IOException {
        byte[] fileBytes = Files.readAllBytes(Paths.get(fileName));
        String content = new String(fileBytes, StandardCharsets.UTF_8); // Чтение в UTF-8

        // Если текст содержит "мусор" (некорректные символы), пробуем перекодировать
        if (containsGarbage(content)) {
            content = new String(fileBytes, Charset.forName("Windows-1251")); // Пример для Windows-1251
        }
        return content;
    }



    // Метод для проверки на "мусор" в тексте
    private boolean containsGarbage(String text) {
        // Проверка на непечатаемые символы и символы вне ASCII
        return text.chars().anyMatch(c -> (c < 32 && c != '\n' && c != '\r' && c != '\t') || c > 127);
    }

    // Метод для обновления файла
    private void updateFile(String fileName, String content) {
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(content);
            System.out.println("File updated: " + fileName);
        } catch (IOException e) {
            System.out.println("Error updating file: " + e.getMessage());
        }
    }

    // Метод для удаления файла
    private void deleteFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose a file: ");
        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile.delete()) {
                System.out.println("Deleted file: " + selectedFile);
            } else {
                System.out.println("Failed to delete file.");
            }
        }
    }

    // Метод для сортировки файлов
    private void sortFiles(String sortType, TextArea fileContentArea) {
        String directoryPath = System.getProperty("user.dir");
        try {
            Path path = Paths.get(directoryPath);
            Path[] files = Files.list(path).toArray(Path[]::new);

            switch (sortType) {
                case "Size (Ascending)":
                    Arrays.sort(files, Comparator.comparingLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            e.printStackTrace();
                            return 0L;
                        }
                    }));
                    break;
                case "Size (Descending)":
                    Arrays.sort(files, Comparator.comparingLong(p -> {
                        try {
                            return -Files.size(p);
                        } catch (IOException e) {
                            e.printStackTrace();
                            return 0L;
                        }
                    }));
                    break;
                case "Name (Ascending)":
                    Arrays.sort(files, Comparator.comparing(p -> p.getFileName().toString()));
                    break;
                case "Name (Descending)":
                    Arrays.sort(files, Comparator.comparing(p -> p.getFileName().toString(), Comparator.reverseOrder()));
                    break;
                case "Creation Date (Newest)":
                    Arrays.sort(files, Comparator.comparingLong(p -> {
                        try {
                            BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class);
                            return -attr.creationTime().toMillis();
                        } catch (IOException e) {
                            e.printStackTrace();
                            return 0L;
                        }
                    }));
                    break;
                case "Creation Date (Oldest)":
                    Arrays.sort(files, Comparator.comparingLong(p -> {
                        try {
                            BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class);
                            return attr.creationTime().toMillis();
                        } catch (IOException e) {
                            e.printStackTrace();
                            return 0L;
                        }
                    }));
                    break;
                default:
                    System.out.println("Invalid sort type.");
                    return;
            }

            // Выводим отсортированные файлы
            fileContentArea.clear();
            for (Path file : files) {
                fileContentArea.appendText(file.getFileName() + "\n");
            }
        } catch (IOException e) {
            fileContentArea.setText("Error reading files: " + e.getMessage());
        }
    }
}