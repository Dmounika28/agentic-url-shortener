package com.assessment.urlshortener.orchestration;

public interface TaskExecutor {

    String execute(EngineeringTask task) throws Exception;
}