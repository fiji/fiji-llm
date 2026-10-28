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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.linguafranca.pwdb.Entry;
import org.linguafranca.pwdb.Group;
import org.linguafranca.pwdb.PropertyValue;
import org.linguafranca.pwdb.format.KdbxCredentials;
import org.linguafranca.pwdb.kdbx.jackson.KdbxDatabase;
import org.scijava.app.AppService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

/**
 * Default implementation of APIKeyService using a password-protected KDBX vault.
 */
@Plugin(type = Service.class)
public class DefaultAPIKeyService extends AbstractService implements
	APIKeyService
{

	private static final String PREF_KEY_PREFIX = "fiji.llm.apikey.";
	private static final String ENCRYPTION_KEY_PREF = "fiji.llm.encryption.key";
	private static final String VAULT_DIRECTORY = ".fiji-llm";
	private static final String VAULT_FILENAME = "api-keys.kdbx";
	private static final String LEGACY_CIPHER = "AES";

	@Parameter
	private PrefService prefService;

	@Parameter
	private AppService appService;

	private File vaultFile;
	private Class<?> legacyPreferenceClass;
	private KdbxDatabase database;
	private char[] masterPassword;

	public DefaultAPIKeyService() {}

	DefaultAPIKeyService(final PrefService prefService, final File vaultFile,
		final Class<?> legacyPreferenceClass)
	{
		this.prefService = prefService;
		this.vaultFile = vaultFile;
		this.legacyPreferenceClass = legacyPreferenceClass;
	}

	@Override
	public void initialize() {
		super.initialize();
		if (legacyPreferenceClass == null) legacyPreferenceClass = getClass();
		if (vaultFile == null) vaultFile = resolveVaultFile();
	}

	@Override
	public synchronized boolean isVaultInitialized() {
		return vaultFile.exists();
	}

	@Override
	public synchronized boolean isUnlocked() {
		return database != null;
	}

	@Override
	public synchronized void unlock(final char[] password) {
		if (password == null || password.length == 0) {
			throw new IllegalArgumentException("Master password must not be empty");
		}
		lock();
		masterPassword = Arrays.copyOf(password, password.length);
		try {
			database = vaultFile.exists() ? readVault() : new KdbxDatabase();
			migrateLegacyKeys();
			if (!vaultFile.exists()) saveVault();
		}
		catch (Exception e) {
			lock();
			throw new IllegalStateException("Unable to unlock or create API key vault",
				e);
		}
	}

	@Override
	public synchronized void lock() {
		database = null;
		if (masterPassword != null) {
			Arrays.fill(masterPassword, '\0');
			masterPassword = null;
		}
	}

	@Override
	public synchronized void resetVault() {
		lock();
		try {
			Files.deleteIfExists(vaultFile.toPath());
		}
		catch (IOException e) {
			throw new IllegalStateException("Unable to delete API key vault", e);
		}
		@SuppressWarnings("deprecation")
		final Map<String, String> legacyPreferences = prefService.getMap(
			legacyPreferenceClass);
		for (final String preferenceKey : legacyPreferences.keySet()) {
			if (preferenceKey.startsWith(PREF_KEY_PREFIX) || preferenceKey.equals(
				ENCRYPTION_KEY_PREF))
			{
				prefService.remove(legacyPreferenceClass, preferenceKey);
			}
		}
	}

	@Override
	public synchronized String getApiKey(final String providerName) {
		final Entry entry = findEntry(providerName);
		if (entry == null) return null;
		final PropertyValue password = entry.getPropertyValue(
			Entry.STANDARD_PROPERTY_NAME_PASSWORD);
		return password == null ? null : password.getValueAsString();
	}

	@Override
	public synchronized void setApiKey(final String providerName,
		final char[] apiKey)
	{
		requireUnlocked();
		if (providerName == null || providerName.isBlank()) {
			throw new IllegalArgumentException("Provider name must not be empty");
		}
		if (apiKey == null || apiKey.length == 0) {
			removeApiKey(providerName);
			return;
		}
		storeApiKey(providerName, apiKey);
		saveVault();
	}

	@Override
	public synchronized boolean hasApiKey(final String providerName) {
		return findEntry(providerName) != null;
	}

	@Override
	public synchronized void removeApiKey(final String providerName) {
		requireUnlocked();
		final Entry entry = findEntry(providerName);
		if (entry == null) return;
		database.getRootGroup().removeEntry(entry);
		saveVault();
	}

	@Override
	public boolean validateApiKey(final String providerName,
		final String apiKey)
	{
		return apiKey != null && !apiKey.trim().isEmpty();
	}

	@Override
	public synchronized void dispose() {
		lock();
		super.dispose();
	}

	private File resolveVaultFile() {
		final File appDirectory = appService.getApp() == null ? null : appService
			.getApp().getBaseDirectory();
		final File[] directories = { appDirectory == null ? null : new File(
			appDirectory, VAULT_DIRECTORY), new File(System.getProperty("user.home"),
			VAULT_DIRECTORY) };
		for (final File directory : directories) {
			if (directory == null) continue;
			try {
				Files.createDirectories(directory.toPath());
				if (Files.isDirectory(directory.toPath()) && Files.isWritable(directory
					.toPath())) return new File(directory, VAULT_FILENAME);
			}
			catch (IOException ignored) {
				// Try the user-home fallback.
			}
		}
		throw new IllegalStateException("Unable to create API key vault directory");
	}

	private void requireUnlocked() {
		if (database == null) {
			throw new IllegalStateException("API key vault is locked");
		}
	}

	private Entry findEntry(final String providerName) {
		requireUnlocked();
		if (providerName == null) return null;
		for (final Entry entry : database.getRootGroup().getEntries()) {
			if (providerName.equals(entry.getTitle())) return entry;
		}
		return null;
	}

	private void storeApiKey(final String providerName, final char[] apiKey) {
		final Group rootGroup = database.getRootGroup();
		final Entry existing = findEntry(providerName);
		if (existing != null) rootGroup.removeEntry(existing);
		final Entry entry = rootGroup.addEntry();
		entry.setTitle(providerName);
		entry.setPropertyValue(Entry.STANDARD_PROPERTY_NAME_PASSWORD, database
			.getPropertyValueStrategy().newProtected().of(apiKey));
	}

	private KdbxDatabase readVault() throws IOException {
		final byte[] passwordBytes = passwordBytes();
		final KdbxCredentials credentials;
		try {
			credentials = new KdbxCredentials(passwordBytes);
		}
		finally {
			Arrays.fill(passwordBytes, (byte) 0);
		}
		try (InputStream input = Files.newInputStream(vaultFile.toPath())) {
			return KdbxDatabase.read(credentials, input);
		}
		finally {
			Arrays.fill(credentials.getKey(), (byte) 0);
		}
	}

	private void saveVault() {
		requireUnlocked();
		Path temporaryFile = null;
		final byte[] passwordBytes = passwordBytes();
		final KdbxCredentials credentials;
		try {
			credentials = new KdbxCredentials(passwordBytes);
		}
		finally {
			Arrays.fill(passwordBytes, (byte) 0);
		}
		try {
			temporaryFile = Files.createTempFile(vaultFile.toPath().getParent(),
				"api-keys-", ".kdbx.tmp");
			try (OutputStream output = Files.newOutputStream(temporaryFile)) {
				database.write(credentials, output);
			}
			try {
				Files.move(temporaryFile, vaultFile.toPath(), StandardCopyOption
					.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			}
			catch (AtomicMoveNotSupportedException e) {
				Files.move(temporaryFile, vaultFile.toPath(), StandardCopyOption
					.REPLACE_EXISTING);
			}
		}
		catch (IOException e) {
			throw new IllegalStateException("Unable to save API key vault", e);
		}
		finally {
			Arrays.fill(credentials.getKey(), (byte) 0);
			if (temporaryFile != null) {
				try {
					Files.deleteIfExists(temporaryFile);
				}
				catch (IOException ignored) {
					// The temporary file is moved on success and best-effort deleted on failure.
				}
			}
		}
	}

	private byte[] passwordBytes() {
		final ByteBuffer encoded = StandardCharsets.UTF_8.encode(CharBuffer.wrap(
			masterPassword));
		final byte[] bytes = new byte[encoded.remaining()];
		encoded.get(bytes);
		if (encoded.hasArray()) Arrays.fill(encoded.array(), (byte) 0);
		return bytes;
	}

	private void migrateLegacyKeys() throws GeneralSecurityException, IOException {
		@SuppressWarnings("deprecation")
		final Map<String, String> legacyPreferences = prefService.getMap(
			legacyPreferenceClass);
		final String encodedKey = legacyPreferences.get(ENCRYPTION_KEY_PREF);
		boolean migratedAny = false;
		for (final Map.Entry<String, String> preference : legacyPreferences.entrySet()) {
			if (!preference.getKey().startsWith(PREF_KEY_PREFIX)) continue;
			if (encodedKey == null) {
				throw new IllegalStateException("Legacy API key encryption key is missing");
			}
			final String providerName = preference.getKey().substring(PREF_KEY_PREFIX
				.length());
			final char[] legacyKey = decryptLegacy(preference.getValue(), encodedKey);
			try {
				final boolean alreadyMigrated = findEntry(providerName) != null;
				if (!alreadyMigrated) {
					storeApiKey(providerName, legacyKey);
					saveVault();
				}
				final KdbxDatabase verifiedDatabase = readVault();
				final Entry migratedEntry = findEntry(verifiedDatabase, providerName);
				if (migratedEntry == null) {
					throw new IOException("Migrated API key was not present in the vault");
				}
				if (!alreadyMigrated) {
					final char[] verifiedKey = migratedEntry.getPropertyValue(Entry
						.STANDARD_PROPERTY_NAME_PASSWORD).getValueAsChars();
					try {
						if (!Arrays.equals(legacyKey, verifiedKey)) {
							throw new IOException("Migrated API key did not verify");
						}
					}
					finally {
						Arrays.fill(verifiedKey, '\0');
					}
				}
				database = verifiedDatabase;
				prefService.remove(legacyPreferenceClass, preference.getKey());
				migratedAny = true;
			}
			finally {
				Arrays.fill(legacyKey, '\0');
			}
		}
		if (encodedKey != null) prefService.remove(legacyPreferenceClass,
			ENCRYPTION_KEY_PREF);
		if (!vaultFile.exists() && !migratedAny) saveVault();
	}

	private Entry findEntry(final KdbxDatabase source, final String providerName) {
		for (final Entry entry : source.getRootGroup().getEntries()) {
			if (providerName.equals(entry.getTitle())) return entry;
		}
		return null;
	}

	private char[] decryptLegacy(final String encryptedText,
		final String encodedKey) throws GeneralSecurityException
	{
		final byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
		final byte[] encrypted = Base64.getDecoder().decode(encryptedText);
		final SecretKeySpec key = new SecretKeySpec(decodedKey, LEGACY_CIPHER);
		Arrays.fill(decodedKey, (byte) 0);
		final byte[] plaintext;
		try {
			final Cipher cipher = Cipher.getInstance(LEGACY_CIPHER);
			cipher.init(Cipher.DECRYPT_MODE, key);
			plaintext = cipher.doFinal(encrypted);
		}
		finally {
			Arrays.fill(encrypted, (byte) 0);
		}
		try {
			final CharBuffer decoded = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(
				plaintext));
			final char[] chars = new char[decoded.remaining()];
			decoded.get(chars);
			if (decoded.hasArray()) Arrays.fill(decoded.array(), '\0');
			return chars;
		}
		finally {
			Arrays.fill(plaintext, (byte) 0);
		}
	}
}
