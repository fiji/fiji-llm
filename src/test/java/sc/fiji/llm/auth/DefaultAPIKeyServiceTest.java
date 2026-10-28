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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.scijava.Context;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.module.Module;
import org.scijava.module.ModuleService;
import org.scijava.prefs.PrefService;

import sc.fiji.llm.Setup;

public class DefaultAPIKeyServiceTest {

	@ClassRule
	public static final TemporaryFolder FOLDER = new TemporaryFolder();

	private static final String LEGACY_KEY_PREFIX = "fiji.llm.apikey.";
	private static final String LEGACY_ENCRYPTION_KEY =
		"fiji.llm.encryption.key";

	private static Context context;
	private PrefService prefService;
	private DefaultAPIKeyService service;
	private File vaultFile;

	@BeforeClass
	public static void setUpContext() {
		context = Setup.context();
	}

	@AfterClass
	public static void tearDownContext() {
		Setup.dispose(context);
	}

	@Before
	public void setUp() throws Exception {
		prefService = context.getService(PrefService.class);
		prefService.clear(LegacyPreferenceScope.class);
		vaultFile = new File(FOLDER.newFolder(), "api-keys.kdbx");
		service = new DefaultAPIKeyService(prefService, vaultFile,
			LegacyPreferenceScope.class);
	}

	@After
	public void tearDown() {
		service.lock();
		prefService.clear(LegacyPreferenceScope.class);
	}

	@Test
	public void testVaultRoundTripAndLocking() throws Exception {
		final char[] masterPassword = "vault master password".toCharArray();
		service.unlock(masterPassword);
		Arrays.fill(masterPassword, '\0');
		assertTrue(service.isUnlocked());
		assertTrue(service.isVaultInitialized());

		final String secret = "api-key-value-not-in-file";
		final char[] apiKey = secret.toCharArray();
		service.setApiKey("Anthropic", apiKey);
		Arrays.fill(apiKey, '\0');

		assertTrue(service.hasApiKey("Anthropic"));
		assertEquals(secret, service.getApiKey("Anthropic"));
		assertFalse(new String(Files.readAllBytes(vaultFile.toPath()), StandardCharsets
			.ISO_8859_1).contains(secret));

		service.lock();
		assertFalse(service.isUnlocked());
		try {
			service.hasApiKey("Anthropic");
			fail("Expected locked vault access to fail");
		}
		catch (IllegalStateException expected) {
			assertTrue(expected.getMessage().contains("locked"));
		}

		final DefaultAPIKeyService wrongPasswordService = newService();
		try {
			wrongPasswordService.unlock("incorrect password".toCharArray());
			fail("Expected incorrect master password to fail");
		}
		catch (IllegalStateException expected) {
			assertFalse(wrongPasswordService.isUnlocked());
		}

		final DefaultAPIKeyService reloadedService = newService();
		final char[] reloadPassword = "vault master password".toCharArray();
		reloadedService.unlock(reloadPassword);
		Arrays.fill(reloadPassword, '\0');
		assertEquals(secret, reloadedService.getApiKey("Anthropic"));
		reloadedService.lock();
	}

	@Test
	public void testMigratesLegacyPreferenceOnlyAfterVaultVerification()
		throws Exception
	{
		final String secret = "legacy-api-key-to-migrate";
		final SecretKey encryptionKey = KeyGenerator.getInstance("AES")
			.generateKey();
		final Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
		final byte[] encrypted = cipher.doFinal(secret.getBytes(StandardCharsets.UTF_8));
		prefService.put(LegacyPreferenceScope.class, LEGACY_ENCRYPTION_KEY,
			Base64.getEncoder().encodeToString(encryptionKey.getEncoded()));
		prefService.put(LegacyPreferenceScope.class, LEGACY_KEY_PREFIX + "Anthropic",
			Base64.getEncoder().encodeToString(encrypted));

		final char[] masterPassword = "new vault password".toCharArray();
		service.unlock(masterPassword);
		Arrays.fill(masterPassword, '\0');

		assertEquals(secret, service.getApiKey("Anthropic"));
		assertNull(prefService.get(LegacyPreferenceScope.class, LEGACY_KEY_PREFIX +
			"Anthropic"));
		assertNull(prefService.get(LegacyPreferenceScope.class,
			LEGACY_ENCRYPTION_KEY));
		assertTrue(vaultFile.isFile());
	}

	@Test
	public void testKeepsVaultValueWhenLegacyPreferenceRemainsAfterCrash()
		throws Exception
	{
		final char[] initialPassword = "vault password".toCharArray();
		service.unlock(initialPassword);
		Arrays.fill(initialPassword, '\0');
		final char[] currentKey = "current-vault-key".toCharArray();
		service.setApiKey("Anthropic", currentKey);
		Arrays.fill(currentKey, '\0');
		service.lock();

		storeLegacyKey("stale-preference-key");
		final char[] unlockPassword = "vault password".toCharArray();
		service.unlock(unlockPassword);
		Arrays.fill(unlockPassword, '\0');

		assertEquals("current-vault-key", service.getApiKey("Anthropic"));
		assertNull(prefService.get(LegacyPreferenceScope.class, LEGACY_KEY_PREFIX +
			"Anthropic"));
	}

	@Test
	public void testPasswordInputDynamicCommandRunsThroughModuleService()
		throws Exception
	{
		final CommandInfo commandInfo = context.getService(CommandService.class)
			.getCommand(VaultPasswordInputCommand.class);
		final Map<String, Object> inputs = Map.of("creatingVault", true,
			"masterPassword", "test-password", "confirmation", "test-password");
		final Module module = context.getService(ModuleService.class).run(commandInfo,
			false, inputs).get();

		assertEquals("test-password", module.getInput("masterPassword"));
		module.setInput("masterPassword", "");
		module.setInput("confirmation", "");
	}

	private DefaultAPIKeyService newService() throws Exception {
		return new DefaultAPIKeyService(prefService, vaultFile,
			LegacyPreferenceScope.class);
	}

	private void storeLegacyKey(final String secret) throws GeneralSecurityException {
		final SecretKey encryptionKey = KeyGenerator.getInstance("AES")
			.generateKey();
		final Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
		final byte[] encrypted = cipher.doFinal(secret.getBytes(StandardCharsets.UTF_8));
		prefService.put(LegacyPreferenceScope.class, LEGACY_ENCRYPTION_KEY,
			Base64.getEncoder().encodeToString(encryptionKey.getEncoded()));
		prefService.put(LegacyPreferenceScope.class, LEGACY_KEY_PREFIX + "Anthropic",
			Base64.getEncoder().encodeToString(encrypted));
	}

	private static final class LegacyPreferenceScope {}
}
