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

package sc.fiji.llm.auth;

import java.util.Objects;

import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.TextWidget;

/** Collects a vault master password through SciJava's UI or command inputs. */
@Plugin(type = Command.class, name = "API Key Vault Password",
	label = "Unlock API Key Vault", visible = false)
public class VaultPasswordInputCommand extends DynamicCommand {

	@Parameter(visibility = ItemVisibility.INVISIBLE, required = false,
		persist = false)
	private boolean creatingVault;

	@Parameter(visibility = ItemVisibility.INVISIBLE, required = false,
		persist = false)
	private String statusMessage = "";

	@Parameter(label = "", visibility = ItemVisibility.MESSAGE, required = false,
		persist = false)
	private String instructions = "";

	@Parameter(label = "Master Password", style = TextWidget.PASSWORD_STYLE,
		persist = false)
	private String masterPassword = "";

	@Parameter(label = "Confirm Master Password",
		style = TextWidget.PASSWORD_STYLE, persist = false)
	private String confirmation = "";

	@Override
	public void initialize() {
		instructions = creatingVault ?
			"<html><body style='width: 300px'><p>Create a master password to protect the API key vault. You will need it each time Fiji starts.</p>" +
			statusMessage + "</body></html>" :
			"<html><body style='width: 300px'><p>Enter the master password to unlock the API key vault.</p>" +
			statusMessage + "</body></html>";
		if (!creatingVault) resolveInput("confirmation");
	}

	@Override
	public void run() {
		if (masterPassword == null || masterPassword.isBlank()) {
			cancel("Master password must not be empty");
		}
		else if (creatingVault && !Objects.equals(masterPassword, confirmation)) {
			cancel("Master passwords do not match");
		}
		confirmation = "";
	}
}
