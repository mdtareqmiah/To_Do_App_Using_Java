import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class TodoApp extends JFrame {

    // UI components
    private JTextField descriptionField;
    private JButton addButton, updateButton, markCompletedButton, refreshButton, resetButton;
    private JList<String> taskList;
    private JList<String> completedTaskList;
    private DefaultListModel<String> taskListModel;
    private DefaultListModel<String> completedTaskListModel;
    private int currentTaskId; // To store the ID of the selected task for updating

    // Database connection
    private Connection connection;

    public TodoApp() {
        // Set up the UI
        setTitle("To-Do List Application");
        setSize(1000, 700);
        setResizable(false); // Prevent resizing
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        //setLayout(new BorderLayout());

        // Initialize the UI components
        descriptionField = new JTextField(30);

        addButton = new JButton("Add Task");
        updateButton = new JButton("Update Task");
        markCompletedButton = new JButton("Mark Completed");
        refreshButton = new JButton("Refresh");
        resetButton = new JButton("Reset");

        taskListModel = new DefaultListModel<>();
        completedTaskListModel = new DefaultListModel<>();
        taskList = new JList<>(taskListModel);
        completedTaskList = new JList<>(completedTaskListModel);
        taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        completedTaskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Add components to the frame
        JPanel panel = new JPanel();
        panel.add(new JLabel("Description: "));
        panel.add(descriptionField);
        panel.add(addButton);
        panel.add(updateButton);
        panel.add(markCompletedButton);
        panel.add(refreshButton);
        panel.add(resetButton);

        // Set up lists in a panel
        JPanel listPanel = new JPanel(new GridLayout(1, 2));
        listPanel.add(new JScrollPane(taskList));
        listPanel.add(new JScrollPane(completedTaskList));

        add(panel, BorderLayout.NORTH);
        add(listPanel, BorderLayout.CENTER);

        // Set up database connection
        setupDBConnection();

        // Load tasks from the database
        loadTasks();

        // Add Task Button functionality
        addButton.addActionListener(e -> {
            String description = descriptionField.getText().trim();
            if (!description.isEmpty()) {
                addTask(description);
                descriptionField.setText("");
            }
        });

        // Update Task Button functionality
        updateButton.addActionListener(e -> {
            if (currentTaskId > 0) {
                String description = descriptionField.getText().trim();
                if (!description.isEmpty()) {
                    updateTask(currentTaskId, description);
                    descriptionField.setText("");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Select a task to update.");
            }
        });

        // Mark Task Completed Button functionality
        markCompletedButton.addActionListener(e -> {
            int selectedIndex = taskList.getSelectedIndex();
            if (selectedIndex != -1) {
                String task = taskList.getSelectedValue();
                markTaskCompleted(task);
            }
        });

        // Refresh Button to reload tasks from the database
        refreshButton.addActionListener(e -> loadTasks());

        // Reset Button to clear all tasks from the database
        resetButton.addActionListener(e -> {
            int confirmation = JOptionPane.showConfirmDialog(this, "Are you sure you want to reset all tasks?", "Confirm Reset", JOptionPane.YES_NO_OPTION);
            if (confirmation == JOptionPane.YES_OPTION) {
                resetTasks();
            }
        });

        // Task selection listener to enable task updates
        taskList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedIndex = taskList.getSelectedIndex();
                if (selectedIndex != -1) {
                    String task = taskList.getSelectedValue();
                    currentTaskId = getTaskIdFromList(task);
                    descriptionField.setText(task.split(" \\| ")[1].replace("Description: ", "").trim());
                }
            }
        });
    }

    private void setupDBConnection() {
        try {
            // Database connection details
            String url = "jdbc:mysql://localhost:3306/todo_db";
            String username = "root";  // change this to your MySQL username
            String password = "Tareq@688021";  // change this to your MySQL password

            connection = DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database connection failed.");
        }
    }

    private void loadTasks() {
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT task_id, description, status, created_at FROM tasks");

            taskListModel.clear();
            completedTaskListModel.clear(); // Clear completed tasks list
            while (resultSet.next()) {
                int taskId = resultSet.getInt("task_id");
                String description = resultSet.getString("description");
                String status = resultSet.getString("status");
                Timestamp createdAt = resultSet.getTimestamp("created_at");

                String taskEntry = "ID: " + taskId + " | Description: " + description + " | Status: " + status +
                                   " | Created: " + createdAt;
                if (status.equals("completed")) {
                    completedTaskListModel.addElement(taskEntry); // Add to completed tasks
                } else {
                    taskListModel.addElement(taskEntry); // Add to pending tasks
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addTask(String description) {
        try {
            String query = "INSERT INTO tasks (description, status, created_at) VALUES (?, 'pending', CURRENT_TIMESTAMP)";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, description);
            preparedStatement.executeUpdate();
            loadTasks();  // Reload tasks after adding
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateTask(int taskId, String description) {
        try {
            String query = "UPDATE tasks SET description = ? WHERE task_id = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, description);
            preparedStatement.setInt(2, taskId);
            preparedStatement.executeUpdate();
            loadTasks();  // Reload tasks after updating
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void markTaskCompleted(String task) {
        try {
            int taskId = getTaskIdFromList(task);
            String query = "UPDATE tasks SET status = 'completed' WHERE task_id = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, taskId);
            preparedStatement.executeUpdate();
            loadTasks();  // Reload tasks to reflect the changes
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void resetTasks() {
        try {
            String query = "DELETE FROM tasks";
            Statement statement = connection.createStatement();
            statement.executeUpdate(query);
            loadTasks();  // Reload tasks after reset
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Helper method to extract the task ID from the list entry
    private int getTaskIdFromList(String listEntry) {
        return Integer.parseInt(listEntry.split(" \\| ")[0].replace("ID: ", ""));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new TodoApp().setVisible(true);
        });
    }
}
