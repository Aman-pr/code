import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in); // Initialize once
        String pathEnv = System.getenv("PATH"); // Get the PATH environment variable
        String homeDir = System.getenv("HOME"); // Get the HOME environment variable
        String[] pathDirs = pathEnv.split(":"); // Split PATH into directories

        while (true) {
            System.out.print("$ "); // Print prompt
            String input = scanner.nextLine().trim(); // Get user input and trim leading/trailing spaces

            // Handle the exit command
            if (input.equals("exit") || input.equals("exit 0") || input.equals("0")) {
                break; // Exit the loop and terminate the shell
            }
            // Handle the echo command
            else if (input.startsWith("echo ")) {
                String echoText = input.substring(5).trim(); // Extract the text after "echo "
                
                // Remove surrounding quotes (both single and double quotes)
                if (echoText.startsWith("'") && echoText.endsWith("'")) {
                    echoText = echoText.substring(1, echoText.length() - 1);
                } 

                echoText = processInput(echoText); // Process the text to handle quoting
                System.out.println(echoText); // Print the processed text
            }
            // Handle the pwd command
            else if (input.equals("pwd")) {
                System.out.println(System.getProperty("user.dir")); // Print the current working directory
            }
            // Handle the cd command
            else if (input.startsWith("cd ")) {
                String path = input.substring(3).trim(); // Extract the path after "cd "
                File dir;

                if (path.equals("~")) {
                    dir = new File(homeDir); // Change to the home directory
                } else if (path.startsWith("/")) {
                    dir = new File(path); // Absolute path
                } else {
                    dir = new File(System.getProperty("user.dir"), path); // Relative path
                }

                if (dir.exists() && dir.isDirectory()) {
                    System.setProperty("user.dir", dir.getCanonicalPath()); // Change the current directory
                } else {
                    System.out.println("cd: " + path + ": No such file or directory");
                }
            } 
            // Handle the type command for built-ins
            else if (input.startsWith("type ")) {
                String command = input.substring(5).trim();
                if (command.equals("echo") || command.equals("exit") || command.equals("type") || command.equals("pwd") || command.equals("cd")) {
                    System.out.println(command + " is a shell builtin");
                } else {
                    // Search for the command in PATH directories
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
            // Handle the cat command
            else if (input.startsWith("cat")) {
                String[] commandParts = splitCommand(input); // Split command into parts
                if (commandParts.length > 1) {
                    ArrayList<String> filePaths = new ArrayList<>(); // List to store file paths
                    for (int i = 1; i < commandParts.length; i++) {
                        // Remove surrounding quotes and add to the list
                        String filePath = commandParts[i].replaceAll("^['\"]|['\"]$", "");
                        filePaths.add(filePath);
                    }

                    // Process the files and get the concatenated result
                    String result = processFiles(filePaths.toArray(new String[0]));
                    System.out.println(result); // Print the final result
                } else {
                    System.out.println("cat: missing file operand");
                }
            }
            // Handle external commands
            else {
                String[] commandParts = splitCommand(input); // Handle quoted arguments
                String command = commandParts[0]; // First part is the command

                // Search for the command in PATH directories
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
                            System.out.print(output.toString()); // Print the captured output

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
                // Toggle quoting state
                inQuote = !inQuote;
                quoteChar = inQuote ? c : 0;
            } else if (!inQuote && c == ' ') {
                // Split on spaces outside quotes
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

    // Helper method to process input, handle quotes and escape sequences
    private static String processInput(String input) {
        StringBuilder processedInput = new StringBuilder();
        boolean inQuote = false;
        char prevChar = 0;
        
        // Flag to track spaces between quoted segments
        boolean lastWasSpace = false;
        
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            
            // Toggle quoting state on encountering a quote (excluding escaped quotes)
            if (c == '"' && prevChar != '\\') {
                inQuote = !inQuote;
            } else if (c == '\\' && i + 1 < input.length() &&
                       (input.charAt(i + 1) == '"' || input.charAt(i + 1) == '\\' ||
                        input.charAt(i + 1) == '$' || input.charAt(i + 1) == '\n')) {
                processedInput.append(c); // Escape sequence handling
                i++; 
                processedInput.append(input.charAt(i));
            } else if (inQuote) {
                processedInput.append(c); // Keep characters inside quotes as is
            } else {
                // Handle spaces outside of quotes
                if (c == ' ') {
                    if (!lastWasSpace && processedInput.length() > 0) {
                        processedInput.append(' '); // Insert one space if the last character wasn't space
                        lastWasSpace = true;
                    }
                } else {
                    processedInput.append(c);
                    lastWasSpace = false; // Reset flag when non-space character encountered
                }
            }
            prevChar = c;
        }
        
        return processedInput.toString();
    }

    // Process the files and concatenate them with periods in between
    public static String processFiles(String[] filePaths) throws IOException {
        StringBuilder result = new StringBuilder();
        boolean firstNonEmptyFile = true;

        for (String filePath : filePaths) {
            File file = new File(filePath);
            if (file.exists() && file.isFile()) {
                String fileContent = readFile(file); // Read the file content

                if (!fileContent.isEmpty()) {
                    // Remove consecutive dots and replace with a single dot only at the boundaries
                    // (i.e., only between file contents)
                    if (firstNonEmptyFile) {
                        result.append(fileContent);
                        firstNonEmptyFile = false;
                    } else {
                        result.append(".").append(fileContent);
                    }
                }
            }
        }

        // After concatenating the contents of all files, remove any consecutive dots
        String finalResult = result.toString().replaceAll("\\.+", ".").trim();

        return finalResult;
    }

    // Read content of the file and return it as a string
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