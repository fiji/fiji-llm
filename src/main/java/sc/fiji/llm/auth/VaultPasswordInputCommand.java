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

	private static final int INSTRUCTIONS_WIDTH = 300;

	@Parameter(visibility = ItemVisibility.INVISIBLE, required = false,
		persist = false)
	private boolean creatingVault;

	@Parameter(visibility = ItemVisibility.INVISIBLE, required = false,
		persist = false)
	private String statusMessage = "";

	@Parameter
	private APIKeyService apiKeyService;

	@Parameter(label = "", visibility = ItemVisibility.MESSAGE, required = false,
		persist = false)
	private String instructions = "";

	@Parameter(label = "Master Password", style = TextWidget.PASSWORD_STYLE,
		validater = "validateMasterPassword", persist = false)
	private String masterPassword = "";

	@Parameter(label = "Confirm Master Password",
		style = TextWidget.PASSWORD_STYLE, validater = "validateConfirmation",
		persist = false)
	private String confirmation = "";

	@Parameter(label = "Reset vault", callback = "resetVaultChanged",
		persist = false)
	private boolean resetVault;

	@Override
	public void initialize() {
		updateInstructions();
		if (!creatingVault) resolveInput("confirmation");
		else {
			resolveInput("resetVault");
			getInfo().getMutableInput("resetVault", Boolean.class)
				.setVisibility(ItemVisibility.INVISIBLE);
		}
	}

	protected void resetVaultChanged() {
		if (creatingVault) return;
		if (resetVault) {
			masterPassword = "";
			getInfo().getMutableInput("masterPassword", String.class).setValue(this,
				"");
		}
		updateInstructions();
	}

	@Override
	public void run() {
		if (resetVault && !creatingVault) {
			apiKeyService.resetVault();
			masterPassword = "";
			confirmation = "";
			return;
		}
		confirmation = "";
	}

	protected String validateMasterPassword() {
		if (resetVault && !creatingVault) return null;
		return masterPassword == null || masterPassword.isBlank() ?
			"Master password must not be empty" : null;
	}

	protected String validateConfirmation() {
		if (!creatingVault) return null;
		if (confirmation == null || confirmation.isBlank()) {
			return "Please confirm the master password";
		}
		return Objects.equals(masterPassword, confirmation) ? null :
			"Master passwords do not match";
	}

	private void updateInstructions() {
		final String prompt;
		if (creatingVault) {
			prompt = "Create a master password to protect the API key vault." +
				"<br><br>You will need it once per Fiji session.";
		}
		else if (resetVault) {
			prompt = "<b>This will permanently delete all saved API keys.</b>" +
				"<br><br>You will need to re-enter your API keys. Press OK to reset.";
		}
		else prompt = "Enter your master password to unlock the API key vault." +
				"<br><br>If you forgot your password, you can reset it now.";
		instructions = "<html><body style='width: " + INSTRUCTIONS_WIDTH +
			"px; margin: 0'><p style='margin-top: 0'>" + prompt + "</p>" +
			statusMessage + "</body></html>";
		getInfo().getMutableInput("instructions", String.class).setValue(this,
			instructions);
	}
}
