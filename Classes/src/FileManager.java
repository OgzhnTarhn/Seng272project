import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class FileManager {
    private Parent parent;
    private Teacher teacher;
    private Child child;

    public FileManager(Parent p, Teacher t, Child c) {
        this.parent = p;
        this.teacher = t;
        this.child = c;
    }

    public void loadTasksFromFile(String filePath, TaskManager taskManager) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                // Yorum veya boş satır atla
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split(";");
                String taskTypeStr = parts[0]; // "TASK1" veya "TASK2"
                String taskId = parts[1];
                String title = parts[2].replace("\"", "");
                String description = parts[3].replace("\"", "");

                if (taskTypeStr.equals("TASK1")) {
                    LocalDateTime deadline = LocalDateTime.parse(parts[4]);
                    int points = Integer.parseInt(parts[5]);
                    Task newTask = new Task(taskId, title, description,
                            TaskType.TASK1,
                            deadline, null, null,
                            points, parent); // Varsayılan parent
                    newTask.setAssignedChild(child);
                    child.addTask(newTask);
                    taskManager.addTask(newTask);
                } else {
                    LocalDateTime startTime = LocalDateTime.parse(parts[4]);
                    LocalDateTime endTime = LocalDateTime.parse(parts[5]);
                    int points = Integer.parseInt(parts[6]);
                    Task newTask = new Task(taskId, title, description,
                            TaskType.TASK2,
                            startTime, startTime, endTime,
                            points, teacher); // Varsayılan teacher
                    newTask.setAssignedChild(child);
                    child.addTask(newTask);
                    taskManager.addTask(newTask);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadWishesFromFile(String filePath, WishManager wishManager) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split(";");
                String wishTypeStr = parts[0]; // "WISH1" / "WISH2"
                String wishId = parts[1];
                String title = parts[2].replace("\"", "");
                String desc = parts[3].replace("\"", "");

                LocalDateTime startTime = null, endTime = null;
                if (!parts[4].equals("null")) {
                    startTime = LocalDateTime.parse(parts[4]);
                }
                if (!parts[5].equals("null")) {
                    endTime = LocalDateTime.parse(parts[5]);
                }

                WishType wType = (wishTypeStr.equals("WISH1")) ? WishType.WISH1 : WishType.WISH2;
                Wish newWish = new Wish(wishId, title, desc, wType,
                        startTime, endTime, child);
                child.addWish(newWish);
                wishManager.addWish(newWish);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadCommands(String filePath,
                             TaskManager taskManager,
                             WishManager wishManager) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                processCommand(line, taskManager, wishManager);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processCommand(String cmd,
                                TaskManager taskManager,
                                WishManager wishManager) {
        if (cmd.startsWith("ADD_TASK1")) {
            parseAddTask1(cmd, taskManager);
        } else if (cmd.startsWith("ADD_TASK2")) {
            parseAddTask2(cmd, taskManager);
        } else if (cmd.startsWith("TASK_DONE")) {
            parseTaskDone(cmd, taskManager);
        } else if (cmd.startsWith("TASK_CHECKED")) {
            parseTaskChecked(cmd, taskManager);
        } else if (cmd.startsWith("ADD_WISH1")) {
            parseAddWish1(cmd, wishManager);
        } else if (cmd.startsWith("ADD_WISH2")) {
            parseAddWish2(cmd, wishManager);
        } else if (cmd.startsWith("WISH_CHECKED")) {
            parseWishChecked(cmd, wishManager);
        } else if (cmd.startsWith("ADD_BUDGET_COIN")) {
            parseAddBudgetCoin(cmd);
        } else if (cmd.startsWith("PRINT_BUDGET")) {
            System.out.println("Child's budget: " + child.getTotalPoints());
        } else if (cmd.startsWith("PRINT_STATUS")) {
            System.out.println("Child's level: " + child.getCurrentLevel());
        } else if (cmd.startsWith("LIST_ALL_TASKS")) {
            parseListAllTasks(cmd, taskManager);
        } else if (cmd.startsWith("LIST_ALL_WISHES")) {
            for (Wish w : wishManager.getAllWishes()) {
                System.out.println(w.toString());
            }
        } else {
            System.out.println("Unknown command: " + cmd);
        }
    }

    // --- Yeni parseListAllTasks metodu (D / V)
    private void parseListAllTasks(String cmd, TaskManager tm) {
        // cmd örn: "LIST_ALL_TASKS D" veya "LIST_ALL_TASKS W"
        String rest = cmd.substring("LIST_ALL_TASKS".length()).trim();
        if (rest.isEmpty()) {
            // Parametre yoksa hata veriyor
            System.out.println("Error: Missing parameter. Use 'LIST_ALL_TASKS D' or 'LIST_ALL_TASKS W'.");
            return;
        }

        if (rest.equals("D")) {
            System.out.println("Daily tasks:");
            List<Task> daily = tm.getDailyTasks();
            if (daily.isEmpty()) {
                System.out.println("No tasks found for today's date.");
            } else {
                for (Task t : daily) {
                    System.out.println(t.toString());
                }
            }
        } else if (rest.equals("V")) {
            System.out.println("Weekly tasks:");
            List<Task> weekly = tm.getWeeklyTasks();
            if (weekly.isEmpty()) {
                System.out.println("No tasks found in the next 7 days range.");
            } else {
                for (Task t : weekly) {
                    System.out.println(t.toString());
                }
            }
        } else {
            System.out.println("Unknown parameter: " + rest + ". Only 'D' or 'W' are allowed.");
        }
    }
    private void parseAddTask1(String line, TaskManager tm) {
        // ADD_TASK1 T 201 "Reading Homework" "Read pages..." 2025-03-05 16:00 8
        try {
            String rest = line.substring("ADD_TASK1".length()).trim();
            String userType = rest.substring(0, 1); // T veya F
            rest = rest.substring(1).trim();

            int sp = rest.indexOf(' ');
            String taskId = rest.substring(0, sp);
            rest = rest.substring(sp).trim();

            String title = extractQuotedText(rest);
            rest = removeFirstQuotedText(rest).trim();

            String desc = extractQuotedText(rest);
            rest = removeFirstQuotedText(rest).trim();

            sp = rest.indexOf(' ');
            String dateStr = rest.substring(0, sp);
            rest = rest.substring(sp).trim();

            sp = rest.indexOf(' ');
            String timeStr = rest.substring(0, sp);
            rest = rest.substring(sp).trim();

            int points = Integer.parseInt(rest);

            LocalDate date = LocalDate.parse(dateStr);
            LocalTime time = LocalTime.parse(timeStr);
            LocalDateTime deadline = LocalDateTime.of(date, time);

            User user = ("T".equals(userType)) ? teacher : parent;
            Task newTask = new Task(taskId, title, desc, TaskType.TASK1,
                    deadline, null, null,
                    points, user);
            newTask.setAssignedChild(child);
            child.addTask(newTask);
            tm.addTask(newTask);

            System.out.println("Added TASK1: " + newTask.toString());
        } catch (Exception e) {
            System.out.println("Error parseAddTask1: " + e.getMessage());
        }
    }

    private void parseAddTask2(String line, TaskManager tm) {
        // ADD_TASK2 F 202 "Painting" "Paint a picture" 2025-03-06 14:00 2025-03-06 15:30 10
        try {
            String rest = line.substring("ADD_TASK2".length()).trim();
            String userType = rest.substring(0, 1); // T / F
            rest = rest.substring(1).trim();

            int sp = rest.indexOf(' ');
            String taskId = rest.substring(0, sp);
            rest = rest.substring(sp).trim();

            String title = extractQuotedText(rest);
            rest = removeFirstQuotedText(rest).trim();

            String desc = extractQuotedText(rest);
            rest = removeFirstQuotedText(rest).trim();

            // Deadline date/time
            sp = rest.indexOf(' ');
            String dDateStr = rest.substring(0, sp);
            rest = rest.substring(sp).trim();

            sp = rest.indexOf(' ');
            String dTimeStr = rest.substring(0, sp);
            rest = rest.substring(sp).trim();

            LocalDateTime deadline = LocalDateTime.of(LocalDate.parse(dDateStr),
                    LocalTime.parse(dTimeStr));

            sp = rest.indexOf(' ');
            String endDateStr = rest.substring(0, sp);
            rest = rest.substring(sp).trim();

            sp = rest.indexOf(' ');
            String endTimeStr = rest.substring(0, sp);
            rest = rest.substring(sp).trim();

            int points = Integer.parseInt(rest);

            LocalDateTime endDt = LocalDateTime.of(LocalDate.parse(endDateStr),
                    LocalTime.parse(endTimeStr));

            User user = ("T".equals(userType)) ? teacher : parent;
            Task newTask = new Task(taskId, title, desc,
                    TaskType.TASK2,
                    deadline, // we can use as "deadline" param
                    deadline, // startTime
                    endDt,    // endTime
                    points,
                    user);
            newTask.setAssignedChild(child);
            child.addTask(newTask);
            tm.addTask(newTask);

            System.out.println("Added TASK2: " + newTask.toString());
        } catch (Exception e) {
            System.out.println("Error parseAddTask2: " + e.getMessage());
        }
    }

    private void parseTaskDone(String line, TaskManager tm) {
        try {
            String rest = line.substring("TASK_DONE".length()).trim();
            tm.markTaskDone(rest);
            child.completeTask(tm.getTaskById(rest));
            System.out.println("Task " + rest + " => DONE by child");
        } catch (Exception e) {
            System.out.println("Error parseTaskDone: " + e.getMessage());
        }
    }

    private void parseTaskChecked(String line, TaskManager tm) {
        try {
            String rest = line.substring("TASK_CHECKED".length()).trim();
            int sp = rest.indexOf(' ');
            String taskId = rest.substring(0, sp);
            String ratingStr = rest.substring(sp).trim();

            int rating = Integer.parseInt(ratingStr);

            Task t = tm.getTaskById(taskId);
            if (t == null) {
                System.out.println("Task not found: " + taskId);
                return;
            }
            parent.approveTask(t, rating);
            System.out.println("Task " + taskId + " => checked with rating " + rating);
        } catch (Exception e) {
            System.out.println("Error parseTaskChecked: " + e.getMessage());
        }
    }

    private void parseAddWish1(String line, WishManager wm) {
        // ADD_WISH1 W301 "Rollerblades" "200 TL"
        try {
            String rest = line.substring("ADD_WISH1".length()).trim();
            int sp = rest.indexOf(' ');
            String wishId = rest.substring(0, sp);
            rest = rest.substring(sp).trim();

            String title = extractQuotedText(rest);
            rest = removeFirstQuotedText(rest).trim();

            String desc = extractQuotedText(rest);
            rest = removeFirstQuotedText(rest).trim();

            Wish w = new Wish(wishId, title, desc,
                    WishType.WISH1,
                    null, null,
                    child);
            child.addWish(w);
            wm.addWish(w);
            System.out.println("ADD_WISH1 -> " + w.toString());
        } catch (Exception e) {
            System.out.println("Error parseAddWish1: " + e.getMessage());
        }
    }

    private void parseAddWish2(String line, WishManager wm) {
        // ADD_WISH2 W302 "Amusement Park" "Entrance + Rides" 2025-05-01 10:00 2025-05-01 18:00
        try {
            String rest = line.substring("ADD_WISH2".length()).trim();

            int sp = rest.indexOf(' ');
            String wishId = rest.substring(0, sp);
            rest = rest.substring(sp).trim();

            String title = extractQuotedText(rest);
            rest = removeFirstQuotedText(rest).trim();

            String desc = extractQuotedText(rest);
            rest = removeFirstQuotedText(rest).trim();

            // start date/time
            sp = rest.indexOf(' ');
            String startDateStr = rest.substring(0, sp);
            rest = rest.substring(sp).trim();

            sp = rest.indexOf(' ');
            String startTimeStr = rest.substring(0, sp);
            rest = rest.substring(sp).trim();

            LocalDateTime startDt = LocalDateTime.of(LocalDate.parse(startDateStr),
                    LocalTime.parse(startTimeStr));

            // end date/time
            sp = rest.indexOf(' ');
            String endDateStr = rest.substring(0, sp);
            rest = rest.substring(sp).trim();

            String endTimeStr = rest;
            LocalDateTime endDt = LocalDateTime.of(LocalDate.parse(endDateStr),
                    LocalTime.parse(endTimeStr));

            Wish w = new Wish(wishId, title, desc,
                    WishType.WISH2, startDt, endDt, child);
            child.addWish(w);
            wm.addWish(w);

            System.out.println("ADD_WISH2 -> " + w.toString());
        } catch (Exception e) {
            System.out.println("Error parseAddWish2: " + e.getMessage());
        }
    }

    private void parseWishChecked(String line, WishManager wm) {
        // WISH_CHECKED W201 APPROVED 2
        // WISH_CHECKED W202 REJECTED
        try {
            String rest = line.substring("WISH_CHECKED".length()).trim();
            int sp = rest.indexOf(' ');
            String wishId = rest.substring(0, sp);
            rest = rest.substring(sp).trim();

            sp = rest.indexOf(' ');
            String decision = (sp == -1) ? rest : rest.substring(0, sp);
            String levelStr = (sp == -1) ? "" : rest.substring(sp).trim();

            Wish w = wm.getWishById(wishId);
            if (w == null) {
                System.out.println("Wish not found: " + wishId);
                return;
            }
            if ("APPROVED".equals(decision)) {
                int lvl = 1;
                if (!levelStr.isEmpty()) {
                    lvl = Integer.parseInt(levelStr);
                }
                parent.approveWish(w, lvl);
                System.out.println("Wish " + wishId + " => APPROVED, reqLevel=" + lvl);
            } else if ("REJECTED".equals(decision)) {
                parent.rejectWish(w);
                System.out.println("Wish " + wishId + " => REJECTED");
            } else {
                System.out.println("Invalid decision: " + decision);
            }
        } catch (Exception e) {
            System.out.println("Error parseWishChecked: " + e.getMessage());
        }
    }

    private void parseAddBudgetCoin(String line) {
        // ADD_BUDGET_COIN 25
        try {
            String rest = line.substring("ADD_BUDGET_COIN".length()).trim();
            int val = Integer.parseInt(rest);
            parent.addBudgetCoin(child, val);
            System.out.println("Added budget coin: " + val);
        } catch (Exception e) {
            System.out.println("Error parseAddBudgetCoin: " + e.getMessage());
        }
    }

    private String extractQuotedText(String text) {
        int firstQ = text.indexOf('"');
        int secondQ = text.indexOf('"', firstQ + 1);
        return text.substring(firstQ + 1, secondQ);
    }

    private String removeFirstQuotedText(String text) {
        int firstQ = text.indexOf('"');
        int secondQ = text.indexOf('"', firstQ + 1);
        String before = text.substring(0, firstQ);
        String after = text.substring(secondQ + 1);
        return before + after;
    }
}
