package uk.ac.ic.wlgitbridge.application;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import uk.ac.ic.wlgitbridge.bridge.WriteLatexDataSource;
import uk.ac.ic.wlgitbridge.writelatex.SnapshotPostException;
import uk.ac.ic.wlgitbridge.writelatex.api.request.base.JSONSource;
import uk.ac.ic.wlgitbridge.writelatex.api.request.push.UnexpectedPostbackException;

/**
 * Created by Winston on 17/11/14.
 */
public class SnapshotPushPostbackContents implements JSONSource {

    private static final String CODE_SUCCESS = "upToDate";

    private final WriteLatexDataSource writeLatexDataSource;
    private final String projectName;

    private final SnapshotPostExceptionBuilder snapshotPostExceptionBuilder;

    private int versionID;
    private SnapshotPostException exception;

    public SnapshotPushPostbackContents(WriteLatexDataSource writeLatexDataSource, String projectName, String contents) {
        this.projectName = projectName;
        this.writeLatexDataSource = writeLatexDataSource;
        snapshotPostExceptionBuilder = new SnapshotPostExceptionBuilder();
        fromJSON(new Gson().fromJson(contents, JsonElement.class));
    }

    @Override
    public void fromJSON(JsonElement json) {
        JsonObject responseObject = json.getAsJsonObject();
        String code = responseObject.get("code").getAsString();
        setResult(responseObject, code);
    }

    public void sendPostback() throws UnexpectedPostbackException {
        if (exception == null) {
            writeLatexDataSource.postbackReceivedSuccessfully(projectName, versionID);
        } else {
            writeLatexDataSource.postbackReceivedWithException(projectName, exception);
        }
    }

    private void setResult(JsonObject responseObject, String code) {
        if (code.equals(CODE_SUCCESS)) {
            setVersionID(responseObject);
        } else {
            setException(responseObject, code);
        }
    }

    private void setVersionID(JsonObject responseObject) {
        versionID = responseObject.get("latestVerId").getAsInt();
    }

    private void setException(JsonObject responseObject, String code) {
        try {
            exception = snapshotPostExceptionBuilder.build(code, responseObject);
        } catch (UnexpectedPostbackException e) {
            throw new RuntimeException(e);
        }
    }

}