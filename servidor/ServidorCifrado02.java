import java.net.*;
import java.io.*;
import java.security.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.*;

public class ServidorCifrado02
{
 public static void main(String a[]) throws Exception
 {

  Logger logger = Logger.getLogger(ServidorCifrado02.class.toString());

  ServerSocket serverSocket = null;
  Socket socket = null;
  boolean estadoSesion = true;

  logger.log(Level.INFO, "Generando la llave..." );
  KeyGenerator keyGen = KeyGenerator.getInstance("AES");
  keyGen.init(128);
  Key llave = keyGen.generateKey();
  logger.log(Level.INFO, "llave= {0}.", llave );
  logger.log(Level.INFO, "Llave generada!" );

  ObjectOutput out = new ObjectOutputStream(new FileOutputStream("llave.ser"));
  out.writeObject( llave );
  out.close();

  try
  {
   logger.log(Level.INFO, "Escuchando por el puerto 8000");
   serverSocket = new ServerSocket(8000); 
  }
  catch(IOException e)
  {
   logger.log(Level.INFO, "java.io.IOException generada");
   e.printStackTrace();
  }

  logger.log(Level.INFO, "Esperando a que los clientes se conecten...");
  while(estadoSesion)
  {
   try
   {
    socket = serverSocket.accept();
    logger.log(Level.INFO, "Se conecto un cliente: {0}.", socket.getInetAddress().getHostName());
    // Como ya hay socket, obtengo los flujos asociados a este
    DataInputStream dis = new DataInputStream( socket.getInputStream() );
    DataOutputStream dos = new DataOutputStream( socket.getOutputStream() );
    // Despues de la conexion, Servidor y Cliente deben ponerse de acuerdo
    // para ver quien escribe primero y entonces el otro debe leer
    BufferedReader br = new BufferedReader( new InputStreamReader( System.in ) );
    // Como el Cliente escribe, yo debo leer
    int bytesLeidos = dis.read();




    logger.log(Level.INFO, "bytes leidos= {0}.", bytesLeidos);
    byte arreglo[]  = new byte[bytesLeidos];
    dis.read( arreglo );

    Cipher cifrar = Cipher.getInstance("AES");
    cifrar.init(Cipher.DECRYPT_MODE, llave);
    bytesToBits( arreglo );
    byte[] newPlainText = cifrar.doFinal(arreglo);
    logger.log(Level.INFO,  "El argumento DESENCRIPTADO es:" );
    String mensaje = new String(newPlainText, "UTF8");
    logger.log(Level.INFO, mensaje);
    dos.writeUTF(mensaje);

    dos.close();
    dis.close();
    socket.close(); 
   }
   catch(IOException e)
   {
    logger.log(Level.SEVERE, "java.io.IOException generada");
    e.printStackTrace();
   }
   estadoSesion = false;
  }
 

 }

	public static void bytesToBits( byte[] texto )
	{
		StringBuilder stringToBits = new StringBuilder();
		for( int i=0; i < texto.length; i++ )
		{
			StringBuilder binary = new StringBuilder();
			byte b = texto[i];
			int val = b;
			for( int j = 0; j < 8; j++ )
			{
				binary.append( (val & 128) == 0 ? 0 : 1 );
				val <<= 1;
			}
			System.out.println( (char)b + " \t " + b + " \t " + binary );
			stringToBits.append( binary );
		}
		System.out.println( "El mensaje completo en bits es:" + stringToBits );
	}

}

