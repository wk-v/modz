package ru.wkov.modz.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
@Data
public class ModzUser {

    @JsonProperty("steamId")
    private String id;

    @JsonProperty("persona_name")
    private String name;
}
