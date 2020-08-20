package ru.parking.localservice_realm;

import io.reactivex.Scheduler;


public interface SchedulerFactory {

    Scheduler androidScheduler();

    Scheduler io();

    Scheduler getScheduler(Class clazz);

    Scheduler androidHeavyScheduler();

    void tryPost(long threadId, Runnable task);
}
