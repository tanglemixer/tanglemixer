package tanglemixer.ntru;

import com.securityinnovation.jNeo.ntruencrypt.NtruEncryptKey;

public class NtruKeyPair {
	
	private NtruEncryptKey publicKey;
	private NtruEncryptKey privateKey;
	
	public NtruKeyPair(NtruEncryptKey publicKey, NtruEncryptKey privateKey) {
		setPublicKey(publicKey);
		setPrivateKey(privateKey);
	}
	
	public NtruKeyPair(NtruEncryptKey publicKey) {
		setPublicKey(publicKey);
	}
	
	public NtruEncryptKey getPrivateKey() {
		return privateKey;
	}
	public void setPrivateKey(NtruEncryptKey privateKey) {
		this.privateKey = privateKey;
	}
	public NtruEncryptKey getPublicKey() {
		return publicKey;
	}
	public void setPublicKey(NtruEncryptKey publicKey) {
		this.publicKey = publicKey;
	}

}
