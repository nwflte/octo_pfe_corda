# Instant Payments CorDapp

This Cordapp is a money transfer platform on corda. It leverages Corda features to allow rapid and parallel transactions.
Registering intra-bank and inter-bank payements with a regulatory body, making pledges and redeems.

A REST API webserver to expose endpoints for different operations and interactions with the nodes, and an UI client will be provided to interact with the backend.

## Components

### States
- `DDRObjectState`: It is an `OwnableState` and `FungibleState` that's a digital representation for real currency, the owner bank of the DDR Object should be able to make money transfers using them or redeem them.
- `DDRObligationState`: It is an `OwnableState` and `LinearState` that either reprensent a pledge from a commercial bank over currency deposited in central bank, or a redeem of issued DDR Objects. Issuing DDRs happens upon approving an obligation pledge request, and redeeming happens upon approving a redeem request.
- `InterBankTransferState`: Is a `LinearState` that represents a money transfer between two clients of different banks, describes the necessary informations to complete the transaction and which are required by regulatory.
- `IntraBankTransferState`: Is a `LinearState` that represents a money transfer between two clients within the same bank.

### Schemas
For each state except DDRObjectState, there's a corresponding schema to save states to custom tables using JPA entities. 

### Contracts
- `DDRObjectContract`: It governs the usage of DDRs in transactions.
- `DDRObligationContract`: It governs the evolution of obligations. It has the following commands:
	- `RequestDDRPledge`: Validation rules governing requesting a pledge by a bank.
	- `CancelDDRPledge`: Validation rules governing canceling a pledge by a bank.
	- `DenyDDRPledge`: Validation rules governing denying a pledge by the central bank.
	- `ApprovingDDRPledge`: Validation rules governing approving a pledge by the central bank i.e issuing DDR Objects equivalent to pledged amount.
	- `RequestDDRRedeem`: Validation rules governing requesting a pledge by a bank.
	- `CancelDDRRedeem`: Validation rules governing canceling a pledge by a bank.
	- `DenyDDRRedeem`: Validation rules governing denying a pledge by the central bank.
	- `ApprovingDDRRedeem`: Validation rules governing approving a pledge by the central bank i.e archiving DDR Objects equivalent to redeemed amount.
- `InterBankTransferContract`: Governs interbank transactions. It has one command : `BankTransfer`
- `IntraBankTransferContract`: Governs intrabank transactions. It has one command: `RecordTransfer`

### Flows
- `RequestDDRPledge`: Used by banks to request DDR Objects after pledging currency, it will create a DDR Obligation with status request. Flow needs an `amount` and a `requesterDate` as parameters.
- `CancelDDRPledge`: Used by banks to cancel a DDR Pledge while awaiting approval. cancelling cannot happen after approving. Flow needs `externalId` of the obligation pledge.
- `DenyDDRPledge`: Used by central bank to deny a Pledge Request i.e if the bank hasn't deposited currency to central bank. needs `externalId` of the obligation pledge.
- `ApproveDDRPledge`: Used by central bank to approve a Pledge and issue DDR Objects. needs `externalId` of the obligation pledge.
- `RequestDDRRedeem`: Used by banks to request redeeming DDR Objects, it will create a DDR Obligation with status request. Flow needs an `amount` and a `requesterDate` as parameters.
- `CancelDDRRedeem`: Used by banks to cancel a DDR Redeem Request while awaiting approval. cancelling cannot happen after approving. Flow needs `externalId` of the request.
- `DenyDDRRedeem`: Used by central bank to deny a Redeem Request.
- `ApproveDDRRedeem`: Used by central bank to approve a Pledge and archiving DDR Objects. needs `externalId` of the obligation pledge.
- `AtomicExchangeDDR`: Used to make a bank transfer that will start instantly, needs `Sender RIB` (Relevé d'Identité Bancaire), `Receiver RIB`, `Receiver Bank` (Identify which Corda node the bank exists on), `amount`, `Execution Date`.
- `RecordIntraBankTransfer`: Used to record inta-bank transfers. needs `Sender RIB`, `Receiver RIB`, `amount` and `execution Date`.

## Pre-requisites:
See https://docs.corda.net/getting-set-up.html.

## Running the nodes in shell

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

You can run the nodes locally by using this command in root folder `gradlew deployNodes` then execute `.\build\nodes\runnodes.bat` (Windows) or `.\build\nodes\runnodes` (Linux).
It will start 4 shell windows, you can start a flow like :
- Make pledge request (From a bank node) : `start com.octo.flows.RequestDDRPledge$Initiator amount: $1000, requesterDate: 2020-01-01`
- Query the ledger (From requesting bank or central bank) : `run vaultQuery contractStateType: com.octo.states.DDRObligationState`
	You can recuperate the `externalId` to cancel, deny or approve the request. 
- Approve pledge request (Central bank node): `start com.octo.flows.ApproveDDRPledge$Initiator externalId: "exampleId"`
	If you approve an amount of $1000, it will issue 100 DDR Object of $10 for example. (For $250)
- Make a bank transfer: `start com.octo.flows.AtomicExchangeDDR$Initiator senderRIB: "senderRIB", receiverRIB: "receiverRIB", receiverBank: "BankB", amount: $500, executionDate: 2020-01-01`
	Total DDR Objects of 500$ will be transfered to BankB (50 DDR Object each of 10$)
	
## Running using Ganache

Another alternative is to use Corda Flavored Ganache, it makes it easier to run a network, visualise transaction and test flows.
See https://blog.b9lab.com/cordacon-2019-highlights-truffles-corda-flavored-ganache-b83bf00f7c29
Note: Central Bank node must have the legal name "O=CentralBank,L=New York,C=US" as defined in builde.gradle file, so make sure you to create a node in Ganache with that name. It's still beta so might be unstable.

## Running Spring Client

Now, you can use the exposed endpoints to start flows and query states.
1- Run `./gradlew deployNodes` and `./build/nodes/runnodes` to start the nodes.
2- Run the spring client for Bank A, always from root folder run `./gradlew runBankAServer`.
3- Test endpoints (i.e Postman or cURL) as described in controllers:
	Examples: Request a pledge : POST {"amount":"5000"} to http://localhost:10050/api/obligations/request-pledge
		  Query all obligations : GET http://localhost:10050/api/obligations/all
		  Query one obligation by id : GET http://localhost:10050/api/obligations/<id>
		etc...
	
## Running Tests

Use IntelliJ and JDK 1.8 (Higher versions might work too, but it's recommanded to use 1.8), the run tests using gradle (If the project is set to run using IntelliJ, you go to `Build, Execution, Deployment -> Gradle` and set `Build and run using` and `Run tests using` to Gradle). There're now two kinds of tests: Contract tests and Flow tests, you can debug the code while tests are running.

Or run from command line : `gradlew test`.





// TODO : complete REST APIs and tests.
// TODO : Test schemas
// TODO : UI Client
// TODO : Business rules validations in flows.
// TODO : DDR selection mechanism.


