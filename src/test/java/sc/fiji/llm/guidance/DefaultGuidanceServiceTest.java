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
package sc.fiji.llm.guidance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;

public class DefaultGuidanceServiceTest {

	private static final Pattern FORMAT_PLACEHOLDER = Pattern.compile(
		"%(?:\\d+\\$)?s");

	private Context context;

	@Before
	public void setUp() {
		context = new Context();
	}

	@After
	public void tearDown() {
		context.dispose();
	}

	@Test
	public void listsMetadata() {
		final AgentGuidanceService service = service();

		final List<AgentGuideMetadata> documents = service.listDocuments();



		assertEquals(20, documents.size());

		assertTrue(documents.stream().anyMatch(document -> "creating-macros".equals(

			document.id())));

		assertTrue(documents.stream().anyMatch(document -> "data-types".equals(document.id())));
		assertTrue(documents.stream().anyMatch(document -> "integrated-chat".equals(document.id())));
		assertTrue(documents.stream().anyMatch(document -> "mcp-server".equals(document.id())));
		assertTrue(documents.stream().anyMatch(document -> "extension-contribution".equals(
			document.id())));

	}



	@Test
	public void convertsTopicsToCanonicalKeywords() {
		assertEquals(List.of("data-types", "print-stream"), AgentGuide.topics(
			AgentGuide.Topic.DATA_TYPES, AgentGuide.Topic.PRINT_STREAM));
	}

	@Test

	public void listsCanonicalUnmodifiableCategories() {

		final AgentGuidanceService service = service();



		final List<String> categories = service.getAvailableTopics();



		assertTrue(categories.contains("macros"));

		assertTrue(categories.contains("onboarding"));

		assertTrue(categories.contains("workflows"));

		assertEquals("agent", categories.get(0));

		assertEquals("workflows", categories.get(categories.size() - 1));

		try {

			categories.add("new-category");

			throw new AssertionError("Categories should be unmodifiable");

		}

		catch (final UnsupportedOperationException expected) {}

	}



	@Test

	public void searchesOneTopic() {

		final AgentGuidanceService service = service();



		final List<AgentGuideMetadata> results = service.search("MACROS");

		assertEquals(2, results.size());
		assertTrue(results.stream().anyMatch(document -> "creating-macros".equals(document.id())));
		assertTrue(results.stream().anyMatch(document -> "scripts-and-macros".equals(document.id())));
	}

	@Test
	public void blankTopicReturnsNoResults() {
		final AgentGuidanceService service = service();

		assertTrue(service.search("").isEmpty());
	}

	@Test
	public void readsRuntimeBaselineGuides() {
		final AgentGuidanceService service = service();

		assertTrue(service.read("integrated-chat").get().contains(
			"Fiji Chat brings approachable natural-language AI assistance into Fiji"));
		assertTrue(service.read("mcp-server").get().contains(
			"The Model Context Protocol (MCP) server is the standard connection point"));
	}

	@Test
	public void readsEveryGuideWithoutTruncationOrUnresolvedPlaceholders() {
		final AgentGuidanceService service = service();

		for (final AgentGuide guide : service.getInstances()) {
			final String id = guide.metadata().id();
			final Optional<String> result = service.read(id);

			assertTrue("Missing guide: " + id, result.isPresent());
			assertEquals("Guide was truncated: " + id, guide.content().length(), result
				.get().length());
			assertEquals("Guide content changed: " + id, guide.content(), result.get());
			assertFalse("Unresolved formatting placeholder in guide: " + id,
				FORMAT_PLACEHOLDER.matcher(result.get()).find());
		}
	}

	@Test
	public void returnsEmptyForUnknownDocument() {
		final AgentGuidanceService service = service();

		assertFalse(service.read("missing").isPresent());
	}

	private AgentGuidanceService service() {
		return context.getService(AgentGuidanceService.class);
	}
}
