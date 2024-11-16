package com.dbdiff.plugin.service;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@State(
    name = "DatabaseDiffConfig",
    storages = {@Storage("databaseDiffConfig.xml")}
)
public class DatabaseConfigService implements PersistentStateComponent<DatabaseConfigService.State> {
    @NotNull
    private State myState = new State();

    public static class State {
        @NotNull
        public String sourceUrl = "";
        @NotNull
        public String sourceUser = "";
        @NotNull
        public String sourcePassword = "";
        @NotNull
        public String targetUrl = "";
        @NotNull
        public String targetUser = "";
        @NotNull
        public String targetPassword = "";
        @NotNull
        public List<String> savedConnections = new ArrayList<>();
        @NotNull
        public Map<String, ConnectionInfo> connectionDetails = new HashMap<>();
    }

    public static class ConnectionInfo {
        public String url;
        public String username;
        public String password;
        public String description;
    }

    public static DatabaseConfigService getInstance(Project project) {
        return project.getService(DatabaseConfigService.class);
    }

    @Nullable
    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        myState = state;
    }

    public void saveConnection(@NotNull String name, @NotNull String url, 
                             @NotNull String username, @NotNull String password) {
        Objects.requireNonNull(name, "Connection name cannot be null");
        Objects.requireNonNull(url, "URL cannot be null");
        Objects.requireNonNull(username, "Username cannot be null");
        Objects.requireNonNull(password, "Password cannot be null");

        ConnectionInfo info = new ConnectionInfo();
        info.url = url;
        info.username = username;
        info.password = password;
        
        myState.savedConnections.add(name);
        myState.connectionDetails.put(name, info);
    }

    public ConnectionInfo getConnection(String name) {
        return myState.connectionDetails.get(name);
    }

    public List<String> getSavedConnections() {
        return new ArrayList<>(myState.savedConnections);
    }
} 