package com.amplayer.ui;

import cc.nnproject.json.JSONObject;

public class BaseAction {

    public final String type;
    public final String details;
    public final String extra;   // optional — e.g. artwork URL template

    public BaseAction(String type, String details, String extra) {
        this.type    = type;
        this.details = details;
        this.extra   = extra != null ? extra : "";
    }

    public static BaseAction fromJSON(JSONObject obj) {
        if (obj == null) return null;
        return new BaseAction(
            obj.getString("type",    ""),
            obj.getString("details", ""),
            obj.getString("extra",   "")
        );
    }
}
