package planner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;
import java.util.stream.Collectors;

public class StudyPlannerApp {

    private static final Scanner SCANNER = new Scanner(System.in);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private static final List<Subject> subjects = new ArrayList<>();
    private static final List<StudySession> schedule = new ArrayList<>();
    private static boolean remindersRunning = false;

    public static void main(String[] args) {
        printBanner();
        boolean exit = false;
        while (!exit) {
            printMenu();
            int choice = readInt("Choose an option: ");
            switch (choice) {
                case 1:
                    enterSubject();
                    break;
                case 2:
                    enterExamDate();
                    break;
                case 3:
                    calculateDaysRemaining();
                    break;
                case 4:
                    allocateStudyHours();
                    break;
                case 5:
                    generateStudySchedule();
                    break;
                case 6:
                    trackCompletedSession();
                    break;
                case 7:
                    displayProgressReport();
                    break;
                case 8:
                    setStudyReminders();
                    break;
                case 9:
                    exit = true;
                    break;
                default:
                    System.out.println("Invalid option. Try again.");
            }
        }
        System.out.println("Goodbye!");
        System.exit(0);
    }

    private static void printBanner() {
        // Simple ANSI colors for terminals that support it (including recent PowerShell)
        String purple = "\u001B[95m";
        String reset = "\u001B[0m";

        System.out.println();
        System.out.println(purple + "****************************************************" + reset);
        System.out.println(purple + "*                                                  *" + reset);
        System.out.println(purple + "*             NWU STUDY PLANNER  v1.0              *" + reset);
        System.out.println(purple + "*                                                  *" + reset);
        System.out.println(purple + "****************************************************" + reset);
        System.out.println("Plan your modules, balance your time,");
        System.out.println("and track your progress towards exam success.\n");
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("1. Enter subjects");
        System.out.println("2. Enter exam dates");
        System.out.println("3. Calculate days remaining");
        System.out.println("4. Allocate study hours");
        System.out.println("5. Generate study schedule");
        System.out.println("6. Track completed session");
        System.out.println("7. Display progress report");
        System.out.println("8. Set study reminders");
        System.out.println("9. Exit");
    }

    // 1. Enter subjects
    private static void enterSubject() {
        System.out.print("Enter subject name (or blank to stop): ");
        String name = SCANNER.nextLine().trim();
        while (!name.isEmpty()) {
            if (findSubject(name).isPresent()) {
                System.out.println("Subject already exists.");
            } else {
                subjects.add(new Subject(name));
                System.out.println("Subject added.");
            }
            System.out.print("Enter subject name (or blank to stop): ");
            name = SCANNER.nextLine().trim();
        }
    }

    // 2. Enter exam dates
    private static void enterExamDate() {
        if (subjects.isEmpty()) {
            System.out.println("No subjects. Add subjects first.");
            return;
        }
        Subject subject = chooseSubject();
        if (subject == null) {
            return;
        }
        LocalDate date = readDate("Enter exam date for " + subject.getName() + " (yyyy-MM-dd): ");
        subject.setExamDate(date);
        System.out.println("Exam date set for " + subject.getName() + ": " + date);
    }

    // 3. Calculate days remaining
    private static void calculateDaysRemaining() {
        if (subjects.isEmpty()) {
            System.out.println("No subjects.");
            return;
        }
        LocalDate today = LocalDate.now();
        System.out.println("Days remaining until each exam (from " + today + "):");
        for (Subject s : subjects) {
            if (s.getExamDate() == null) {
                System.out.println("- " + s.getName() + ": exam date not set");
            } else {
                long days = ChronoUnit.DAYS.between(today, s.getExamDate());
                if (days < 0) {
                    System.out.println("- " + s.getName() + ": exam date has passed");
                } else {
                    System.out.println("- " + s.getName() + ": " + days + " day(s) remaining");
                }
            }
        }
    }

