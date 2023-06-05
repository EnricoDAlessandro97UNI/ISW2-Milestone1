package it.uniroma2.isw2.milestone1.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import it.uniroma2.isw2.milestone1.exceptions.RateLimitExceededGithubAPIException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RestHelper {
	
	private static final Logger LOGGER = Logger.getLogger(RestHelper.class.getName());
	
	private static final String RATE_LIMIT_EXCEEDED = "API rate limit exceeded for user ID";
	
	private RestHelper() {
		
	}
	
	public static JsonObject getJSONObject(String url) throws IOException {
		OkHttpClient client = new OkHttpClient();
		Request request = new Request.Builder().url(url).build();
		Response response = client.newCall(request).execute();
		return JsonParser.parseString(response.body().string()).getAsJsonObject();
	}
	
	public static JsonObject getJSONObject(String url, String token, String cache) throws IOException, RateLimitExceededGithubAPIException {
		String body = getJSON(url, token, cache);
		JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
		if (obj.get("message") != null && obj.get("message").getAsString().startsWith(RATE_LIMIT_EXCEEDED)) {
			throw new RateLimitExceededGithubAPIException();
		}
		if (!Files.exists(Paths.get(cache)) && obj.size() > 0)
			cacheResponse(cache, body);
		return obj;
	}
	
	public static JsonArray getJSONArray(String url, String token, String cache) throws IOException {
		String body = getJSON(url, token, cache);
		JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
		if (!Files.exists(Paths.get(cache)) && arr.size() > 0)
			cacheResponse(cache, body);
		return arr;
	}
	
	private static String getJSON(String url, String token, String cache) throws IOException {
		Path cachePath = Paths.get(cache);

		if (Files.exists(cachePath)) {
			return Files.readString(cachePath);
		} else {
			if (cache != null)
				LOGGER.log(Level.WARNING, String.format("Risorsa %s non trovata nella cache locale", cache));
			OkHttpClient client = new OkHttpClient();
			Request req = new Request.Builder().url(url).header("Authorization", "token " + token).build();
			Response res = client.newCall(req).execute();
			return res.body().string();
		}
	}
	
	private static void cacheResponse(String cacheFile, String body) throws IOException {
		Path cachePath = Paths.get(cacheFile);
		Files.createDirectories(cachePath.getParent());
		Files.createFile(cachePath);
		Files.writeString(cachePath, body, StandardCharsets.UTF_8);
	}
	
}
