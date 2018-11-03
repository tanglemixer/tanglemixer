package tanglemixer.client;

import tanglemixer.MessageHandler;
import tanglemixer.Util;
import tanglemixer.ntru.NtruHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

import com.securityinnovation.jNeo.NtruException;

import jota.IotaAPI;
import jota.error.ArgumentException;
import jota.utils.InputValidator;

public class TangleMixer {

	private static final int REQUIRED_JAVA_MAJOR_VERSION = 8;
	private static final int REQUIRED_JAVA_MINOR_VERSION = 151;
	public static final String VERSION = "1.0.0";

	public static void main(String[] args) throws IOException {
		if (!isJavaVersionOk()) {
			System.out.println("Your Java version is too old.");
			System.out.println("TangleMixer requires at least jre 1.8.0_151 installed.");
			System.out.println("Your version: " + System.getProperty("java.version"));
			System.exit(2);
		}
		Security.setProperty("crypto.policy", "unlimited");

		if (args.length == 2) {
			if (args[0].equals("setupPublicKey")) {
				Util.convertPublicKey(args[1]);
				System.exit(0);
			}
		}
		
		interactiveMode();

	}

	private static void interactiveMode() throws IOException {
		
		System.out.println("Welcome to the TangleMixer, version " + VERSION + "!");
		System.out.println();
		System.out.println("TangleMixer will NEVER ask you for your seed. You have to transfer the funds on your own.");
		System.out.println("Please complete the following steps in order to use TangleMixer.");
		System.out.println("You can cancel the process at any time with ctrl+c");
		System.out.println();
		
		ArrayList<String> returnAddresses = new ArrayList<String>();

		System.out.println("Please enter the addresses where you want to receive your IOTAs. The addresses must include the checksum (=90 characters)");
		System.out.println("Ensure that you own the seed to the corresponding addresses or you will lose your IOTAs.");
		System.out.println("Your balance will be randomly split across the given addresses.");
		System.out.println("Minimum two addresses, maximium ten. One address per line. Finish with an empty line.");
		System.out.print("> ");
		Scanner stdin = new Scanner(System.in);
		boolean stop = false;
		while (stdin.hasNextLine() && !stop) {
		
			if (returnAddresses.size() == 10) {
				stop = true;
				break;
			}
			String userInput = stdin.nextLine();
			if (userInput.isEmpty() && returnAddresses.size() >= 2) {
				stop = true;
				break;
			}
			
			if (userInput.length() != 90) {
				System.out.println("You entered an invalid address");
				System.out.println("Your IOTA address must consist of 90 characters. Please try again");
				System.out.print("> ");
				continue;
			}
			
			if (returnAddresses.contains(userInput)) {
				System.out.println("You already entered this address, please enter a different address");
				System.out.print("> ");
				continue;
			}
			
			try {
				if (InputValidator.checkAddress(userInput)) {
					returnAddresses.add(userInput);
					if (returnAddresses.size() == 10) {
						break;
					}
					if (returnAddresses.size() == 1) {
						System.out.println("OK, next address please");

					} else {
						System.out.println("OK, next one, or press enter to finish");

					}
					System.out.print("> ");
				} else {
					System.out.println("You entered an invalid address, please try again");
					System.out.print("> ");
					continue;
				}
			} catch (ArgumentException e) {
				System.out.println("You entered an invalid address, please try again");
				System.out.print("> ");
				continue;
			}

		}
		
		IotaAPI api = null;
		while (true) {
			System.out.println("Please enter an IOTA node URL in the form of http(s)://host:port");
			System.out.print("> ");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String nodeUrl = br.readLine();

			try {
				api = Util.connect(nodeUrl);
			} catch (Exception m) {
				System.out.println("The URL you provded is invalid, please try again.");
				continue;
			}
			System.out.print("Testing the node... ");
			if (!Util.testNode(api)) {
				System.out.println();
				System.out.println("The node seems to be unavailable, please try a different one.");
				continue;
			}
			System.out.println("OK");
			break;
		}
		
		System.out.print("Setting up encryption... ");
		NtruHandler ntruClient = null;
		NtruHandler ntruServer = null;
		String publicKey = null;
		try {
			ntruClient = new NtruHandler();
			publicKey = ntruClient.getPublicKey();
			ntruServer = new NtruHandler(Util.getPublicServerKey());
		} catch (NtruException e) {
			System.out.println();
			System.out.println("Something went wrong. Encryption setup failed: " + e.getLocalizedMessage());
			e.printStackTrace();
			System.out.println("Exiting");
			System.exit(1);
		}
		System.out.println("OK");

		System.out.print("Preparing personal message receiver address... ");
		String address = null;
		try {
			address = Util.getIotaAddress(api, Util.generateSeed(), 0, true);
		} catch (ArgumentException | NoSuchAlgorithmException e) {
			System.out.println();
			System.out.println("Something went wrong setting up the IOTA address: " + e.getLocalizedMessage());
			e.printStackTrace();
			System.out.println("Exiting");
			System.exit(1);
		}
		System.out.println("OK");

		System.out.print("Sending encrypted HELLO message into the tangle, this might take a while... ");
		StringBuilder helloMessage = new StringBuilder();
		helloMessage.append(address).append(Util.FIELD_DELIMITER);
		helloMessage.append(publicKey).append(Util.FIELD_DELIMITER);
		Iterator<String> it = returnAddresses.iterator();
		while (it.hasNext()) {
			helloMessage.append(it.next());
			if (it.hasNext()) {
				helloMessage.append(Util.SUB_FIELD_DELIMITER);
			}
		}
		
		MessageHandler messageHandlerServer = new MessageHandler(api, ntruServer);
		try {
			messageHandlerServer.sendMessage(helloMessage.toString(), Util.TANGLE_MIXER_ADDRESS);
		} catch (Exception e) {
			System.out.println();
			System.out.println("Something went wrong while sending the HELLO message: " + e.getLocalizedMessage());
			e.printStackTrace();
			System.out.println("Please try again later.");
			System.exit(1);
		}
		System.out.println(" OK");
		
		
		System.out.print("Waiting for answer from the tangle. Depending on the TangelMixer system status and the tangle speed this might take a while... ");

		MessageHandler messageHandlerClient = new MessageHandler(api, ntruClient);
		String message = null;
		try {
			message = messageHandlerClient.getMessage(address);
		} catch (Exception e) {
			System.out.println();
			System.out.println("Something went wrong while receiving your TangleMixer details from the tangle: " + e.getLocalizedMessage());
			e.printStackTrace();
			System.out.println("Please try again later.");
			System.exit(1);
		}
		
		System.out.println("OK");
		System.out.println("Message from TangleMixer:");
		message = message.replaceAll("\n", System.lineSeparator());
		System.out.println(message);
		stdin.close();

	}

	private static boolean isJavaVersionOk() {
		String fullVersion = System.getProperty("java.version");
		int majorVersion = Integer.parseInt(String.valueOf(fullVersion.charAt(2)));
		if (majorVersion < REQUIRED_JAVA_MAJOR_VERSION) {
			return false;
		} else if (majorVersion > REQUIRED_JAVA_MAJOR_VERSION) {
			return true;
		} else {
			try {
				int minorVersion = Integer.parseInt(fullVersion.substring(fullVersion.lastIndexOf("_") + 1));
				return (minorVersion >= REQUIRED_JAVA_MINOR_VERSION);
			} catch (NumberFormatException e) {
				return false;
			}

		}
	}

}
