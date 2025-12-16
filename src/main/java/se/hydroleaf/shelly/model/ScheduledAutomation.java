package se.hydroleaf.shelly.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ScheduledAutomation {
    private final AutomationDefinition definition;
    private final List<ScheduledFuture<?>> futures = Collections.synchronizedList(new ArrayList<>());

    public void addFuture(ScheduledFuture<?> future) {
        futures.add(future);
    }

    public void cancelAll() {
        futures.forEach(future -> future.cancel(true));
        futures.clear();
    }
}
