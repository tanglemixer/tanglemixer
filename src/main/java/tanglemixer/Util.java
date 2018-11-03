package tanglemixer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import com.securityinnovation.jNeo.ntruencrypt.NtruEncryptKey;

import jota.pow.pearldiver.PearlDiverLocalPoW;
import jota.IotaAPI;
import jota.dto.response.GetNewAddressResponse;
import jota.dto.response.GetNodeInfoResponse;
import jota.error.ArgumentException;
import jota.utils.TrytesConverter;

public class Util {

	public static String FIELD_DELIMITER = Messages.getString("Util.fieldDelimiter");
	public static String SUB_FIELD_DELIMITER = Messages.getString("Util.subFieldDelimiter");

	public static String TANGLE_MIXER_ADDRESS = Messages.getString("Util.tangleMixerAddress"); //$NON-NLS-1$
	private static String PUBLIC_KEY = Messages.getString("Util.publicKey");

	public static String trytesToString(String trytes) {
		String result;
		if (trytes.length() % 2 != 0) {
			result = TrytesConverter.trytesToAscii(trytes.substring(0, trytes.length() - 1)).trim();

		} else {
			result = TrytesConverter.trytesToAscii(trytes).trim();
		}
		return result;
	}

	public static String stringToTrytes(String string) {
		return TrytesConverter.asciiToTrytes(string);
	}

	public static boolean testNode(IotaAPI api) {
		try {
			GetNodeInfoResponse response = api.getNodeInfo();
			response.getAppVersion();
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public static String generateSeed() throws NoSuchAlgorithmException {
		return generateRandomString(81);
	}

	private static String generateRandomString(int length) throws NoSuchAlgorithmException {
		String characters = Messages.getString("Util.seedCharacters");
		SecureRandom random = SecureRandom.getInstanceStrong();
		char[] buffer = new char[length];
		for (int i = 0; i < length; ++i) {
			buffer[i] = characters.toCharArray()[random.nextInt(characters.length())];
		}
		return new String(buffer);
	}

	public static String getIotaAddress(IotaAPI api, String seed, int index, boolean withChecksum) throws ArgumentException {
		GetNewAddressResponse addresses = api.generateNewAddresses(seed, 2, withChecksum, 1);
		return addresses.first();
	}

	public static IotaAPI connect(String connectString) throws MalformedURLException {

		URL url = new URL(connectString);
		String protocol = url.getProtocol();
		String host = url.getHost();
		String port = String.valueOf(url.getPort());

		return new IotaAPI.Builder().protocol(protocol).host(host).port(port).localPoW(new PearlDiverLocalPoW()).build();
	}

	public static NtruEncryptKey getPublicServerKey() {

		NtruEncryptKey ntruKey = null;
		try {
			byte[] decodedKey = Base64.getDecoder().decode(PUBLIC_KEY);

			ntruKey = new NtruEncryptKey(decodedKey);

		} catch (Exception e) {
			System.err.println(Messages.getString("Util.errPublicKey"));
			e.printStackTrace();
			System.exit(8);

		}
		return ntruKey;

	}

	public static void convertPublicKey(String filename) {
		Path path = Paths.get(filename);
		byte[] fileContents = null;
		try {
			fileContents = Files.readAllBytes(path);
		} catch (IOException e) {
			System.out.println("Unable to open file " + filename + " : " + e.getMessage());
			System.exit(8);
		}
		System.out.println(new String(Base64.getEncoder().encode(fileContents)));
	}

}
