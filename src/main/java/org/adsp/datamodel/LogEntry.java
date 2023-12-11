package org.adsp.datamodel;

public record LogEntry(double time, int makespan, int cost, boolean optimal) {
}