    // 4. Allocate study hours (total per subject)
    private static void allocateStudyHours() {
        if (subjects.isEmpty()) {
            System.out.println("No subjects.");
            return;
        }
        Subject subject = chooseSubject();
        if (subject == null) {
            return;
        }
        double hours = readDouble("Enter total study hours to allocate for " + subject.getName() + ": ");
        if (hours <= 0) {
            System.out.println("Hours must be positive.");
            return;
        }
        subject.setTotalStudyHours(hours);
        System.out.println("Allocated " + hours + " hour(s) to " + subject.getName() + ".");
    }

    // 5. Generate study schedule
    private static void generateStudySchedule() {
        schedule.clear();
        LocalDate today = LocalDate.now();

        List<Subject> schedulable = subjects.stream()
                .filter(s -> s.getExamDate() != null && s.getTotalStudyHours() > 0)
                .collect(Collectors.toList());

        if (schedulable.isEmpty()) {
            System.out.println("No subjects with both exam date and study hours set.");
            return;
        }

        for (Subject s : schedulable) {
            long days = ChronoUnit.DAYS.between(today, s.getExamDate());
            if (days <= 0) {
                System.out.println("Skipping " + s.getName() + " because the exam date is today or in the past.");
                continue;
            }
            double hoursPerDay = s.getTotalStudyHours() / days;
            for (int i = 0; i < days; i++) {
                LocalDate day = today.plusDays(i);
                // default start time 18:00 (6 PM)
                LocalTime startTime = LocalTime.of(18, 0);
                schedule.add(new StudySession(
                        UUID.randomUUID().toString(),
                        s,
                        LocalDateTime.of(day, startTime),
                        hoursPerDay
                ));
            }
        }

        if (schedule.isEmpty()) {
            System.out.println("No sessions generated.");
            return;
        }

        schedule.sort(Comparator.comparing(StudySession::getStart));
        System.out.println("Study schedule generated:");
        printSchedule(schedule);
    }

    // 6. Track completed session
    private static void trackCompletedSession() {
        if (schedule.isEmpty()) {
            System.out.println("No scheduled sessions. Generate a schedule first.");
            return;
        }

        System.out.println("Scheduled sessions:");
        printSchedule(schedule);
        String id = readString("Enter the Session ID you completed: ");

        Optional<StudySession> optional = schedule.stream()
                .filter(s -> s.getId().equals(id))
                .findFirst();

        if (!optional.isPresent()) {
            System.out.println("Session ID not found.");
            return;
        }

        StudySession session = optional.get();
        if (session.isCompleted()) {
            System.out.println("Session is already marked as completed.");
            return;
        }

        session.setCompleted(true);
        session.getSubject().addCompletedHours(session.getDurationHours());
        System.out.println("Marked session as completed for subject: " + session.getSubject().getName());
    }

    // 7. Display progress report
    private static void displayProgressReport() {
        if (subjects.isEmpty()) {
            System.out.println("No subjects.");
            return;
        }
        System.out.println("Progress report:");
        for (Subject s : subjects) {
            double total = s.getTotalStudyHours();
            double done = s.getCompletedHours();
            double percent = total > 0 ? (done / total) * 100.0 : 0.0;
            String examInfo = s.getExamDate() == null ? "Exam not set" : "Exam: " + s.getExamDate();
            System.out.printf("- %s | %s | %.2f/%.2f hours (%.1f%%)\n",
                    s.getName(), examInfo, done, total, percent);
        }
    }

