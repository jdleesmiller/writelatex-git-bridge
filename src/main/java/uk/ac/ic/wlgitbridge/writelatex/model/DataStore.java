package uk.ac.ic.wlgitbridge.writelatex.model;

import org.eclipse.jgit.lib.Repository;
import uk.ac.ic.wlgitbridge.bridge.*;
import uk.ac.ic.wlgitbridge.writelatex.SnapshotFetcher;
import uk.ac.ic.wlgitbridge.writelatex.CandidateSnapshot;
import uk.ac.ic.wlgitbridge.writelatex.api.request.exception.FailedConnectionException;
import uk.ac.ic.wlgitbridge.writelatex.api.request.getforversion.SnapshotAttachment;
import uk.ac.ic.wlgitbridge.writelatex.api.request.push.exception.SnapshotPostException;
import uk.ac.ic.wlgitbridge.writelatex.filestore.GitDirectoryContents;
import uk.ac.ic.wlgitbridge.writelatex.filestore.store.WLFileStore;
import uk.ac.ic.wlgitbridge.writelatex.model.db.PersistentStore;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Winston on 06/11/14.
 */
public class DataStore implements CandidateSnapshotCallback {

    private final File rootGitDirectory;
    private final PersistentStore persistentStore;
    private final SnapshotFetcher snapshotFetcher;
    private final ResourceFetcher resourceFetcher;

    public DataStore(String rootGitDirectoryPath) {
        rootGitDirectory = initRootGitDirectory(rootGitDirectoryPath);
        persistentStore = new PersistentStore(rootGitDirectory);
        List<String> excludedFromDeletion = persistentStore.getProjectNames();
        excludedFromDeletion.add(".wlgb");
        WLFileStore.deleteInDirectoryApartFrom(rootGitDirectory, excludedFromDeletion.toArray(new String[] {}));

        snapshotFetcher = new SnapshotFetcher();
        resourceFetcher = new ResourceFetcher(persistentStore);
    }

    public List<WritableRepositoryContents> updateProjectWithName(String name, Repository repository) throws IOException, SnapshotPostException {
        List<Snapshot> snapshots = snapshotFetcher.getSnapshotsForProjectAfterVersion(name, persistentStore.getLatestVersionForProject(name));
        List<WritableRepositoryContents> commits = makeCommitsFromSnapshots(name, repository, snapshots);
        return commits;
    }

    private List<WritableRepositoryContents> makeCommitsFromSnapshots(String name, Repository repository, List<Snapshot> snapshots) throws IOException {
        List<WritableRepositoryContents> commits = new LinkedList<WritableRepositoryContents>();
        for (Snapshot snapshot : snapshots) {
            List<RawFile> files = new LinkedList<RawFile>();
            files.addAll(snapshot.getSrcs());
            for (SnapshotAttachment snapshotAttachment : snapshot.getAtts()) {
                files.add(resourceFetcher.get(name, snapshotAttachment.getUrl(), snapshotAttachment.getPath(), repository));
            }
            commits.add(new GitDirectoryContents(files, rootGitDirectory, name, snapshot));
        }
        return commits;
    }

    public CandidateSnapshot createCandidateSnapshotFromProjectWithContents(String projectName, RawDirectory directoryContents, RawDirectory oldDirectoryContents) throws SnapshotPostException, IOException, FailedConnectionException {
        CandidateSnapshot candidateSnapshot = new CandidateSnapshot(projectName,
                persistentStore.getLatestVersionForProject(projectName),
                directoryContents,
                oldDirectoryContents);
        candidateSnapshot.writeServletFiles(rootGitDirectory);
        return candidateSnapshot;
    }

    @Override
    public void approveSnapshot(int versionID, CandidateSnapshot candidateSnapshot) {
        persistentStore.setLatestVersionForProject(candidateSnapshot.getProjectName(), versionID);
    }

    private File initRootGitDirectory(String rootGitDirectoryPath) {
        File rootGitDirectory = new File(rootGitDirectoryPath);
        rootGitDirectory.mkdirs();
        return rootGitDirectory;
    }

}