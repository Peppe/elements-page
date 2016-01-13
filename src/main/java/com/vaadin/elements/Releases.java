package com.vaadin.elements;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Releases {

    public static final int CACHE_MINUTES = 15;
    private static final List<String> REPOSITORIES = Arrays.asList(
            "vaadin-core-elements",
            "vaadin-grid",
            "vaadin-combo-box",
            "vaadin-charts");

    private static LoadingCache<String, GitHubRelease> cache;

    private static LoadingCache<String, GitHubRelease> getCache() {
        if (cache == null) {
            init();
        }
        return cache;
    }

    private static void init() {
        cache = CacheBuilder
                .newBuilder()
                .expireAfterWrite(CACHE_MINUTES, TimeUnit.MINUTES)
                .build(new CacheLoader<String, GitHubRelease>() {
                    @Override
                    public GitHubRelease load(String repo) throws Exception {
                        return getGitHubRelease(repo);
                    }
                });

        Unirest.setObjectMapper(new ObjectMapper() {
            Gson gson = new Gson();

            @Override
            public <T> T readValue(String json, Class<T> aClass) {
                return gson.fromJson(json, aClass);
            }

            @Override
            public String writeValue(Object o) {
                return gson.toJson(o);
            }
        });
    }


    public static String getLatestVersionNumber(String repo) {
        GitHubRelease latestRelease = getLatestRelease(repo);
        return latestRelease == null || latestRelease.tagName == null ? "not released" : latestRelease.tagName;
    }

    private static GitHubRelease getLatestRelease(String repo) {
        try {
            return getCache().get(repo);
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }


    public List<GitHubRelease> getLatestReleases() {
        ArrayList<GitHubRelease> releases = new ArrayList<GitHubRelease>();

        for (String repository : REPOSITORIES) {
            GitHubRelease latestRelease = getLatestRelease(repository);
            if(latestRelease != null){
                releases.add(latestRelease);
            }
        }

        return releases;

    }

    private static GitHubRelease getGitHubRelease(String repository) {
        try {
            return Unirest.get("https://api.github.com/repos/vaadin/{repo}/releases/latest")
                    .routeParam("repo", repository)
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "Vaadin")
                    .asObject(GitHubRelease.class)
                    .getBody();
        } catch (UnirestException e) {
            e.printStackTrace();
            return null;
        }
    }
}