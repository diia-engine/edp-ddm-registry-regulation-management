package com.epam.digital.data.platform.management.service;

import com.epam.digital.data.platform.management.model.dto.ChangeInfo;
import com.epam.digital.data.platform.management.model.dto.VersionedFileInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import java.util.List;
import org.springframework.lang.Nullable;

public interface VersionManagementService {

    /**
     * Get versions list
     */
    List<ChangeInfo> getVersionsList() throws RestApiException;


    @Nullable
    ChangeInfo getMasterInfo() throws RestApiException;

    /**
     * Details of head master
     */
    List<String> getDetailsOfHeadMaster(String path) throws Exception;

    /**
     * Details of current version
     */
    List<VersionedFileInfo> getVersionFileList(String versionName) throws Exception;

    /**
     * Create new version
     */
    String createNewVersion(String subject) throws RestApiException;

    ChangeInfo getVersionDetails(String versionName) throws RestApiException;

//    /**
//     * Mark reviewed the version
//     */
//    boolean markReviewed(String versionName);
//
//    /**
//     * Submit version by name
//     */
//    void submit(String versionName);
//
//    /**
//     * Decline version by name
//     */
//    void decline(String versionName);
//
//    /**
//     * Rebase version
//     */
//    void rebase(String versionName);
//
//    /**
//     * Put votes to review
//     */
//    boolean vote(String versionName, String label, short value);
//
//    /**
//     * Add robot comment
//     */
//    void robotComment(String versionName, String robotId, String robotRunId, String comment, String message, String filePath);
}
