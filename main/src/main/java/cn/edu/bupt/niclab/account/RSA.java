package cn.edu.bupt.niclab.account;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import android.content.Context;
import android.util.Log;

public class RSA {

	public static final String PUBLIC_KEY_FILE = "pk.der";
	public static final String PRIVATE_KEY_FILE = "private_key.der";
	
	private static RSA mInstance;
	
	private PublicKey mPubKey;
	private PrivateKey mPvtKey;
	
	private RSA(){
	}
	
	public static RSA instance(){
		if (mInstance == null) {
			mInstance = new RSA();
		}
		return mInstance;
	}
	
	public PublicKey getPublicKey(Context context) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException{
		if (mPubKey == null) {
			InputStream ins = context.getAssets().open(PUBLIC_KEY_FILE);
			byte[] keyData = new byte[ins.available()];
			ins.read(keyData);
			ins.close();
			
			X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(keyData);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			mPubKey = kf.generatePublic(publicKeySpec);
		}
		return this.mPubKey;
	}
	
	public PrivateKey getPrivateKey(Context context) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException{
		if (mPvtKey == null) {
			InputStream ins = context.getAssets().open(PRIVATE_KEY_FILE);
			byte[] keyData = new byte[ins.available()];
			ins.read(keyData);
			ins.close();
			
			PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(keyData);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			mPvtKey = kf.generatePrivate(privateKeySpec);
		}
		return mPvtKey;
	}
	
	public byte[] encryptWithPrivateKey(Context context, String origin) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException, IOException, IllegalBlockSizeException, BadPaddingException{
		
		Cipher pkCipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
		pkCipher.init(Cipher.ENCRYPT_MODE, getPrivateKey(context));
		long before = System.nanoTime();
		byte[] encrypedInByte = pkCipher.doFinal(origin.getBytes());
		long duration = System.nanoTime() - before;
		Log.d("RSAHelper", "encryptWithPrivateKey use " + duration);
		return encrypedInByte;
	}
	
	public String decryptWithPrivateKey(Context context, byte[] data) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException, IOException, IllegalBlockSizeException, BadPaddingException{
		Cipher pkcCipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
		pkcCipher.init(Cipher.DECRYPT_MODE, getPrivateKey(context));
		long before = System.nanoTime();
		byte[] out = pkcCipher.doFinal(data);
		long duration = System.nanoTime() - before;
		Log.d("RSAHelper", "decryptWithPrivateKey use " + duration);
		return new String(out);
	}
	
	public byte[] encryptWithPublicKey(Context context, String origin) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException, IOException, IllegalBlockSizeException, BadPaddingException{
		
		Cipher pkCipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
		pkCipher.init(Cipher.ENCRYPT_MODE, getPublicKey(context));

		long before = System.nanoTime();
		byte[] encrypedInByte = pkCipher.doFinal(origin.getBytes());
		long duration = System.nanoTime() - before;
		Log.d("RSAHelper", "encryptWithPublicKey use " + duration);
		
		return encrypedInByte;
	}
	
	public String decryptWithPublicKey(Context context, byte[] data) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException, IOException, IllegalBlockSizeException, BadPaddingException{
		Cipher pkcCipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
		pkcCipher.init(Cipher.DECRYPT_MODE, getPublicKey(context));
		long before = System.nanoTime();
		byte[] out = pkcCipher.doFinal(data);
		long duration = System.nanoTime() - before;
		Log.d("RSAHelper", "decryptWithPublicKey use " + duration);
		return new String(out);
	}
	
}
