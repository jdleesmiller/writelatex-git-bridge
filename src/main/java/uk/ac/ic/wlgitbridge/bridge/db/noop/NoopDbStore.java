package uk.ac.ic.wlgitbridge.bridge.db.noop;

import uk.ac.ic.wlgitbridge.bridge.db.DBStore;
import uk.ac.ic.wlgitbridge.bridge.db.ProjectState;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public class NoopDbStore implements DBStore {

    @Override
    public void close() { return; }

    @Override
    public void prepareRequest() throws SQLException {
        return;
    }

    @Override
    public void endRequest(RequestEnd end) throws SQLException {
        return;
    }

    @Override
    public int getNumProjects() {
        return 0;
    }

    @Override
    public List<String> getProjectNames() {
        return null;
    }

    @Override
    public void setLatestVersionForProject(String project, int versionID) {

    }

    @Override
    public int getLatestVersionForProject(String project) {
        return 0;
    }

    @Override
    public void addURLIndexForProject(String projectName, String url, String path) {

    }

    @Override
    public void deleteFilesForProject(String project, String... files) {

    }

    @Override
    public String getPathForURLInProject(String projectName, String url) {
        return null;
    }

    @Override
    public String getOldestUnswappedProject() {
        return null;
    }

    @Override
    public int getNumUnswappedProjects() {
        return 0;
    }

    @Override
    public void setRequestEnd(String end) {
        return;
    }

    @Override
    public ProjectState getProjectState(String projectName) {
        return null;
    }

    @Override
    public void setLastAccessedTime(String projectName, Timestamp time) {

    }

}
