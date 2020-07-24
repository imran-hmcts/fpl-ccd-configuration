package uk.gov.hmcts.reform.fpl;

import java.util.List;
import java.util.Optional;

public class TaskSection {

    private final String name;
    private final List<Task> tasks;
    private String hint;
    private String info;

    public TaskSection(String name, List<Task> tasks) {
        this.name = name;
        this.tasks = tasks;
    }

    public static TaskSection newSection(String name, List<Task> tasks) {
        return new TaskSection(name, tasks);
    }

    public TaskSection withHint(String hint) {
        this.hint = hint;
        return this;
    }

    public TaskSection withInfo(String info) {
        this.info = info;
        return this;
    }

    public String getName() {
        return name;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public Optional<String> getHint() {
        return Optional.ofNullable(hint);
    }

    public Optional<String> getInfo() {
        return Optional.ofNullable(info);
    }
}