package PiratePontoon;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class Dealer extends JFrame {

	private Deck newdeck;
	private JTextArea displayArea; // display information to user
	private ExecutorService executor; // will run players
	private ServerSocket server; // server socket
	private SockServer[] sockServer; // Array of objects to be threaded
	private int portServer;
	private int playerMax;
	private int roundMax;
	private int counter = 1; // counter of number of connections
	private int round = 0;
	private int namesNum = 0;
	private ArrayList<ArrayList<Card>> player;

	private ArrayList<Card> dhand =new ArrayList<>();
	private String[] names = new String[4];
	private ArrayList<String> winner = new ArrayList<>();
	
	private int playersleft;
	
	// sets up the server GUI
	public Dealer(int port, int playerNum, int roundNum) {

		super( "Dealer" );
		
		//store values given from command line
		portServer = port;
		playerMax = playerNum;
		roundMax = roundNum;

		player = new ArrayList<ArrayList<Card>>();
		
		sockServer = new SockServer[10]; // create array for server threads
		executor = Executors.newFixedThreadPool(playerMax+1); // create thread pool

		displayArea = new JTextArea(); // create displayArea
		displayArea.setEditable(false);
		add( new JScrollPane( displayArea ), BorderLayout.CENTER );

		setSize( 400, 400 ); // set size of window
		setVisible( true ); // show window
	} // end Server constructor

	// set up and run server 
	public void runDeal()
	{
		try // set up server to receive connections
		{
			// creates socket for players to connect to
			server = new ServerSocket( portServer, playerMax ); // create ServerSocket
				// connects clients to server
				while ( playerMax >= counter ) 
				{
					try 
					{
						//create a new runnable object to serve the next client to call in
						sockServer[counter] = new SockServer(counter);
						// make that new object wait for a connection on that new server object
						sockServer[counter].waitForConnection();
						// launch that server object into its own new thread
						executor.execute(sockServer[ counter ]);
						// then, continue to create another object and wait (loop)
						
					} // end try
					catch ( EOFException eofException )
					{
						displayMessage( "\nServer terminated connection" );
					} // end catch
					finally 
					{
						++counter;	
					} // end finally
				}// end while
		} // end try
		catch ( IOException ioException ) 
		{} // end catch
	} // end method runServer

	// used to show message on server window
	private void displayMessage( final String messageToDisplay )
	{
		SwingUtilities.invokeLater(
				new Runnable() 
				{
					public void run() // updates displayArea
					{
						displayArea.append( messageToDisplay ); // append message
					} // end method run
				} // end runnable
				); // end SwingUtilities.invokeLater
	} // end displayMessage
	
	//opens new deck and gives dealer and players 2 cards from a deck
	private void DealCards(){
		try{
			newdeck = new Deck();
			playersleft = counter-1;
			Card dcard1=(Card) newdeck.cardList.get(Deck.index);
			Deck.index++;
			Card dcard2=(Card) newdeck.cardList.get(Deck.index);
			Deck.index++;
			dhand.add(dcard1);
			dhand.add(dcard2);
			displayMessage("\n\nDealers Cards:\n" +dcard1.ToString() + ", " +dcard2.ToString());

			for (int i=1;i< counter;i++) {
				
				Card c1=(Card) newdeck.cardList.get(Deck.index);
				Deck.index++;
				Card c2=(Card) newdeck.cardList.get(Deck.index);
				Deck.index++;
				ArrayList<Card> hand =new ArrayList<>();
				hand.add(c1);
				hand.add(c2);
				player.add(hand);
				//tells players the cards they have and the total of their hand
				sockServer[i].sendData("\nYou were Dealt:\n" + GetCurrentHand(hand));
				sockServer[i].sendData("Your Total: " +  GetHandTotal(hand)+"\n");

			} //end for
		} //end try
		catch(NullPointerException n){}
	} //end DealCards
	
	//checks players hands and gives results
	private void Results() {

		try{
			// checks the hand of each player
			for (int i=1;i <= counter;i++) {
				// shows players dealers total
				sockServer[i].sendData("Dealer has " + GetHandTotal(dhand)+"\n");
				sockServer[i].sendData("Winners:");

				// checks if  dealer
				if( (GetHandTotal(dhand) <= 21) && (GetHandTotal(player.get(i-1)) <= 21 ) ){
					// checks if a player wins
					if (GetHandTotal(dhand) < GetHandTotal(player.get(i-1))) {
						winner.add(names[i-1]);
					} // end if

				}//end if 
				
				// checks if dealers hand is bust
				if(CheckBust(dhand)){
					// checks if a player wins
					if(GetHandTotal(player.get(i-1)) <= 21){
						winner.add(names[i-1]);
					} // end if
				} // end if

				
			}//end for
			
		}//end try
		
		catch(NullPointerException n){}
		finally{
			// tells users names of winners stored in winner arrayList
			for (int i=1; i < counter; i++) {
				for (String win : winner) {
					sockServer[i].sendData(win);
				} //end for
			} //end for
			// tells users dealer is the winner
			if (winner.size() == 0) {
				if (CheckBust(dhand)) {
					for (int i=1; i < counter; i++) {
						sockServer[i].sendData("No winner");
					} //end for
				} //end if
				else {
					for (int i=1; i < counter; i++) {
						sockServer[i].sendData("Dealer");
					} //end for
				} //end else
			} //end if
			// checks if max rounds have been played then clears arrayLists and begins next round
			if (round < roundMax) {
				player.clear();
				dhand.clear();
				winner.clear();
				DealCards();
				displayMessage("\n\nDealing cards to players\n\n");
				round++;
			} //end if
			// sends message to clients to close connections
			else if (round == roundMax) {
				for (int i=1; i < counter; i++) {
					sockServer[i].sendData("Game finished");
				} //end for
			} //end else if
		} //end finally
	} //end results

	
	//create server threads for clients
	private class SockServer implements Runnable
	{
		private ObjectOutputStream output; // output stream to client
		private ObjectInputStream input; // input stream from client
		private Socket connection; // connection to client
		private int myConID;

		public SockServer(int counterIn)
		{
			myConID = counterIn;
		}

		public void run() {
			try {
				try {
					getStreams(); // get input & output streams
					processConnection(); // process connection

				} // end try
				catch ( EOFException eofException ) 
				{
					displayMessage( "\nServer" + myConID + " terminated connection" );
				}
				finally
				{
					closeConnection(); //  close connection
				}// end catch
			} // end try
			catch ( IOException ioException ) 
			{} // end catch
		} // end try

		// wait for connection to arrive, then display connection info
		private void waitForConnection() throws IOException
		{

			displayMessage( "Waiting for player " + myConID + "\n" );
			connection = server.accept(); // allow server to accept connection            
			displayMessage( "Connection from player " + myConID + " received\n\n");
		} // end method waitForConnection

		private void getStreams() throws IOException
		{
			// set up output stream for objects
			output = new ObjectOutputStream( connection.getOutputStream() );
			output.flush(); // flush output buffer to send header information

			// set up input stream for objects
			input = new ObjectInputStream( connection.getInputStream() );

			//displayMessage( "\nGot I/O streams\n" );
		} // end method getStreams

		// process connection with client
		private void processConnection() throws IOException
		{
			String message = "Connection to server successful";
			sendData( message ); // send connection successful message
			sendData( "Enter your Name"); // send connection successful message

			do // process messages sent from client
			{ 
				try // read message and display it
				{
					//uses cardhit method if hit message is received
					if(message.contains("hit")){				
						cardhit();
					}
					//uses checkdone method if stay message is received
					if(message.contains("stay")){
						this.sendData("Please Wait");
						playersleft--;
						CheckDone();
					}
					//collects and stores name of clients
					if(message.contains("submit")){
						
						message = ( String ) input.readObject();
						names[this.myConID -1] = message;
						sendData("\nWelcome to Pirate Pontoon, "+ message);
						namesNum++;
						
						//starts first round of Pirate Pontoon once all names have been collected
						if (namesNum == playerMax){
								player.clear();
								dhand.clear();
								DealCards();
								displayMessage("\n\nDealing cards to players\n\n");
								round++;
						} //end if
					} //end if
					message = ( String ) input.readObject(); // read new message
				} // end try
				catch ( ClassNotFoundException classNotFoundException ) 
				{
					displayMessage( "\nUnknown object type received" );
				} // end catch

			} while ( !message.equals( "CLIENT>>> TERMINATE" ) );
		} // end method processConnection

		//method for dealers turn
		private void DealerTurn() {		
			try {
				Thread.sleep(1000);
			} //end try
			catch (InterruptedException e) {
				e.printStackTrace();
			} //end catch
			//gives dealer new cards until hand is greater than 16
			if (GetHandTotal(dhand) <= 16){
				while(GetHandTotal(dhand) <= 16){
					Card card1=(Card) newdeck.cardList.get(Deck.index);
					Deck.index++;
					dhand.add(card1);
					displayMessage("Dealer hits " + card1.ToString() +  "\n" + "Total:" + GetHandTotal(dhand) + "\n");				
				} //end while
			} //end if
			//checks if dealer is bust
			if(CheckBust(dhand)){
				displayMessage("Dealer Busts!\n");
			} //end if
			else{
				displayMessage("Dealer has " + GetHandTotal(dhand));
			} //end else

			Results();
		} //end DealerTurn

		private void cardhit() { //method to give a new card to player if they choose to hit

			Card hitCard=(Card) newdeck.cardList.get(Deck.index);
			Deck.index++;
			sendData("You hit: " + hitCard.ToString());
			player.get(this.myConID -1).add(hitCard);
			sendData("Your current Hand: \n" +  GetCurrentHand(player.get(this.myConID -1))+"\n");
			sendData("Your Total: " +  GetHandTotal(player.get(this.myConID -1))+"\n");
			if(CheckBust(player.get(this.myConID -1))) {			//if player busted
				sendData("Bust!\n");
				playersleft--;
				if(playersleft==0){
					DealerTurn();
				} //end if
			} //end if
		}//end method cardhit


		private void CheckDone() {

			if(playersleft==0){

				DealerTurn();
			}
		}

		// close streams and socket
		private void closeConnection() 
		{
			displayMessage( "\nTerminating connection " + myConID + "\n" );
			try 
			{
				output.close(); // close output stream
				input.close(); // close input stream
				connection.close(); // close socket
			} // end try
			catch ( IOException ioException ) 
			{} // end catch
		} // end method closeConnection

		// send message to client
		private void sendData( String message )
		{
			try // send object to client
			{
				output.writeObject( message );
				output.flush(); // flush output to client

			} // end try
			catch ( IOException ioException ) 
			{
				displayArea.append( "\nError writing object" );
			} // end catch
		} // end method sendData


	} // end class SockServer

	// adds total values of cards in a hand
	private int GetHandTotal(List<Card> hand) {
		int handTotal = 0;
		for (Card hand1 : hand) {
			handTotal += hand1.getValue();
		} //end for
		return handTotal;
	} //end method GetHandTotal
	
	// places cards of a hand into a string and returns it
	private String GetCurrentHand(List<Card> hand) {
		String handCurrent = "";
		for (Card hand1 : hand) {
			handCurrent += hand1.ToString()+", ";
		} //end for
		return handCurrent;
	} //end method GetCurrentHand
	
	// checks if a hand has bust
	private boolean CheckBust(List<Card> hand) {
		boolean bust;
		if(GetHandTotal(hand) > 21) {
			bust = true;
		}
		else {
			bust = false;
		}
		return bust;
	} //end method CheckBust
	
	public static void main( String[] args )
	   {
	      Dealer server; // create server
	      
	      //gives defaults command line arguments
	      if ( args.length == 0) {
	    	  server = new Dealer( 25027, 4, 4);
	      }
	      //ensure max players is 4
	      else {
	    	  if (Integer.parseInt(args[1]) > 4) {
	    		  args[1] = "4";
	    	  }
	    	  //send command line arguments to dealer class
	    	  server = new Dealer( Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
	      }
	      
	      server.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
	      server.runDeal(); // run server application
	   } // end main
	
} // end class Server

