import java.net.*;
import java.io.*;
import java.security.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.*;

public class ClienteCifrado02
{
 public static void main(String a[]) throws Exception
 {
  Socket socket = null;
  // Peticion es lo que envia el Cliente
  String peticion = "";
  String respuesta = null;
  boolean conectionIsNotLost = true;

  try
  {
  Logger logger = Logger.getLogger(ClienteCifrado02.class.toString());  

   ObjectInput in = new ObjectInputStream(new FileInputStream("../servidor/llave.ser"));
   Key llave = (Key)in.readObject();
   logger.log(Level.INFO, "llave= {0}.", llave );
   in.close();
			
   System.out.println("Me conecto al puerto 8000 del servidor");
   socket = new Socket(a[0],8000);
   // Como ya hay socket, obtengo los flujos asociados a este
   DataInputStream dis = new DataInputStream( socket.getInputStream() );
   DataOutputStream dos = new DataOutputStream( socket.getOutputStream() );
    BufferedReader br = new BufferedReader( new InputStreamReader( System.in ) );
   while(conectionIsNotLost){
// Despues de la conexion, Servidor y Cliente deben ponerse de acuerdo
    // para ver quien escribe primero y entonces el otro debe leer

    respuesta = dis.readUTF();//se lee un mensaje
     System.out.println("El mensaje que me envio el servidor es: " + respuesta);
    // Como el Servidor escribe, yo debo leer


   // Ya que me conecte con el Servidor, debo leer del teclado para despues eso mismo enviarlo al Servidor
   //BufferedReader br = new BufferedReader( new InputStreamReader( System.in ) );
   peticion = br.readLine();
   logger.log(Level.INFO, "Mi peticion es: {0}.", peticion );
   logger.log(Level.INFO, "Ahora encriptamos la peticion..." );

   byte[] arrayPeticion = peticion.getBytes();
   Cipher cifrar = Cipher.getInstance("AES");
   cifrar.init(Cipher.ENCRYPT_MODE, llave);
   byte[] cipherText = cifrar.doFinal( arrayPeticion );
   logger.log(Level.INFO, "El argumento ENCRIPTADO es:" );
   logger.log(Level.INFO, new String( cipherText ) );
   bytesToBits( cipherText );
   // Primero envio la longitud del arreglo cifrado
   dos.write( cipherText.length );
   dos.write( cipherText );

   }
    dos.close();
   dis.close();
   socket.close();
    
  }
  catch(IOException e)
  {
   System.out.println("java.io.IOException generada");
   e.printStackTrace();
  }
 }



	public static void bytesToBits( byte[] texto ){
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