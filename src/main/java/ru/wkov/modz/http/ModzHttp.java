package ru.wkov.modz.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import lombok.SneakyThrows;
import ru.wkov.modz.ModzBean;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES;
import static java.lang.Integer.min;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.exists;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzHttp implements ModzBean {

    private final Map<String, ModzData> cache;

    private final HttpClient client;

    private final JsonMapper mapper;

    private final Path path;

    @SneakyThrows
    public ModzHttp() {
        cache = new HashMap<>();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10L))
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        mapper = JsonMapper.builder()
                .serializationInclusion(NON_EMPTY)
                .configure(ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
                .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
                .addModule(new JavaTimeModule())
                .build();

        path = getRootPath().resolve("steam.json");
        if (exists(path)) {
            cache(List.of(mapper.readValue(path.toFile(), ModzData[].class)));
        }
    }

    @SneakyThrows
    public Collection<ModzUser> getAuthors(Collection<String> ids) {
        var uri = ModzUrls
                .api("https://api.steampowered.com/IPlayerService/GetPlayerLinkDetails/v1/")
                .add("steamIds", ids)
                .add("key", getWebApiKey())
                .uri();

        var request = HttpRequest
                .newBuilder()
                .GET()
                .uri(uri)
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        var body = client
                .send(request, ofString(UTF_8))
                .body();

        return mapper
                .readValue(body, Body.class)
                .getResponse()
                .getAccounts()
                .stream()
                .map(Body.Response.Account::getUser)
                .collect(toSet());
    }

    @SneakyThrows
    public Collection<ModzData> getDetails(Collection<String> ids) {
        var pfi = ids.stream().filter(id -> !cache.containsKey(id)).toList();
        var key = getWebApiKey();

        if (!pfi.isEmpty() && key != null) {
            var uri = ModzUrls
                    .api("https://api.steampowered.com/IPublishedFileService/GetDetails/v1/")
                    .add("includeAdditionalPreviews", true)
//                  .add("strip_description_bbcode", true)
//                  .add("includeReactions", true)
                    .add("includeChildren", true)
//                  .add("includeMetadata", true)
//                  .add("includeKvTags", true)
                    .add("includeVotes", true)
//                  .add("includeTags", true)
                    .add("publishedFileIds", pfi)
                    .add("key", key)
                    .uri();

            var request = HttpRequest
                    .newBuilder()
                    .GET()
                    .uri(uri)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .build();

            var response = client
                    .send(request, ofString(UTF_8));

            if (response.statusCode() != 200) {
                throw new IllegalStateException(response.statusCode() + "\n" + response.body());
            }

            var details = mapper.readValue(response.body(), Body.class).getResponse().getDetails();

            var grouped = details.stream().collect(groupingBy(ModzData::getCreatedBy));
            var keys = new ArrayList<>(grouped.keySet());
            for (int i = 0; i < keys.size(); ) {
                for (var author : getAuthors(keys.subList(i, min(i += 50, keys.size())))) {
                    for (var data : grouped.get(author.getId())) {
                        data.setCreatedBy(author.getName());
                    }
                }
            }

            cache(details);
        }

        return ids.stream().map(cache::get).filter(Objects::nonNull).toList();
    }

    @SneakyThrows
    private void cache(List<ModzData> details) {
        for (var data : details) {
            cache.put(data.getWorkshop(), data);
        }

        mapper.writeValue(path.toFile(), cache.values());
    }

    @Data
    public static class Body {

        @JsonProperty("response")
        private Response response;

        @Data
        public static class Response {

            @JsonProperty("accounts")
            private List<Account> accounts;

            @JsonProperty("publishedFileDetails")
            private List<ModzData> details;

            @Data
            public static class Account {

                @JsonProperty("public_data")
                private ModzUser user;
            }
        }
    }
}
