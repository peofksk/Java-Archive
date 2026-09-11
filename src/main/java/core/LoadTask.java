package core;

@FunctionalInterface
public interface LoadTask {
    void run() throws Exception;
}