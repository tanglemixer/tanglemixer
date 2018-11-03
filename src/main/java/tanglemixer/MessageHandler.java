package tanglemixer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.securityinnovation.jNeo.NtruException;

import jota.IotaAPI;
import jota.dto.response.FindTransactionResponse;
import jota.dto.response.GetBundleResponse;
import jota.error.ArgumentException;
import jota.model.Transaction;
import jota.model.Transfer;
import jota.utils.Checksum;
import tanglemixer.ntru.NtruHandler;

public class MessageHandler {
	
	private NtruHandler ntru = null;
	private IotaAPI api = null;
	
	public MessageHandler(IotaAPI api, NtruHandler ntru) {
		this.api = api;
		this.ntru = ntru;
	}
	
	public String getMessage(String address) {
		while (true) {
			FindTransactionResponse response = null;
			try {
				Thread.sleep(1000);
				String[] responseAddress = new String[1];
				responseAddress[0] = Checksum.removeChecksum(address);
				response = api.findTransactions(responseAddress, null, null, null);

			} catch (ArgumentException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			String[] res = response.getHashes();
			for (String transaction : res) {
				try {
					GetBundleResponse bundle = api.getBundle(transaction);
					List<Transaction> transactions = bundle.getTransactions();
					StringBuilder encryptedMessage = new StringBuilder();
					for (Transaction t : transactions) {
						encryptedMessage.append(t.getSignatureFragments());
					}

					String result = ntru.decrypt(encryptedMessage.toString());

					if (result == null) {
						continue;
					}
					return result;
				} catch (ArgumentException e) {
					// dont care
				}
			}
		}
	}

	public boolean sendMessage(String message, String address) {
		String data = null;
		try {
			data = ntru.encrypt(message);
		} catch (NtruException | IOException e) {
			e.printStackTrace();
			return false;
		}
		
		DateTimeFormatter formatter = DateTimeFormatter.BASIC_ISO_DATE;
		List<Transfer> transfers = new ArrayList<>();
		Transfer trans = new Transfer(address, 0, data, Util.stringToTrytes(LocalDate.now().format(formatter)));
		transfers.add(trans);
		try {
			api.sendTransfer("", 2, 9, 14, transfers, null, null, false, false, null);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
		
	
}
