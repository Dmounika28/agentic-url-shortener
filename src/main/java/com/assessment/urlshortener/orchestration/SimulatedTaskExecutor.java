package com.assessment.urlshortener.orchestration;

import org.springframework.stereotype.Component;

@Component
public class SimulatedTaskExecutor implements TaskExecutor {

    @Override
    public String execute(EngineeringTask task) {
        return "Completed: " + task.getDescription();
    }
}