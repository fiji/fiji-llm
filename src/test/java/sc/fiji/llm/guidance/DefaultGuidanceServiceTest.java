/*-
 * #%L
 * Fiji software for LLM integration.
 * %%
 * Copyright (C) 2025 - 2026 ImageJ2 Developers
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;

public class DefaultGuidanceServiceTest {

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

	public void listsCanonicalUnmodifiableCategories() {

		final AgentGuidanceService service = service();



		final List<String> categories = service.getAvailableTopics();



		assertTrue(categories.contains("macros"));

		assertTrue(categories.contains("onboarding"));

		assertTrue(categories.contains("workflows"));

		assertEquals("application", categories.get(0));

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
	public void readsWithAnEnforcedBound() {
		final AgentGuidanceService service = service();

		final Optional<String> result = service.read(
			"environment-inspection", 20);

		assertTrue(result.isPresent());
		assertEquals(20, result.get().length());
	}

	@Test
	public void readsRuntimeBaselineGuides() {
		final AgentGuidanceService service = service();

		assertTrue(service.read("integrated-chat", 1000).get().contains(
			"chatbot embedded in Fiji"));
		assertTrue(service.read("mcp-server", 1000).get().contains(
			"live Fiji/ImageJ image analysis application"));
	}

	@Test
	public void returnsEmptyForUnknownDocument() {
		final AgentGuidanceService service = service();

		assertFalse(service.read("missing", 100).isPresent());
	}

	private AgentGuidanceService service() {
		return context.getService(AgentGuidanceService.class);
	}
}
