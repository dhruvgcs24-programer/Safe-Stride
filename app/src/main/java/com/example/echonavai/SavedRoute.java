package com.example.echonavai;

import java.util.List;

public class SavedRoute {
    private String destinationName;
    private List<RouteStep> steps;

    public SavedRoute(String destinationName, List<RouteStep> steps) {
        this.destinationName = destinationName;
        this.steps = steps;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public List<RouteStep> getSteps() {
        return steps;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public void setSteps(List<RouteStep> steps) {
        this.steps = steps;
    }
}