    // 8. Set study reminders
    private static void setStudyReminders() {
        if (schedule.isEmpty()) {
            System.out.println("No sessions in schedule. Generate a schedule first.");
            return;
        }
        if (remindersRunning) {
            System.out.println("Reminders are already active while the program is running.");
            return;
        }

        remindersRunning = true;
        System.out.println("Reminders are now active. You will see reminders in the console at the start time of each session while this program remains open.");

        Thread reminderThread = new Thread(() -> {
            while (remindersRunning) {
                try {
                    LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
                    for (StudySession session : schedule) {
                        if (!session.isCompleted() && !session.isReminded()) {
                            LocalDateTime start = session.getStart().truncatedTo(ChronoUnit.MINUTES);
                            if (!start.isAfter(now)) {
                                System.out.println();
                                System.out.println(">>> Reminder: It's time to study "
                                        + session.getSubject().getName()
                                        + " for " + String.format("%.1f", session.getDurationHours())
                                        + " hour(s) starting at "
                                        + session.getStart().toLocalTime().format(TIME_FORMAT)
                                        + ".");
                                session.setReminded(true);
                            }
                        }
                    }
                    Thread.sleep(60_000); // check every minute
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        reminderThread.setDaemon(true);
        reminderThread.start();
    }

    // Helpers

    private static void printSchedule(List<StudySession> sessions) {
        System.out.println("ID | Date | Time | Subject | Duration (hours) | Completed");
        for (StudySession s : sessions) {
            System.out.printf("%s | %s | %s | %s | %.2f | %s\n",
                    s.getId(),
                    s.getStart().toLocalDate().format(DATE_FORMAT),
                    s.getStart().toLocalTime().format(TIME_FORMAT),
                    s.getSubject().getName(),
                    s.getDurationHours(),
                    s.isCompleted() ? "Yes" : "No");
        }
    }

    private static Subject chooseSubject() {
        System.out.println("Available subjects:");
        for (int i = 0; i < subjects.size(); i++) {
            System.out.println((i + 1) + ". " + subjects.get(i).getName());
        }
        int index = readInt("Choose subject number (0 to cancel): ");
        if (index == 0) {
            return null;
        }
        if (index < 1 || index > subjects.size()) {
            System.out.println("Invalid index.");
            return null;
        }
        return subjects.get(index - 1);
    }

    private static Optional<Subject> findSubject(String name) {
        return subjects.stream()
                .filter(s -> s.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    private static int readInt(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                String line = SCANNER.nextLine();
                return Integer.parseInt(line.trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }

    private static double readDouble(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                String line = SCANNER.nextLine();
                return Double.parseDouble(line.trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static String readString(String prompt) {
        System.out.print(prompt);
        return SCANNER.nextLine().trim();
    }

    private static LocalDate readDate(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                String line = SCANNER.nextLine().trim();
                return LocalDate.parse(line, DATE_FORMAT);
            } catch (Exception e) {
                System.out.println("Invalid date format. Use yyyy-MM-dd.");
            }
        }
    }

    // Data classes

    private static class Subject {
        private final String name;
        private LocalDate examDate;
        private double totalStudyHours;
        private double completedHours;

        Subject(String name) {
            this.name = name;
        }

        String getName() {
            return name;
        }

        LocalDate getExamDate() {
            return examDate;
        }

        void setExamDate(LocalDate examDate) {
            this.examDate = examDate;
        }

        double getTotalStudyHours() {
            return totalStudyHours;
        }

        void setTotalStudyHours(double totalStudyHours) {
            this.totalStudyHours = totalStudyHours;
        }

        double getCompletedHours() {
            return completedHours;
        }

        void addCompletedHours(double hours) {
            this.completedHours += hours;
        }
    }

    private static class StudySession {
        private final String id;
        private final Subject subject;
        private final LocalDateTime start;
        private final double durationHours;
        private boolean completed;
        private boolean reminded;

        StudySession(String id, Subject subject, LocalDateTime start, double durationHours) {
            this.id = id;
            this.subject = subject;
            this.start = start;
            this.durationHours = durationHours;
        }

        String getId() {
            return id;
        }

        Subject getSubject() {
            return subject;
        }

        LocalDateTime getStart() {
            return start;
        }

        double getDurationHours() {
            return durationHours;
        }

        boolean isCompleted() {
            return completed;
        }

        void setCompleted(boolean completed) {
            this.completed = completed;
        }

        boolean isReminded() {
            return reminded;
        }

        void setReminded(boolean reminded) {
            this.reminded = reminded;
        }
    }
}

