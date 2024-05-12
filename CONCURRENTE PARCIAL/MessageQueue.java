import java.util.ArrayList;
import java.util.List;

import java.util.LinkedList;
import java.util.Queue;

public class MessageQueue {
    private Queue<String> queue;

    public MessageQueue() {
        this.queue = new LinkedList<>();
    }

    public void addMessage(String message) {
        queue.offer(message);
    }

    public String getNextMessage() {
        return queue.poll();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
}

