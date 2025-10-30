package sc.fiji.llm.service;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

/**
 * Default implementation of APIKeyService using encrypted preferences storage.
 * 
 * Note: This is a basic implementation. For production use, consider:
 * - Platform-specific credential storage (Keychain on macOS, Credential Manager on Windows)
 * - Master password protection
 * - More robust encryption
 */
@Plugin(type = Service.class)
public class DefaultAPIKeyService extends AbstractService implements APIKeyService {

	private static final String PREF_KEY_PREFIX = "fiji.llm.apikey.";
	private static final String ENCRYPTION_KEY_PREF = "fiji.llm.encryption.key";
	private static final String ALGORITHM = "AES";

	@Parameter
	private PrefService prefService;

	private SecretKey encryptionKey;

	@Override
	public void initialize() {
		super.initialize();
		// Initialize or load encryption key
		encryptionKey = loadOrCreateEncryptionKey();
	}

	@Override
	public String getApiKey(final String providerName) {
		final String encrypted = prefService.get(getClass(), PREF_KEY_PREFIX + providerName);
		if (encrypted == null) {
			return null;
		}
		return decrypt(encrypted);
	}

	@Override
	public void setApiKey(final String providerName, final String apiKey) {
		final String encrypted = encrypt(apiKey);
		prefService.put(getClass(), PREF_KEY_PREFIX + providerName, encrypted);
	}

	@Override
	public boolean hasApiKey(final String providerName) {
		return prefService.get(getClass(), PREF_KEY_PREFIX + providerName) != null;
	}

	@Override
	public void removeApiKey(final String providerName) {
		prefService.remove(getClass(), PREF_KEY_PREFIX + providerName);
	}

	@Override
	public boolean validateApiKey(final String providerName, final String apiKey) {
		// TODO: Implement actual validation by making a test API call
		// For now, just check if the key is not empty
		return apiKey != null && !apiKey.trim().isEmpty();
	}

	// Encryption helpers

	private SecretKey loadOrCreateEncryptionKey() {
		final String keyString = prefService.get(getClass(), ENCRYPTION_KEY_PREF);
		if (keyString != null) {
			final byte[] decodedKey = Base64.getDecoder().decode(keyString);
			return new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);
		}
		else {
			// Generate new key
			try {
				final KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
				keyGen.init(256);
				final SecretKey key = keyGen.generateKey();
				// Store it
				final String encoded = Base64.getEncoder().encodeToString(key.getEncoded());
				prefService.put(getClass(), ENCRYPTION_KEY_PREF, encoded);
				return key;
			}
			catch (NoSuchAlgorithmException e) {
				throw new RuntimeException("Failed to generate encryption key", e);
			}
		}
	}

	private String encrypt(final String plainText) {
		try {
			final Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
			final byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(encrypted);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to encrypt API key", e);
		}
	}

	private String decrypt(final String encryptedText) {
		try {
			final Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, encryptionKey);
			final byte[] decoded = Base64.getDecoder().decode(encryptedText);
			final byte[] decrypted = cipher.doFinal(decoded);
			return new String(decrypted, StandardCharsets.UTF_8);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to decrypt API key", e);
		}
	}
}
