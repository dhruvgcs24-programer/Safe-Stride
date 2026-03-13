package com.example.echonavai;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RouteManager {

    private static final String PREF_NAME = "routes_pref";
    private static final String KEY_ROUTES = "saved_routes";

    private final SharedPreferences preferences;
    private final Gson gson;

    public RouteManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void saveRoute(SavedRoute route) {
        List<SavedRoute> routes = getAllRoutes();
        routes.add(route);

        String json = gson.toJson(routes);
        preferences.edit().putString(KEY_ROUTES, json).apply();
    }

    public List<SavedRoute> getAllRoutes() {
        String json = preferences.getString(KEY_ROUTES, null);

        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }

        SavedRoute[] routesArray = gson.fromJson(json, SavedRoute[].class);
        if (routesArray == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(Arrays.asList(routesArray));
    }

    public SavedRoute getRouteByName(String destinationName) {
        List<SavedRoute> routes = getAllRoutes();

        for (SavedRoute route : routes) {
            if (route.getDestinationName().equalsIgnoreCase(destinationName)) {
                return route;
            }
        }

        return null;
    }

    public void clearAllRoutes() {
        preferences.edit().remove(KEY_ROUTES).apply();
    }
}