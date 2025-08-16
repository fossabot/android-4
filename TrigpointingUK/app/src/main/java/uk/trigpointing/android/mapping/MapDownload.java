package uk.trigpointing.android.mapping;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class MapDownload {
    public String name;
    public String description;
    @JsonProperty("source_url")
    public String sourceUrl;
    public String attribution;
    public int minZoom;
    public int maxZoom;
    public List<Double> bounds;
    public String type;
    public String format;
    @JsonProperty("file_url")
    public String fileUrl;
    @JsonProperty("file_size")
    public long fileSize;
    @JsonProperty("file_timestamp")
    public String fileTimestamp;

    // A class to hold the root of the YAML structure
    public static class MapDownloadsList {
        public String creator;
        public String version;
        public String timestamp;
        public String email;
        public List<MapDownload> maps;
    }
}
