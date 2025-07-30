package ru.wkov.modz.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
@Data
public class ModzData implements Serializable {

    @JsonProperty("title")
    private String title;

    @JsonProperty("publishedFileId")
    private String workshop;

    @JsonProperty("creator")
    private String createdBy;

    @JsonProperty("file_description")
    private String description;

    @JsonProperty("favorited")
    private Integer favorites;

    @JsonProperty("subscriptions")
    private Integer subscriptions;

    @JsonProperty("time_created")
    private ZonedDateTime createdAt;

    @JsonProperty("time_updated")
    private ZonedDateTime updatedAt;

    @JsonProperty("vote_data")
    private Rate rate = new Rate();

    @JsonProperty("tags")
    private List<Tag> tags = new ArrayList<>();

    @JsonProperty("previews")
    private List<Preview> previews = new ArrayList<>();

    @JsonProperty("children")
    private List<ModzData> requires = new ArrayList<>();

    @Data
    public static class Rate implements Serializable {

        @JsonProperty("score")
        public Double score;

        @JsonProperty("votes_up")
        public Integer up;

        @JsonProperty("votes_down")
        public Integer down;
    }

    @Data
    public static class Tag implements Serializable {

        @JsonProperty("display_name")
        private String name;
    }

    @Data
    public static class Preview implements Serializable {

        @JsonProperty("url")
        private String url;

        @JsonProperty("filename")
        private String name;
    }
}
