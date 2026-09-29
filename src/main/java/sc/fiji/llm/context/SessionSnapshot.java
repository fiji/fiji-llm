/*-
 * #%L
 * Fiji software for LLM integration.
 * %%
 * Copyright (C) 2025 - 2026 Fiji developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package sc.fiji.llm.context;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * A brief listing of what is open in Fiji, added to each chat message so the
 * model knows which scripts and images exist without their content being sent.
 * It reuses the output of the listing tools, so it matches what the model gets
 * by calling them.
 */
public final class SessionSnapshot {

	/** Maximum number of images listed; the active image is always included. */
	static final int MAX_IMAGES = 20;

	private static final String HEADER =
		"=== FIJI SESSION SNAPSHOT (open items as of this message; NOT attached, use tools to inspect) ===";
	private static final String FOOTER = "=== END FIJI SESSION SNAPSHOT ===";

	private SessionSnapshot() {
		// utility class
	}

	/**
	 * Formats the snapshot from listing tool results.
	 *
	 * @param scriptList result of fiji_script_list, or null if unavailable
	 * @param imageList result of fiji_image_list, or null if unavailable
	 * @return the snapshot, or an empty string if neither listing is usable
	 */
	public static String format(final String scriptList, final String imageList) {
		return format(scriptList, imageList, null);
	}

	/**
	 * Formats the snapshot from listing results and conversation state.
	 *
	 * @param scriptList result of fiji_script_list, or null if unavailable
	 * @param imageList result of fiji_image_list, or null if unavailable
	 * @param conversationStats conversation status such as {@code unnamed} or
	 *          {@code named}, or null when unavailable
	 * @return the snapshot, or an empty string if no information is usable
	 */
	public static String format(final String scriptList, final String imageList,
		final String conversationStats)
	{
		final JsonObject scripts = parse(scriptList);
		final JsonObject images = parse(imageList);
		if (scripts == null && images == null && (conversationStats == null ||
			conversationStats.isBlank())) return "";
		final StringBuilder sb = new StringBuilder(HEADER).append("\n");
		if (conversationStats != null && !conversationStats.isBlank()) sb.append(
			"conversation_stats: ").append(conversationStats).append("\n");
		if (scripts != null) sb.append("fiji_script_list: ").append(scripts).append(
			"\n");
		if (images != null) sb.append("fiji_image_list: ").append(capImages(images))
			.append("\n");
		return sb.append(FOOTER).toString();
	}

	/** Parses a listing, or returns null if it is missing or an error. */
	private static JsonObject parse(final String json) {
		if (json == null) return null;
		try {
			final JsonElement element = JsonParser.parseString(json);
			if (!element.isJsonObject() || element.getAsJsonObject().has("error")) {
				return null;
			}
			return element.getAsJsonObject();
		}
		catch (final JsonParseException e) {
			return null;
		}
	}

	private static JsonObject capImages(final JsonObject images) {
		final JsonElement list = images.get("open_images");
		if (list == null || !list.isJsonArray() || list.getAsJsonArray()
			.size() <= MAX_IMAGES) return images;

		final JsonArray all = list.getAsJsonArray();
		final JsonArray kept = new JsonArray();
		for (final JsonElement image : all) {
			if (isActive(image)) kept.add(image);
		}
		for (final JsonElement image : all) {
			if (kept.size() >= MAX_IMAGES) break;
			if (!isActive(image)) kept.add(image);
		}
		final JsonObject capped = images.deepCopy();
		capped.add("open_images", kept);
		capped.addProperty("omitted_images", all.size() - kept.size());
		return capped;
	}

	private static boolean isActive(final JsonElement image) {
		return image.isJsonObject() && image.getAsJsonObject().has("active") &&
			image.getAsJsonObject().get("active").getAsBoolean();
	}
}
