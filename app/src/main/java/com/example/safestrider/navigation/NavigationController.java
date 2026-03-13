package com.example.safestrider.navigation;

import java.util.ArrayList;
import java.util.List;

public class NavigationController {

    private boolean guiding = false;
    private boolean destinationAnnounced = false;
    private List<RouteStep> currentSteps = new ArrayList<>();
    private int currentIndex = 0;

    public void startGuidance(SavedRoute route) {
        if (route == null || route.getSteps() == null || route.getSteps().isEmpty()) {
            guiding = false;
            destinationAnnounced = false;
            currentSteps.clear();
            currentIndex = 0;
            return;
        }

        currentSteps = route.getSteps();
        currentIndex = 0;
        guiding = true;
        destinationAnnounced = false;
    }

    public boolean isGuiding() {
        return guiding;
    }

    public String getCurrentInstruction() {
        if (!guiding) {
            return "";
        }

        if (currentIndex < currentSteps.size()) {
            return currentSteps.get(currentIndex).getInstruction();
        }

        if (!destinationAnnounced) {
            destinationAnnounced = true;
            return "Destination reached";
        }

        guiding = false;
        return "";
    }

    public void moveToNextStep() {
        if (!guiding) return;

        if (currentIndex < currentSteps.size()) {
            currentIndex++;
        }
    }

    public void stopGuidance() {
        guiding = false;
        destinationAnnounced = false;
        currentSteps.clear();
        currentIndex = 0;
    }
}