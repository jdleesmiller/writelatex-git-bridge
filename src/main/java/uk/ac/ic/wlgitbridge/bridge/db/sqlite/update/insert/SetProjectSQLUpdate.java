package uk.ac.ic.wlgitbridge.bridge.db.sqlite.update.insert;

import uk.ac.ic.wlgitbridge.bridge.db.sqlite.SQLUpdate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Created by Winston on 20/11/14.
 */
public class SetProjectSQLUpdate implements SQLUpdate {

    private static final String SET_PROJECT =
            "INSERT OR REPLACE "
                    + "INTO `projects`(`name`, `version_id`, `last_accessed`) "
                    + "VALUES (?, ?, ?);\n";

    private final String projectName;
    private final int versionID;

    public SetProjectSQLUpdate(String projectName, int versionID) {
        this.projectName = projectName;
        this.versionID = versionID;
    }

    @Override
    public String getSQL() {
        return SET_PROJECT;
    }

    @Override
    public void addParametersToStatement(
            PreparedStatement statement
    ) throws SQLException {
        statement.setString(1, projectName);
        statement.setInt(2, versionID);
        statement.setTimestamp(
                3, Timestamp.valueOf(LocalDateTime.now()));
    }

}
