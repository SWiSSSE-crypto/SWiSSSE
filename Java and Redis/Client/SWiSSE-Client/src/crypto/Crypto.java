package crypto;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

public class Crypto {
	
	// Constants
	public static final int AES_KEY_SIZE = 128; // in bits
    public static final int GCM_NONCE_LENGTH = 12; // in bytes
    public static final int GCM_TAG_LENGTH = 16; // in bytes

    
    // Key generation
	public static SecretKey key_gen() throws GeneralSecurityException
	{
		SecureRandom random = SecureRandom.getInstanceStrong();
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(AES_KEY_SIZE, random);
		SecretKey key = keyGen.generateKey();
		return key;
	}
	
	
	// SHA1
	public static byte[] SHA1(SecretKeySpec signingKey, String address) throws GeneralSecurityException
	{
		try {
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(signingKey);
			return mac.doFinal(address.getBytes());
		} catch (NoSuchAlgorithmException e){
			e.printStackTrace();
		}
		return null;
	}
	
	
	// AES-GCM encryption
	public static byte[] GCM_encrypt(SecretKey key, SecureRandom random, String plaintext) throws GeneralSecurityException
	{
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "SunJCE");
	    final byte[] nonce = new byte[GCM_NONCE_LENGTH];
	    random.nextBytes(nonce);
	    GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, nonce);
	    cipher.init(Cipher.ENCRYPT_MODE, key, spec);
	    
	    byte[] ciphertext = cipher.doFinal(plaintext.getBytes());
	    byte[] nonce_and_ciphertext = new byte[nonce.length + ciphertext.length];
	    System.arraycopy(nonce, 0, nonce_and_ciphertext, 0, nonce.length);
	    System.arraycopy(ciphertext, 0, nonce_and_ciphertext, nonce.length, ciphertext.length);
	    cipher = null;
	    
		return nonce_and_ciphertext;
	}
	
	
	// AES-GCM decryption
	public static String GCM_decryption(SecretKey key, byte[] nonce_and_ciphertext) throws GeneralSecurityException
	{
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "SunJCE");
		byte[] nonce = Arrays.copyOfRange(nonce_and_ciphertext, 0, GCM_NONCE_LENGTH);
		byte[] ciphertext_bytes = Arrays.copyOfRange(nonce_and_ciphertext, GCM_NONCE_LENGTH, nonce_and_ciphertext.length);
		GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, nonce);
		cipher.init(Cipher.DECRYPT_MODE, key, spec);
        
		byte[] plaintext_bytes = cipher.doFinal(ciphertext_bytes);
		cipher = null;
		return new String(plaintext_bytes);
	}
	
}
