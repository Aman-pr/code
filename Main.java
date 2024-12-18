import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        String pathEnv = System.getenv("PATH");
        String homeDir = System.getenv("HOME");
        String[] pathDirs = pathEnv.split(":");

        while (true) {
            System.out.print("$ ");
            String input = scanner.nextLine().trim();

            if (input.equals("exit") || input.equals("exit 0") || input.equals("0")) {
                break;
            }
            else if (input.startsWith("echo ")) {
                String echoText = input.substring(5);
                StringBuilder result = new StringBuilder();
                boolean inQuotes = false;
                char quoteType = 0;
                
                for (int i = 0; i < echoText.length(); i++) {
                    char c = echoText.charAt(i);
                    
                    if ((c == '\'' || c == '"') && !inQuotes) {
                        inQuotes = true;
                        quoteType = c;
                        continue;
                    } else if (c == quoteType && inQuotes) {
                        inQuotes = false;
                        quoteType = 0;
                        continue;
                    }
                    
                    if (c == ' ' && !inQuotes) {
                        if (result.length() > 0 && result.charAt(result.length() - 1) != ' ') {
                            result.append(' ');
                        }
                    } else {
                        result.append(c);
                    }
                }
                
                System.out.println(result.toString().trim());
            }
            else if (input.equals("pwd")) {
                String currentDir = System.getProperty("user.dir");
                System.out.println(currentDir);  // Output the directory path without the "$ "
            }


            else if (input.startsWith("cd ")) {
                String path = input.substring(3).trim();
                File dir;

                if (path.equals("~")) {
                    dir = new File(homeDir);
                } else if (path.startsWith("/")) {
                    dir = new File(path);
                } else {
                    dir = new File(System.getProperty("user.dir"), path);
                }

                if (dir.exists() && dir.isDirectory()) {
                    System.setProperty("user.dir", dir.getCanonicalPath());  // Change the working directory
                } else {
                    System.out.println("cd: " + path + ": No such file or directory");
                }
            }
            else if (input.equals("pwd")) {
                String currentDir = System.getProperty("user.dir");
                System.out.println("$ " + currentDir);  // Only print the current directory when executing "pwd"
            }
            else if (input.startsWith("type ")) {
                String command = input.substring(5).trim();
                if (command.equals("echo") || command.equals("exit") || command.equals("type") || 
                    command.equals("pwd") || command.equals("cd")) {
                    System.out.println(command + " is a shell builtin");
                } else {
                    boolean found = false;
                    for (String dir : pathDirs) {
                        File file = new File(dir, command);
                        if (file.exists() && file.isFile() && file.canExecute()) {
                            System.out.println(command + " is " + file.getAbsolutePath());
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        System.out.println(command + ": not found");
                    }
                }
            }
            else if (input.startsWith("cat")) {
                String[] commandParts = splitCommand(input);
                if (commandParts.length > 1) {
                    ArrayList<String> filePaths = new ArrayList<>();
                    for (int i = 1; i < commandParts.length; i++) {
                        String filePath = commandParts[i].replaceAll("^['\"]|['\"]$", "");
                        filePaths.add(filePath);
                    }
                    String result = processFiles(filePaths.toArray(new String[0]));
                    System.out.println(result);
                } else {
                    System.out.println("cat: missing file operand");
                }
            }
            else {
                String[] commandParts = splitCommand(input);
                String command = commandParts[0];

                boolean found = false;
                for (String dir : pathDirs) {
                    File file = new File(dir, command);
                    if (file.exists() && file.isFile() && file.canExecute()) {
                        found = true;
                        try {
                            ProcessBuilder processBuilder = new ProcessBuilder(commandParts);
                            processBuilder.redirectErrorStream(true);
                            Process process = processBuilder.start();

                            Scanner processScanner = new Scanner(process.getInputStream());
                            StringBuilder output = new StringBuilder();
                            while (processScanner.hasNextLine()) {
                                output.append(processScanner.nextLine()).append("\n");
                            }
                            processScanner.close();
                            System.out.print(output.toString());

                            process.waitFor();
                        } catch (Exception e) {
                            System.out.println(command + ": error while executing command");
                        }
                        break;
                    }
                }
                if (!found) {
                    System.out.println(command + ": not found");
                }
            }
        }
    }

    private static String[] splitCommand(String input) {
        ArrayList<String> parts = new ArrayList<>();
        StringBuilder currentPart = new StringBuilder();
        boolean inQuote = false;
        char quoteChar = 0;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if ((c == '"' || c == '\'') && (quoteChar == 0 || quoteChar == c)) {
                inQuote = !inQuote;
                quoteChar = inQuote ? c : 0;
            } else if (!inQuote && c == ' ') {
                if (currentPart.length() > 0) {
                    parts.add(currentPart.toString());
                    currentPart.setLength(0);
                }
            } else {
                currentPart.append(c);
            }
        }

        if (currentPart.length() > 0) {
            parts.add(currentPart.toString());
        }

        return parts.toArray(new String[0]);
    }

    public static String processFiles(String[] filePaths) throws IOException {
        StringBuilder result = new StringBuilder();
        boolean firstNonEmptyFile = true;

        for (String filePath : filePaths) {
            File file = new File(filePath);
            if (file.exists() && file.isFile()) {
                String fileContent = readFile(file);

                if (!fileContent.isEmpty()) {
                    if (firstNonEmptyFile) {
                        result.append(fileContent);
                        firstNonEmptyFile = false;
                    } else {
                        result.append(".").append(fileContent);
                    }
                }
            }
        }

        String finalResult = result.toString().replaceAll("\\.+", ".").trim();
        return finalResult;
    }

    private static String readFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString().trim();
    }
}    
  
