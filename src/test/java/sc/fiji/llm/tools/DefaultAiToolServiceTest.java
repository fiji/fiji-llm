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

package sc.fiji.llm.tools;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;

public class DefaultAiToolServiceTest {

	private Context context;

	@Before
	public void setUp() {
		context = new Context();
	}

	@After
	public void tearDown() {
		context.dispose();
	}

	/**
	 * Without {@code @P(name = ...)}, LangChain4j names parameters after the
	 * Java parameters, which are {@code arg0}, {@code arg1}, etc. unless
	 * compiled with {@code -parameters}.
	 */
	@Test
	public void testToolParametersHaveSnakeCaseNames() {
		final AiToolService service = context.getService(AiToolService.class);
		final List<String> badNames = new ArrayList<>();
		for (final ToolSpecification spec : service.getToolsWithExecutors()
			.keySet())
		{
			if (spec.parameters() == null) continue;
			for (final String param : spec.parameters().properties().keySet()) {
				if (!param.matches("[a-z][a-z0-9]*(_[a-z0-9]+)*") || param.matches(
					"arg[0-9]+"))
				{
					badNames.add(spec.name() + "." + param);
				}
			}
		}
		assertTrue("Tool parameters need @P(name = ...): " + badNames, badNames
			.isEmpty());
	}

	@Test
	public void testErrorMessageReportsMissingParameters() {
		final String message = DefaultAiToolService.errorMessage("sample_tool",
			spec(), "{\"end_line\": 5}", new NullPointerException());
		assertTrue(message, message.contains("missing required parameter(s): start_line."));
		assertTrue(message, message.contains("start_line (required)"));
		assertTrue(message, message.contains("verbose"));
	}

	@Test
	public void testErrorMessageReportsUnexpectedParameters() {
		final String message = DefaultAiToolService.errorMessage("sample_tool",
			spec(), "{\"start_line\": 1, \"end_line\": 5, \"line\": 2}",
			new NullPointerException());
		assertTrue(message, message.contains("unexpected parameter(s): line."));
		assertFalse(message, message.contains("missing"));
	}

	@Test
	public void testErrorMessageReportsInvalidJson() {
		final String message = DefaultAiToolService.errorMessage("sample_tool",
			spec(), "{start_line", new IllegalArgumentException());
		assertTrue(message, message.contains("not a valid JSON object"));
	}

	@Test
	public void testErrorMessageReportsExecutionFailure() {
		final String message = DefaultAiToolService.errorMessage("sample_tool",
			spec(), "{\"start_line\": 1, \"end_line\": 5}", new RuntimeException(
				"wrapper", new IllegalStateException("no active script")));
		assertTrue(message, message.contains(
			"IllegalStateException: no active script"));
	}

	private static ToolSpecification spec() {
		for (final var method : DefaultAiToolServiceTest.class
			.getDeclaredMethods())
		{
			if (method.isAnnotationPresent(Tool.class)) {
				return ToolSpecifications.toolSpecificationFrom(method);
			}
		}
		assertNotNull("No sample tool", null);
		return null;
	}

	@Tool(name = "sample_tool")
	static String sampleTool(@P(name = "start_line", value = "First line") final int startLine,
		@P(name = "end_line", value = "Last line") final int endLine,
		@P(name = "verbose", value = "Verbose output", required = false) final Boolean verbose)
	{
		return "";
	}
}
