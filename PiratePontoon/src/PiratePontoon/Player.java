package PiratePontoon;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class Player extends JFrame 
{
	private JLabel label;
	private JTextField nameInput;
	private JButton submit;
	private JPanel details;
	private JButton hit;
	private JButton stay;
	private JPanel buttons;
	private JTextArea displayArea; // display information to user
	private ObjectOutputStream output; // output stream to server
	private ObjectInputStream input; // input stream from server
	private String message = ""; // message from server
	private String hostServer; // host server for this application
	private int portServer;
	private Socket client; // socket to communicate with server
	
	// set up client GUI
	public Player( String host, int port )
	{
		super( "Player" );
		
		//store values given from command line
		hostServer = host;
		portServer = port;
		
		label = new JLabel("Enter Name:");
		details = new JPanel();
		details.setLayout(new GridLayout(1,2));
		nameInput = new JTextField("");
		submit = new JButton("Submit");
		
		buttons = new JPanel();
		buttons.setLayout(new GridLayout(1,2));
		hit = new JButton("Hit");
		stay = new JButton("Stay");
		
		submit.addActionListener(
				new ActionListener() 
				{
					// send message to server
					public void actionPerformed( ActionEvent event )
					{
						if (nameInput.getText() != "") {
							String name = nameInput.getText();
							sendData( "submit");
							sendData(name);
							details.setVisible(false);
						}
					} // end method actionPerformed
				} // end ActionListener
				); // end addActionListener
		
		hit.addActionListener(
				new ActionListener() 
				{
					// send message to server
					public void actionPerformed( ActionEvent event )
					{
						sendData( "hit" );
					} // end method actionPerformed
				} // end ActionListener
				); // end addActionListener
		
		stay.addActionListener(
				new ActionListener() 
				{
					// send message to server
					public void actionPerformed( ActionEvent event )
					{
						sendData( "stay" );
					} // end method actionPerformed
				} // end ActionListener
				); // end addActionListener
		details.add(label, BorderLayout.NORTH);
		details.add(nameInput, BorderLayout.SOUTH);
		details.add(submit, BorderLayout.SOUTH);
		details.setVisible(false);
		add(details, BorderLayout.NORTH);
		
		buttons.add(hit, BorderLayout.SOUTH);
		buttons.add(stay, BorderLayout.SOUTH);
		buttons.setVisible(false);
		add(buttons,BorderLayout.SOUTH);
		
		displayArea = new JTextArea(); // create displayArea
		add( new JScrollPane( displayArea ), BorderLayout.CENTER );

		setSize( 400, 400 ); // set size of window
		setVisible( true ); // show window
	} // end Client constructor

	// connect to server and process messages from server
	public void runClient() 
	{
		try // connect to server, get streams, process connection
		{
			connectToServer(); // create a Socket to make connection
			getStreams(); // get the input and output streams
			processConnection(); // process connection
		} // end try
		catch ( EOFException eofException ) 
		{
			displayMessage( "\nClient terminated connection" );
		} // end catch
		catch ( IOException ioException ) 
		{} // end catch
		finally 
		{
			closeConnection();
		} // end finally
	} // end method runClient

	// connect to server
	private void connectToServer() throws IOException
	{      
		displayMessage( "Attempting connection...\n" );

		// create Socket to make connection to server
		client = new Socket( InetAddress.getByName( hostServer ), portServer );

		// display connection information
		
	} // end method connectToServer

	// get streams to send and receive data
	private void getStreams() throws IOException
	{
		// set up output stream for objects
		output = new ObjectOutputStream( client.getOutputStream() );      
		output.flush(); // flush output buffer to send header information

		// set up input stream for objects
		input = new ObjectInputStream( client.getInputStream() );
		details.setVisible(true);
	} // end method getStreams

	// process connection with server
	private void processConnection() throws IOException
	{


		do // process messages sent from server
		{ 
			try // read message and display it
			{
				message = ( String ) input.readObject(); // read new message
				if (message.contains("You were Dealt:")) {
					buttons.setVisible(true);
				} //end if
				displayMessage( "\n" + message ); // display message
				if (message.contains("Bust!") || message.contains("Please Wait")){
					buttons.setVisible(false);				
				} //end if
				if (message.contains("Game finished")) {
					closeConnection();
				}
				
			} // end try
			catch ( ClassNotFoundException classNotFoundException ) 
			{
				displayMessage( "\nUnknown message received" );
			} // end catch

		} while ( !message.equals( "SERVER>>> TERMINATE" ) );
	} // end method processConnection

	// close connections to server
	private void closeConnection() 
	{
		displayMessage( "\nClosing connection" );
		try // close streams and server socket
		{
			output.close(); // close output stream
			input.close(); // close input stream
			client.close(); // close server socket
		} // end try
		catch ( IOException ioException ) 
		{} // end catch
	} // end method closeConnection

	// send message to server
	private void sendData( String message )
	{
		try // send object to server
		{
			output.writeObject(  message );
			output.flush(); // flush data to output
		} // end try
		catch ( IOException ioException )
		{
			displayArea.append( "\nError writing object" );
		} // end catch
	} // end method sendData

	// shows message to player
	private void displayMessage( final String messageToDisplay )
	{
		SwingUtilities.invokeLater(
				new Runnable()
				{
					public void run() // updates displayArea
					{
						displayArea.append( messageToDisplay );
					} // end method run
				}  // end runnable class
				); // end SwingUtilities.invokeLater
	} // end displayMessage

	public static void main( String[] args )
	   {
	      Player client; // create client

	      //gives defaults command line arguments
	      if ( args.length == 0) {
	         client = new Player( "127.0.0.1", 25027 );
	      } //end if
	      else {
	    	//send command line arguments to player class
	         client = new Player( args[0], Integer.parseInt(args[1]) );
	      } //end else

	      client.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
	      client.runClient(); // run client application
	   } // end main
	

	
}//end player class
