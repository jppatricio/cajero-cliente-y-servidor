/*
ACS - Poryecto Final
Objetivo: Crear una comunicación cifrada cliente-servidor mediante sockets PCP 
usando una llave privada. 

-Este es el código del Servidor,abre el socket 8000 por defeto, y espera a que se conecte el cliente

Alumnos : Jaime Porras Patricio, Arias Pelayo Thomas Alejandro
*/
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.*;
import java.security.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.*;

public class ServidorCifrado02 {

  public static void main(String a[]) throws Exception {

    Logger logger = Logger.getLogger(ServidorCifrado02.class.toString());
    //Creacion de los sockets
    ServerSocket serverSocket = null;
    Socket socket = null;
    boolean estadoSesion = true;
    //Generamos la llave 
    logger.log(Level.INFO, "Generando la llave...");
    KeyGenerator keyGen = KeyGenerator.getInstance("AES");//usamos modo de encriptación AES
    keyGen.init(128);//numero de bits de la llave
    //Se crea la llave
    Key llave = keyGen.generateKey();
    logger.log(Level.INFO, "llave= {0}.", llave);
    logger.log(Level.INFO, "Llave generada!");
    //Se copia la llave en un archivo .ser
    ObjectOutput out = new ObjectOutputStream(new FileOutputStream("llave.ser"));
    out.writeObject(llave);
    out.close();

    try {
      logger.log(Level.INFO, "Escuchando por el puerto 8000");
      serverSocket = new ServerSocket(8000);
    } catch (IOException e) {
      logger.log(Level.INFO, "java.io.IOException generada");
      e.printStackTrace();
    }

    logger.log(Level.INFO, "Esperando a que los clientes se conecten...");
    while (estadoSesion) {
      try {
        socket = serverSocket.accept();
        logger.log(Level.INFO, "Se conecto un cliente: {0}.", socket.getInetAddress().getHostName());
        // Como ya hay socket, obtengo los flujos asociados a este

        // INIT de las variables globales dentro del while de interacción con el usuario
        boolean connectionIsNotLost = true;
        Cipher decipher = Cipher.getInstance("AES");
        Cipher cipher = Cipher.getInstance("AES");

        decipher.init(Cipher.DECRYPT_MODE, llave);
        cipher.init(Cipher.ENCRYPT_MODE, llave);

        DataInputStream dis = new DataInputStream(socket.getInputStream());
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

        String mensaje = "";
        String opcionLeida = "";
        String response = "";

        byte[] cipherText;
        int bytesLeidos;

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        while (connectionIsNotLost) {

          // MANDO QUE SE ACEPTO Y LA BIENVENIDA JUNTO LAS OPCIONES DISPONIBLES
          mensaje = "Conexion aceptada \n Elige una de las siguientes opciones seguidas de un 'ENTER'\n\n 1.-\t Consultar saldo\n 2.-\t Depositar\n 3.-\t Retirar\n 4.-\t Salir";
          //envia el mensaje cifrado al cliente para que elija la opcion que guste
          cipherText = cipher.doFinal(mensaje.getBytes());
          dos.write(cipherText.length);
          dos.write(cipherText);

          // LECTURA DE LA RESPUESTA AL CLIENTE
          bytesLeidos = dis.read();
          logger.log(Level.INFO, "bytes leidos= {0}.", bytesLeidos);
          byte arreglo[] = new byte[bytesLeidos];
          dis.read(arreglo);
          byte[] newPlainText = decipher.doFinal(arreglo);
          opcionLeida = new String(newPlainText, "UTF8");
          logger.log(Level.INFO, "El argumento DESENCRIPTADO es: {0}", opcionLeida);

          String errorMsg = "Algo salio mal. Reintente";
          //segun la opcionLeida que elije el cliente se hacen las instrucciones de Consultar/Depositar/Retirar
          switch (opcionLeida) {
            case "1":
              if (!getSaldo().equalsIgnoreCase("")) {
                //manda el saldo al cliente en un mensaje cifrado
                cipherText = cipher.doFinal(getSaldo().getBytes());
                dos.write(cipherText.length);
                dos.write(cipherText);
              } else {
                //manda mensaje de error
                cipherText = cipher.doFinal(errorMsg.getBytes());
                dos.write(cipherText.length);
                dos.write(cipherText);
              }
              break;
                //opcion de depositar dinero
            case "2":
            //envia el mensaje cifrado al cliente
              cipherText = cipher.doFinal("¿Cuanto quiere depositar?".getBytes());
              dos.write(cipherText.length);
              dos.write(cipherText);
              boolean correctDeposit = false;
              while (!correctDeposit) {
                // LECTURA DE LA RESPUESTA AL CLIENTE
                bytesLeidos = dis.read();
                logger.log(Level.INFO, "bytes leidos= {0}.", bytesLeidos);
                arreglo = new byte[bytesLeidos];
                dis.read(arreglo);
                newPlainText = decipher.doFinal(arreglo);
                opcionLeida = new String(newPlainText, "UTF8");
                logger.log(Level.INFO, "El argumento DESENCRIPTADO es: {0}", opcionLeida);
                try {
                  float f = Float.parseFloat(opcionLeida);//le la cantidad de dinero que quiere depositar el cliente
                  if (putDeposit(f)) {//si registra bien el deposito entonces se madnda mensaje cifrado
                    cipherText = cipher.doFinal("Se ha registrado su deposito correctamente".getBytes());
                    dos.write(cipherText.length);
                    dos.write(cipherText);
                    correctDeposit = true;
                  } else {//manda un mensaje cifrado de error al clliente
                    cipherText = cipher.doFinal(errorMsg.getBytes());
                    dos.write(cipherText.length);
                    dos.write(cipherText);
                    correctDeposit = false;
                  }
                } catch (Exception e) {//El cliente no ingreso un valor numerico
                  cipherText = cipher.doFinal("Error > Ingrese un valor numerico... *Ej=>200.50".getBytes());
                  dos.write(cipherText.length);
                  dos.write(cipherText);
                }
              }
              break;
            case "3"://case para la opcion de retirar
              cipherText = cipher.doFinal("¿Cuanto quiere retirar?".getBytes());
              dos.write(cipherText.length);
              dos.write(cipherText);
              boolean correctWithdrawal = false;
              while (!correctWithdrawal) {
                // LECTURA DE LA RESPUESTA AL CLIENTE
                bytesLeidos = dis.read();
                logger.log(Level.INFO, "bytes leidos= {0}.", bytesLeidos);
                arreglo = new byte[bytesLeidos];
                dis.read(arreglo);
                newPlainText = decipher.doFinal(arreglo);
                opcionLeida = new String(newPlainText, "UTF8");
                logger.log(Level.INFO, "Withdrawing: {0}", opcionLeida);
                try {
                  float f = Float.parseFloat(opcionLeida);//lee la cantidad de dinero que quiere retirar el cliente
                  if (putWithdrawal(f).equalsIgnoreCase("OK")) {
                    cipherText = cipher.doFinal(("Se ha registrado su retiro de $" + f + "MXN correctamente").getBytes());
                    //se manda mensaje de retiro cifrado al cliente
                    dos.write(cipherText.length);
                    dos.write(cipherText);
                    correctWithdrawal = true;
                    //Si se trata de retirar más dinero del que se tiene manda mensaje cifrado de que no se tiene suficiente dinero
                  } else if (putWithdrawal(f).equalsIgnoreCase("ZERO")) {
                    cipherText = cipher.doFinal(("No tiene suficiente dinero...").getBytes());
                    dos.write(cipherText.length);
                    dos.write(cipherText);
                    correctWithdrawal = true;
                  } else {
                    //envía mensaje de error cifrado
                    cipherText = cipher.doFinal(errorMsg.getBytes());
                    dos.write(cipherText.length);
                    dos.write(cipherText);
                    correctWithdrawal = true;
                  }
                } catch (Exception e) {
                  cipherText = cipher.doFinal("Error > Ingrese un valor numerico... *Ej=>200.50".getBytes());
                  dos.write(cipherText.length);
                  dos.write(cipherText);
                }
              }
              break;
            case "4"://case para salir
              cipherText = cipher.doFinal("¡Hasta luego!".getBytes());
              dos.write(cipherText.length);
              dos.write(cipherText);
              connectionIsNotLost = false;
              break;

            default:
              logger.log(Level.FINE, "THIS IS FINE!");
              break;
          }
        }
        //cierra el socket y el data input/output streamer
        dos.close();
        dis.close();
        socket.close();
      } catch (IOException e) {
        logger.log(Level.SEVERE, "java.io.IOException generada");
        e.printStackTrace();
      }
      estadoSesion = false;
    }

  }
    //Esta funcion ya esta del ejemplo de ClienteCifrado02.java en http://profesores.fi-b.unam.mx/carlos/acs/Tema-06-Sockets-Java-Criptografia/
    //para que la terminal se viera un poco más limpia no la llamamos en ningun lado
  public static void bytesToBits(byte[] texto) {
    StringBuilder stringToBits = new StringBuilder();
    for (int i = 0; i < texto.length; i++) {
      StringBuilder binary = new StringBuilder();
      byte b = texto[i];
      int val = b;
      for (int j = 0; j < 8; j++) {
        binary.append((val & 128) == 0 ? 0 : 1);
        val <<= 1;
      }
      System.out.println((char) b + " \t " + b + " \t " + binary);
      stringToBits.append(binary);
    }
    System.out.println("El mensaje completo en bits es:" + stringToBits);

  }

