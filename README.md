# TangleMixer
TangleMixer is a coin-mixing service for IOTA. With TangleMixer you can anonymize the source of your IOTAs.

# How does it work
Unlike other coin-mixing services, TangleMixer purely uses the IOTA tangle to communicate. There's no central website.
Every message is quantum proof encrypted with [NTRU](https://github.com/NTRUOpenSourceProject).

Steps involved:
1. TangleMixer client sends the user submitted information encrypted to the TangleMixer address. Only the TangleMixer service is able to decrypt this message.
    user information contain: user addresses, user's ntru public key, user's communication address
2. The TangleMixer backend decrypts the message, and responds to the given address with an encrypted message using the public key sent by the user
3. The TangleMixer client receives the encrypted messages and decrypts it
4. The message is printed, the TangelMixer client ends here. The message contains the deposit address where the user should send the IOTAs.
5. The User sends IOTAs to the given address
6. TangleMixer service sends the IOTAs back to the given addresses. The TangleMixer service charges a small fee. The fee is presented to the user in step 4.

# How to get started
Get a copy of the [TangleMixer client](https://github.com/tanglemixer/tanglemixer/releases) to get started.

The TangleMxier client is written in java. Please ensure that you have java installed. Minimum verison is 1.8.0_151.

Start the program with:

```
java -jar tanglemixer-1.0.0.jar

```

The TangleMixer client guides you through the setup process.

- Enter two to ten addresses where you receive your mixed funds
- Enter an IOTA node
- The TangleMixer client sends a message into tangle and waits for a response from the TangleMixer service.
- Please carefully read the response.
- The response contains the address where you deposit your IOTAs to mix. 
- The response also tells you the current fees. Small amounts are FREE to test our service.

# Is this a scam?
No.

As there are fees involved we want to provide the best mixing service possible.
We hope that many users use our service, the fees help us to cover the costs.
If we would scam people we could close our service immediately. 
We believe in IOTA and want to make it even better with this service. 

The TangleMixer service is quiet new, if something goes wrong TangleMixer returns the original amount back to the first given address.
