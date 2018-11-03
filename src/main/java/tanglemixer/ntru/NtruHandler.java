package tanglemixer.ntru;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.securityinnovation.jNeo.NtruException;
import com.securityinnovation.jNeo.OID;
import com.securityinnovation.jNeo.ObjectClosedException;
import com.securityinnovation.jNeo.PlaintextBadLengthException;
import com.securityinnovation.jNeo.Random;
import com.securityinnovation.jNeo.ntruencrypt.NtruEncryptKey;

import tanglemixer.Util;

public class NtruHandler {

	public static final String NTRU_OID = "ees1499ep1";
	private NtruEncryptKey key;

	private OID oid;

	private Random createSeededRandom() {
		byte seed[] = new byte[32];
		java.util.Random sysRand = new java.security.SecureRandom();
		sysRand.nextBytes(seed);
		Random prng = new Random(seed);
		return prng;
	}

	public NtruHandler() throws NtruException {
		generateNewKey();
	}

	public NtruHandler(NtruEncryptKey key) throws NtruException {
		this.key = key;
	}

	private void generateNewKey() throws NtruException {
		oid = OID.valueOf(NTRU_OID);
		Random random = createSeededRandom();
		this.key = NtruEncryptKey.genKey(oid, random);

	}

	public String getPublicKey() throws ObjectClosedException {
		return new String(Base64.getEncoder().encode(key.getPubKey()));
	}

	public String encrypt(String data) throws ObjectClosedException, PlaintextBadLengthException, IOException {
		byte[] dataByte = data.getBytes();
		byte[] ivBytes = null;
		byte[] encryptedBuf = null;
		byte[] wrappedAESKey = null;
		try {
			Random prng = createSeededRandom();
			KeyGenerator keygen = KeyGenerator.getInstance("AES");
			keygen.init(256);
			SecretKey aesKey = keygen.generateKey();

			ivBytes = new byte[16];
			prng.read(ivBytes);
			IvParameterSpec iv = new IvParameterSpec(ivBytes);

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, aesKey, iv);
			encryptedBuf = cipher.doFinal(dataByte);
			java.util.Arrays.fill(dataByte, (byte) 0);

			byte aesKeyBytes[] = aesKey.getEncoded();
			wrappedAESKey = key.encrypt(aesKeyBytes, prng);
			java.util.Arrays.fill(aesKeyBytes, (byte) 0);

			ByteArrayOutputStream outByte = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(outByte);
			out.writeInt(ivBytes.length);
			out.write(ivBytes);
			out.writeInt(wrappedAESKey.length);
			out.write(wrappedAESKey);
			out.writeInt(encryptedBuf.length);
			out.write(encryptedBuf);
			out.close();
			outByte.close();
			
			return Util.stringToTrytes(new String(Base64.getEncoder().encode(outByte.toByteArray())));

		} catch (java.security.GeneralSecurityException e) {
			System.out.println("AES error: " + e); // TODO
		}
		return null;
	}

	public String decrypt(String encryptedData) {
		byte data[] = null;
		try {

			byte[] baseDecoded = Base64.getDecoder().decode(Util.trytesToString(encryptedData).getBytes());

			ByteArrayInputStream inByte = new ByteArrayInputStream(baseDecoded);
			DataInputStream in = new DataInputStream(inByte);
			byte ivBytes[] = new byte[in.readInt()];
			in.readFully(ivBytes);

			byte wrappedKey[] = new byte[in.readInt()];
			in.readFully(wrappedKey);

			byte encFileContents[] = new byte[in.readInt()];
			in.readFully(encFileContents);

			byte aesKeyBytes[] = key.decrypt(wrappedKey);
			SecretKeySpec aesKey = new SecretKeySpec(aesKeyBytes, "AES");
			java.util.Arrays.fill(aesKeyBytes, (byte) 0);

			IvParameterSpec iv = new IvParameterSpec(ivBytes);
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, aesKey, iv);
			data = cipher.doFinal(encFileContents);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return new String(data);
	}

}