  //De aquí en adelnate se definen las funciones para mostrar saldo, retirar y depositar dinero
  public static String getSaldo() {
    try {
      FileReader reader = new FileReader("client.txt");
      BufferedReader bufferedReader = new BufferedReader(reader);
      String line = bufferedReader.readLine();
      reader.close();
      return ("Saldo es de: $" + line + " MXN");

    } catch (Exception e) {
      return "";
    }
  }

  public static Float getFloatSaldo() {
    try {
      FileReader reader = new FileReader("client.txt");
      BufferedReader bufferedReader = new BufferedReader(reader);
      String line = bufferedReader.readLine();
      reader.close();
      return Float.parseFloat(line);

    } catch (Exception e) {
      return -1f;
    }
  }

  public static boolean putDeposit(float q) {
    try {
      Float s = getFloatSaldo();
      FileWriter writer = new FileWriter("client.txt", false);
      s = s + q;
      writer.write(s.toString());
      writer.close();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public static String putWithdrawal(float q) {
    try {
      Float s = getFloatSaldo();
      if ((s - q) < 0) {
        return "Zero";
      }
      s = s - q;
      FileWriter writer = new FileWriter("client.txt", false);
      writer.write(s.toString());
      writer.close();
      return "OK";
    } catch (Exception e) {
      return "Error";
    }
  }

}
