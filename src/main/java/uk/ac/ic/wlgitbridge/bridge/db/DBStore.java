package uk.ac.ic.wlgitbridge.bridge.db;

import uk.ac.ic.wlgitbridge.bridge.db.postgres.PostgresConfig;
import uk.ac.ic.wlgitbridge.bridge.db.postgres.PostgresDBStore;
import uk.ac.ic.wlgitbridge.bridge.db.postgres.PostgresOptions;
import uk.ac.ic.wlgitbridge.bridge.db.sqlite.SqliteDBStore;
import uk.ac.ic.wlgitbridge.bridge.repo.RepoStore;
import uk.ac.ic.wlgitbridge.util.Log;

import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * Created by winston on 20/08/2016.
 */
public interface DBStore {

    int getNumProjects();

    List<String> getProjectNames();

    void setLatestVersionForProject(String project, int versionID);

    int getLatestVersionForProject(String project);

    void addURLIndexForProject(String projectName, String url, String path);

    void deleteFilesForProject(String project, String... files);

    String getPathForURLInProject(String projectName, String url);

    String getOldestUnswappedProject();

    void swap(String projectName, String compressionMethod);

    void restore(String projectName);

    String getSwapCompression(String projectName);

    int getNumUnswappedProjects();

    ProjectState getProjectState(String projectName);

    /**
     * Sets the last accessed time for the given project name.
     * @param projectName the project's name
     * @param time the time, or null if the project is to be swapped
     */
    void setLastAccessedTime(String projectName, Timestamp time);
}
