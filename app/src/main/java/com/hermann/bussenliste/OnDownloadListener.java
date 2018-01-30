package com.hermann.bussenliste;

import org.json.JSONException;

/**
 * Created by sebas on 29.01.2018.
 */

public interface OnDownloadListener {
    public void downloadTaskCompleted (String response);
    public void downloadTaskFailed (int statusCode);
    public void updateSyncStatusFailed (JSONException e);
}
